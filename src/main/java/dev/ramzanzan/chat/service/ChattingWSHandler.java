package dev.ramzanzan.chat.service;

import dev.ramzanzan.chat.model.ByteBufferedRimpMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Component
public class ChattingWSHandler implements WebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if(message instanceof BinaryMessage){
            var msg = ByteBufferedRimpMessage.from(((BinaryMessage)message).getPayload(), false);


        }else if (message instanceof PongMessage){

        }else if (message instanceof TextMessage){

        }else if (message instanceof PingMessage){

        }

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
