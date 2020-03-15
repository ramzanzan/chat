package ramzanzan.chat.exceptions;

import lombok.Getter;

import java.io.IOException;

public class RimpIOException extends IOException {

    @Getter
    private String sessionId;

    public RimpIOException(String _sessionId, String _msg, Throwable _cause){
        super(_msg,_cause);
        sessionId=_sessionId;
    }

    public RimpIOException(String _sessionId, Throwable _cause){
        super(_cause);
        sessionId=_sessionId;
    }

    public RimpIOException(String _sessionId, String _msg){
        super(_msg);
        sessionId=_sessionId;
    }

    @Override
    public String toString() {
        return "SessionId: "+sessionId+" ; "+super.toString();
    }
}
