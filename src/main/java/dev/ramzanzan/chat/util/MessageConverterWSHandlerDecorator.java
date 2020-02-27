package dev.ramzanzan.chat.util;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.util.function.Function;

public class MessageConverterWSHandlerDecorator<T,R extends WebSocketMessage> extends WebSocketHandlerDecorator {

    private Function<WebSocketMessage<T>,R> converter;

    public MessageConverterWSHandlerDecorator(WebSocketHandler delegate, Function<WebSocketMessage<T>,R> converter){
        this(delegate);
        this.converter=converter;
    }

    public MessageConverterWSHandlerDecorator(WebSocketHandler delegate) {
        super(delegate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, converter.apply((WebSocketMessage<T>) message));
    }
}
