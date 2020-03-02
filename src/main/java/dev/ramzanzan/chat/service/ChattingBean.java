package dev.ramzanzan.chat.service;

import dev.ramzanzan.chat.exceptions.RimpIOException;
import dev.ramzanzan.chat.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChattingBean {

    public interface Header{
        String AUTHORIZATION = "AUTHORIZATION";
        String SEARCH = "SEARCH";
        String FROM = "FROM";
        String TO = "TO";
        String ORDER = "ORDER";
    }

    public interface Status{
        int OK = HttpStatus.OK.value();
        int CONFLICT = HttpStatus.CONFLICT.value();
        int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    }

    private static final int ID_MIN_CHARS = 4;
    private static final int ID_MAX_CHARS = 64;
    private static final String ID_PATTERN = "[a-zA-Z0-9_-]{4,64}";
    private static final int SEARCH_COUNT = 10;

    @Autowired
    private PeerRegistry<ChattingPeerSession> registry;

    public void process(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        if(_msg.isRequestNotResponse())
            switch (_msg.getMethod())
        //todo
    }

    private boolean send(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        try {
            _peer.send(_msg);
            return true;
        }catch (RimpIOException e){
            log.error("Peer closed:",e);
            close(_peer);
        }
        return false;
    }

    private boolean checkIdSpelling(String _id){
        if(_id==null || _id.length()<ID_MIN_CHARS || _id.length()>ID_MAX_CHARS) return false;
        return _id.matches(ID_PATTERN);
    }

    private void handleFind(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        var searchString = _msg.getHeaders().getFirst(Header.SEARCH);
        if(!checkIdSpelling(searchString)){
            closeMalicious(_peer,"Bad find: bad search string: "+searchString+" right pattern: "+ID_PATTERN);
            return;
        }
        ChattingPeerSession[] peers = registry.find(searchString,SEARCH_COUNT);
        String ids;
        if (peers==null) ids="";
        else if(peers.length==1) ids=peers[0].getId();
        else ids = Arrays.stream(peers).map(ChattingPeerSession::getId).collect(Collectors.joining(" "));

        var response = new ByteBufferedRimpMessage(_msg.getId(),Status.OK,_msg.getMethod());
        response.addHeader(HttpHeaders.CONTENT_TYPE,MediaType.TEXT_PLAIN_VALUE); //todo encoding
        var charset = StandardCharsets.UTF_16;
        response.setData(charset.encode(ids));
        send(_peer,response);
    }

    private void handleJoin(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        var proposedId = _msg.getHeaders().getFirst(Header.FROM);
        if(!checkIdSpelling(proposedId)){
            closeMalicious(_peer,"Bad find: bad search string: "+proposedId+" right pattern: "+ID_PATTERN);
            return;
        }
        ByteBufferedRimpMessage response;
        if(registry.tryAdd(proposedId,_peer)) {
            response = new ByteBufferedRimpMessage(_msg.getId(), Status.OK, _msg.getMethod());
            _peer.authorize(proposedId);
            log.info("Authorized, id: "+proposedId);
        }
        else
            response = new ByteBufferedRimpMessage(_msg.getId(),Status.CONFLICT, _msg.getMethod());
        send(_peer,response);
    }

    private boolean checkAuth(ChattingPeerSession _peer, MultiValueMap<String,String> _headers){
         return  _peer.isAuthorized()
                 && _peer.getId().equals(_headers.getFirst(Header.FROM));
    }

    private void handleInvite(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_peer,_msg.getHeaders())) {
            closeMalicious(_peer, "Unauthorized");
            return;
        }
        var targetId = _msg.getHeaders().getFirst(Header.TO);
        if(_peer.getOutboundInvites().containsKey(targetId)) return;
        var targetPeer = registry.get(targetId);
        if(targetPeer==null){
            var response = new ByteBufferedRimpMessage(_msg.getId(),Status.NOT_FOUND,_msg.getMethod());
            send(_peer,response);
            return;
        }
        targetPeer.getInboundInvites().put(_msg.getId(),_peer);
        _peer.getOutboundInvites().put(targetId,targetPeer);
        send(targetPeer,_msg);
    }

    private void handleInviteResponse(ChattingPeerSession _peer, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_peer,_msg.getHeaders())){
            closeMalicious(_peer,"Unauthorized");
            return;
        }
        var inviter = _peer.getInboundInvites().get(_msg.getId());
        if(inviter==null ||) {

            return;
        }
        if(_msg.getStatusCode()==Status.OK) {
            _peer.getDialogs().put(inviter.getId(), inviter);
            inviter.getDialogs().put(_peer.getId(), _peer);
        }
        send(inviter,_msg);
    }

    private boolean checkAddressing(ChattingPeerSession _from, ByteBufferedRimpMessage _msg){
        var toId = _msg.getHeaders().getFirst(Header.TO);
        return _from.hasPaired(toId);
    }

    private void relay(ChattingPeerSession _from, ByteBufferedRimpMessage _msg){
        var toId = _msg.getHeaders().getFirst(Header.TO);
        var toPeer = _from.getPaired(toId);
        if(toPeer!=null) send(toPeer,_msg);
    }

    private void handleSend(ChattingPeerSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        if(!checkAddressing(_from,_msg)) return;
        relay(_from,_msg);
    }

    private void handleSendResponse(ChattingPeerSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAddressing(_from, _msg)) return;
        relay(_from,_msg);
    }

    private void handleClose(ChattingPeerSession _from, ByteBufferedRimpMessage _msg){
        if(!checkAuth(_from,_msg.getHeaders())) {
            closeMalicious(_from, "Unauthorized");
            return;
        }
        if(!checkAddressing(_from,_msg)) return;
        var toId = _msg.getHeaders().getFirst(H_TO);
        var toPeer = _from.getDialogs().remove(toId);
        if(toPeer!=null) {
            toPeer.getDialogs().remove(_from.getId());
            send(toPeer,_msg);
        }
    }

    private void closeMalicious(ChattingPeerSession _peer, String _reason){
        //todo
        close(_peer);
    }

    private void close(ChattingPeerSession _peer){

    }

}
