package dev.ramzanzan.chat.service;

import dev.ramzanzan.chat.exceptions.RimpIOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import dev.ramzanzan.chat.model.ByteBufferedRimpMessage;
import dev.ramzanzan.chat.model.RimpClientSession;
import dev.ramzanzan.chat.model.RimpMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class RimpServer {

    private interface Header{
        String AUTHORIZATION = "AUTHORIZATION";
        String QUERY = "QUERY";
        String FROM = "FROM";
        String TO = "TO";
        String MESSAGE_ID = "MESSAGE-ID";
        String ORDER = "ORDER";
    }

    private interface Status{
        int OK = HttpStatus.OK.value();
        int BAD_REQUEST = HttpStatus.BAD_REQUEST.value();
        int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    }

    private static final int ID_MIN_CHARS = 4;
    private static final int ID_MAX_CHARS = 64;
    private static final String ID_PATTERN = "[a-zA-Z0-9_-]{4,64}";
    private static final String ID_GLOB_CHAR = "*";
    private static final int SEARCH_COUNT = 10;

    private Registry<RimpClientSession> registry;

    public RimpServer(Registry<RimpClientSession> _registry){
        registry=_registry;
    }

    public void process(RimpClientSession _peer, ByteBufferedRimpMessage _msg){
        if(_msg.isRequestNotResponse())
            switch (_msg.getMethod()){
                case FIND : handleFind(_peer,_msg); break;
                case JOIN: handleJoin(_peer,_msg); break;
                case INVITE: handleInvite(_peer,_msg); break;
                case SEND: handleSend(_peer, _msg); break;
                case CLOSE: handleClose(_peer,_msg); break;
            }
        else
            switch (_msg.getMethod()){
                case INVITE: handleInviteResponse(_peer,_msg); break;
                case SEND: handleSendResponse(_peer, _msg); break;
                default: closeMalicious(_peer,"Undefined response ");
            }
    }

    private boolean send(RimpClientSession _peer, ByteBufferedRimpMessage _msg){
        if(_peer==null || _peer.isClosed()) return false;
        try {
            _peer.send(_msg);
            return true;
        }catch (RimpIOException e){
            log.error("Sending error:",e);
            close(_peer);
        }
        return false;
    }

    private boolean checkIdSpelling(String _id){
        return _id.matches(ID_PATTERN+"\\"+ID_GLOB_CHAR+"?");
    }

    private void handleFind(RimpClientSession _peer, ByteBufferedRimpMessage _msg){
        var query = _msg.getHeaders().getFirst(Header.QUERY);
        if(query==null || !checkIdSpelling(query)){
            query = query==null ? "null" : query;
            closeMalicious(_peer,"Bad find: bad search string: "+query+" right pattern: "+ID_PATTERN);
            return;
        }

        String result = null;
        if(query.endsWith(ID_GLOB_CHAR)){
            query = query.substring(0,query.length()-1);
            RimpClientSession[] peers = registry.find(query,SEARCH_COUNT);
            if (peers!=null)
                result = Arrays.stream(peers).map(RimpClientSession::getId).collect(Collectors.joining(" "));
        }else if(registry.contains(query)) {
            result = query;
        }
        ByteBufferedRimpMessage response;
        if (result==null) {
            response = new ByteBufferedRimpMessage(Status.NOT_FOUND, RimpMessage.Method.FIND);
            response.addHeader(Header.QUERY, query);
        }
        else {
            response = new ByteBufferedRimpMessage(Status.OK, RimpMessage.Method.FIND);
            response.addHeader(Header.QUERY, query);
//            response.addHeader(HttpHeaders.CONTENT_TYPE,MediaType.TEXT_PLAIN_VALUE); //todo ? && encoding
            var charset = StandardCharsets.UTF_16;
            response.setData(charset.encode(result));
        }
        send(_peer,response);
    }

    private void handleJoin(RimpClientSession _peer, ByteBufferedRimpMessage _msg){
        var proposedId = _msg.getHeaders().getFirst(Header.FROM);
        if(proposedId==null || !checkIdSpelling(proposedId)){
            proposedId = proposedId==null ? "null" : proposedId;
            closeMalicious(_peer,"Bad join: bad id: "+proposedId+" right pattern: "+ID_PATTERN);
            return;
        }
        ByteBufferedRimpMessage response;
        if(registry.tryAdd(proposedId,_peer)) {
            response = new ByteBufferedRimpMessage(Status.OK, _msg.getMethod());
            _peer.authorize(proposedId);
            log.info("Authorized, id: "+proposedId);
        }
        else
            response = new ByteBufferedRimpMessage(Status.BAD_REQUEST, _msg.getMethod());
        response.addHeader(Header.FROM,proposedId);
        send(_peer,response);
    }

    private boolean checkAuth(RimpClientSession _peer, MultiValueMap<String,String> _headers){
        return  _peer.isAuthorized()
                && _peer.getId().equals(_headers.getFirst(Header.FROM));
    }

    private void lockPairSharable(RimpClientSession peer1, RimpClientSession peer2){
        boolean ascOrder = peer1.getId().compareTo(peer2.getId())<0;
        if(ascOrder){
            peer1.lockSharable();
            peer2.lockSharable();
        }else {
            peer2.lockSharable();
            peer1.lockSharable();
        }
    }

    private void unlockPairSharable(RimpClientSession peer1, RimpClientSession peer2){
        boolean ascOrder = peer1.getId().compareTo(peer2.getId())<0;
        if(ascOrder){
            peer2.unlockSharable();
            peer1.unlockSharable();
        }else {
            peer1.unlockSharable();
            peer2.unlockSharable();
        }
    }

    private void handleInvite(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        var toId = _msg.getHeaders().getFirst(Header.TO);
        var toPeer = registry.get(toId);
        if(toPeer==null || toPeer.isClosed()){
            var response = new ByteBufferedRimpMessage(Status.NOT_FOUND,_msg.getMethod());
            response.addHeader(Header.FROM,toId);
            send(_from,response);
            return;
        }

        toPeer.lockSharable();
        if(toPeer.isClosed()){
            toPeer.unlockSharable();
            var response = new ByteBufferedRimpMessage(Status.NOT_FOUND,_msg.getMethod());
            response.addHeader(Header.FROM,toId);
            send(_from,response);
            return;
        }
        toPeer.getInvitesFrom().add(_from.getId());
        toPeer.unlockSharable();

        send(toPeer,_msg);
    }

    private void handleInviteResponse(RimpClientSession _invited, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_invited,_msg.getHeaders())){
            closeMalicious(_invited,"Unauthorized");
            return;
        }
        var inviterId = _msg.getHeaders().getFirst(Header.TO);
        if(inviterId == null || !_invited.getInvitesFrom().contains(inviterId)){
            closeMalicious(_invited,"Bad invite response: bad header");
            return;
        }
        var inviter = registry.get(inviterId);


        if(inviter==null || inviter.isClosed()) {
            _invited.lockSharable();
            if(_invited.isClosed()) {
                //do nothing
                _invited.unlockSharable();
                return;
            }
            _invited.getInvitesFrom().remove(inviterId);
            _invited.unlockSharable();

            var close = new ByteBufferedRimpMessage(RimpMessage.Method.CLOSE);
            close.addHeader(Header.FROM,inviterId);
            send(_invited,close);
            return;
        }else {
            lockPairSharable(inviter,_invited);
            if(_invited.isClosed()){
                //do nothing
                unlockPairSharable(inviter,_invited);
                return;
            }
            if(inviter.isClosed()){
                _invited.getInvitesFrom().remove(inviterId);
                unlockPairSharable(inviter,_invited);

                var close = new ByteBufferedRimpMessage(RimpMessage.Method.CLOSE);
                close.addHeader(Header.FROM,inviterId);
                send(_invited,close);
                return;
            }
            if(_msg.getStatusCode()==Status.OK) {
                _invited.getDialogs().put(inviter.getId(), inviter);
                inviter.getDialogs().put(_invited.getId(), _invited);
            }
            _invited.getInvitesFrom().remove(inviterId);
            unlockPairSharable(inviter,_invited);
        }
        send(inviter,_msg);
    }

    private boolean checkAddressing(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        var toId = _msg.getHeaders().getFirst(Header.TO);
        return _from.hasPaired(toId);
    }

    private void relay(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        var toId = _msg.getHeaders().getFirst(Header.TO);
        var toPeer = _from.getPaired(toId);
        if(toPeer==null || toPeer.isClosed()){
            //todo resp no ok
        }
        else
            send(toPeer,_msg);
    }

    private void handleSend(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        if(!checkAddressing(_from,_msg)) return;
        relay(_from,_msg);
    }

    private void handleSendResponse(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        if(!checkAddressing(_from, _msg)) return;
        relay(_from,_msg);
    }

    private void handleClose(RimpClientSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        if(!checkAddressing(_from,_msg)) return;
        var toId = _msg.getHeaders().getFirst(Header.TO);
        _from.lockSharable();
        var toPeer = _from.getDialogs().remove(toId);
        _from.unlockSharable();
        if(toPeer!=null && !toPeer.isClosed()) {
            toPeer.lockSharable();
            if(toPeer.isClosed()) {
                toPeer.unlockSharable();
                return;
            }
            toPeer.getDialogs().remove(_from.getId());
            toPeer.unlockSharable();
            send(toPeer,_msg);
        }
    }

    public void closeMalicious(RimpClientSession _peer, String _reason){
        //todo
        close(_peer);
    }

    public void close(RimpClientSession _peer){
        if(_peer.isClosed()) return;

        _peer.lockExclusively();
        if(_peer.isClosed()) return;
        try {
            _peer.getWss().close();
        }catch (IOException e){
            log.error("Error while closing: wss.id: "+_peer.getWss().getId(),e);
        }
        if(!_peer.isAuthorized()) return;

        registry.remove(_peer.getId());
        var closeMsg = new ByteBufferedRimpMessage(RimpMessage.Method.CLOSE);
        closeMsg.addHeader(Header.FROM,_peer.getId());
        for(var id : _peer.getInvitesFrom()){
            var peer = registry.get(id);
            send(peer,closeMsg);
        }
        for (var peer : _peer.getDialogs().values()){
            send(peer,closeMsg);
        }
        _peer.unlockExclusively();
    }
}




