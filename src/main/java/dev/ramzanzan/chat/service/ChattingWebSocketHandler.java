package dev.ramzanzan.chat.service;

import dev.ramzanzan.chat.model.ByteBufferedRimpMessage;
import dev.ramzanzan.chat.model.ChattingPeerSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Component
@Slf4j
public class ChattingWebSocketHandler implements WebSocketHandler {

    private static final String chattingPeerSession = "chps";

    @Autowired
    private ChattingServerBean chattingBean;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var chps = new ChattingPeerSession(session);
        session.getAttributes().put(chattingPeerSession,chps);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        var chps = (ChattingPeerSession)session.getAttributes().get(chattingPeerSession);
        if(message instanceof BinaryMessage) {
            var msg = ByteBufferedRimpMessage.from(((BinaryMessage) message).getPayload(), false);
            chattingBean.process(chps, msg);
        }
        else {
            var str = "Unsupported web socket message from: " + chps + " msg: " + message.toString();
            chattingBean.closeMalicious(chps,str);
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
