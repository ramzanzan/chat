package dev.ramzanzan.chat.model;

import org.springframework.web.socket.WebSocketMessage;

import java.nio.ByteBuffer;

public class ProtocolizedBinaryMessage implements WebSocketMessage<ByteBuffer> {

    private String protocolString;

    public ProtocolizedBinaryMessage(String _protocolString){

    }

    public ProtocolizedBinaryMessage setHeaders(){

        return this;
    }

    public ProtocolizedBinaryMessage setContent(){

        return this;
    }

    public ProtocolizedBinaryMessage setTerminator(){
        return this;
    }

    public ProtocolizedBinaryMessage build(){
        return this;
    }

    public boolean isBuild(){
        return false;
    }

    @Override
    public ByteBuffer getPayload() {
        return null;
    }

    @Override
    public int getPayloadLength() {
        return 0;
    }

    @Override
    public boolean isLast() {
        return false;
    }
}
