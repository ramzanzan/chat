package dev.ramzanzan.chat.model;


import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

public class UserSession {

    private String id;
    private String authToken;
    private WebSocketSession wsSession;
    private Set<UserSession> outboundInvites;
    private Set<UserSession> inboundInvites;
    private Set<UserSession> dialogs;

    public UserSession(WebSocketSession _wss, String _id, String _authToken){

    }

    public void send(WebSocketMessage<?> wsm){

    }

    public void close(){

    }
}
