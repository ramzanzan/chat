package dev.ramzanzan.chat.model;

import dev.ramzanzan.chat.exceptions.RimpIOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
public class ChattingPeerSession {

    private String id;
    private WebSocketSession wss;
    private Map<String, ChattingPeerSession> outboundInvites;    //by usId
    private Map<String, ChattingPeerSession> inboundInvites;     //by msgId
    private Map<String, ChattingPeerSession> dialogs;            //by usId

    public ChattingPeerSession(WebSocketSession _wss){
        wss = new ConcurrentWebSocketSessionDecorator(_wss, 2000, 1<<12);
    }

    public void authorize(String _id){
        id=_id;
        outboundInvites = new ConcurrentHashMap<>();
        inboundInvites = new ConcurrentHashMap<>();
        dialogs = new ConcurrentHashMap<>();
    }

    public ChattingPeerSession getPaired(String _id){
        return dialogs.get(_id);
    }

    public boolean hasPaired(String _id){
        return dialogs.containsKey(_id);
    }

    public boolean isAuthorized(){
        return id!=null ;
    }

    public void send(RimpMessage<?,? extends ByteBuffer> _msg) throws RimpIOException {
        if(!wss.isOpen()) throw new RimpIOException(id,"Closed");
        try {
            wss.sendMessage(new BinaryMessage(_msg.serialize()));
        }catch (IOException e){
            String id = this.id!=null ? this.id : "null";
            log.error("Sending error, id: "+id+" wss.id: "+wss.getId(),e);
            throw new RimpIOException(id,e);
        }
    }

}
