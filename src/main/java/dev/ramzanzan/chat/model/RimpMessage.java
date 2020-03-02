package dev.ramzanzan.chat.model;

import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/***
 * Ramzanzan instant messaging protocol
 */
@Getter
public abstract class RimpMessage<D,S> {

    public enum Method{
        FIND,
        JOIN,
        INVITE,
        SEND,
        CLOSE;

        private static HashMap<String,Method> map = new HashMap<>();

        static {
            for(var m : Method.values())
                map.put(m.name(),m);
        }

        public static Method getValue(String _str){
            return map.get(_str);
        }
    }

    protected static final String RIMP = "RIMP";
    protected static final Random random = new Random(new Date().getTime());

    protected boolean isRequestNotResponse;
    protected int statusCode;
    protected Method method;
    protected MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
    protected D data;

    public RimpMessage(Method _method){
        method = _method;
        isRequestNotResponse = true;
    }

    public RimpMessage(int _statusCode, Method _method){
        statusCode = _statusCode;
        method=_method;
        isRequestNotResponse =false;
    }

    public RimpMessage<D,S> setId(String _id){
        return this;
    }

    public RimpMessage<D,S> addHeader(String _header, String _value){
        headers.add(_header,_value);
        return this;
    }

    public RimpMessage<D,S> setHeaders(MultiValueMap<String,String> _headers){
        headers=_headers;
        return this;
    }

    public RimpMessage<D,S> setData(D _data){
        data=_data;
        return this;
    }

    public int getStatusCode(){
        if(isRequestNotResponse) throw new IllegalStateException();
        return statusCode;
    }

    public Method getMethod(){
        return method;
    }

    public abstract S serialize();
}
