package dev.ramzanzan.chat.service;

import dev.ramzanzan.chat.model.ByteBufferedRimpMessage;
import dev.ramzanzan.chat.model.RimpClientSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Slf4j
@Component
public class RimpServerWebSocketHandler implements WebSocketHandler {

    private static final String peerSession = "chps";

    @Autowired
    private RimpServer server;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var chps = new RimpClientSession(session);
        session.getAttributes().put(peerSession,chps);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        var chps = (RimpClientSession)session.getAttributes().get(peerSession);
        if(message instanceof BinaryMessage) {
            var msg = ByteBufferedRimpMessage.deserialize(((BinaryMessage) message).getPayload(), false);
            server.process(chps, msg);
        }
        else {
            var str = "Unsupported web socket message from: " + chps + " msg: " + message.toString();
            server.closeMalicious(chps,str);
        }
//        }else if (message instanceof PongMessage){
//
//        }else if (message instanceof TextMessage){
//
//        }else if (message instanceof PingMessage){
//
//        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error: ");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
