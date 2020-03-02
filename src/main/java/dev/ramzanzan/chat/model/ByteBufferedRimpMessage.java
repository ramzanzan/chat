package dev.ramzanzan.chat.model;

import dev.ramzanzan.chat.util.Util;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ByteBufferedRimpMessage extends RimpMessage<ByteBuffer,ByteBuffer> {

    private Charset charset = StandardCharsets.UTF_8;
    private ByteBuffer serialized;
    private CharsetEncoder encoder;
    private boolean changed;

    public ByteBufferedRimpMessage(Method _method) {
        super(_method);
    }

    public ByteBufferedRimpMessage(String _id, int _statusCode, Method _method) {
        super(_id, _statusCode,_method);
    }

    @Override
    public RimpMessage<ByteBuffer, ByteBuffer> addHeader(String _header, String _value) {
        changed=true;
        return super.addHeader(_header, _value);
    }

    @Override
    public RimpMessage<ByteBuffer, ByteBuffer> setHeaders(MultiValueMap<String, String> _headers) {
        changed=true;
        return super.setHeaders(_headers);
    }

    @Override
    public RimpMessage<ByteBuffer, ByteBuffer> setData(ByteBuffer _data) {
        //todo cloning?
        changed=true;
        return super.setData(_data);
    }

    /**
     * @return must be used as readonly
     */
    @Override
    public MultiValueMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public ByteBuffer getData() {
        var bb = data.asReadOnlyBuffer();
        bb.clear();
        return bb;
    }

    public ByteBufferedRimpMessage setCharset(Charset _charset){
        charset=_charset;
        changed=true;
        return this;
    }

    @Override
    public ByteBuffer serialize() {
        if(!changed) return serialized;
        if(encoder==null) encoder = charset.newEncoder();
        else if(!encoder.charset().equals(charset)) encoder = charset.newEncoder();
        //todo
        throw new UnsupportedOperationException();
    }

    public static ByteBufferedRimpMessage from(ByteBuffer _bb, boolean _newBackedBuffer) throws ParseException{
        var bb = _newBackedBuffer ? Util.deepClone(_bb) : _bb;
        var pline = parseProtocolLine(bb);
        ByteBufferedRimpMessage message = pline.length==4
                ? new ByteBufferedRimpMessage(pline[1],Integer.parseInt(pline[2]),Method.getValue(pline[3]))
                : new ByteBufferedRimpMessage(Method.getValue(pline[2]));
        message.setId(pline[1]);
        var headers = parseHeaders(bb,message.headers);
        message.setHeaders(headers);
        message.setData(bb.slice());
        bb.clear();
        message.serialized = bb;
        message.changed = false;
        return message;
    }

    /***
     * Valid string: "word( word){2,3}\r\n"
     * where word: [a-zA-Z0-9_-]+
     * else ParseException will be thrown
     * Line are returned in uppercase
     */
    protected static String[] parseProtocolLine(ByteBuffer _bb) throws ParseException {
        var sb = new StringBuilder();
        byte cr='\r', lf='\n';
        byte b =_bb.get();
        while (b!=cr){
            b = _bb.get();
            sb.append(b);
        }
        if(_bb.get()!=lf) throw new ParseException("Malformed protocol line: bad CRLF", _bb.position());
        var line = sb.toString().toUpperCase();
        var words = line.split(" ");
        if(words.length!=3 && words.length!=4)
            throw new ParseException("Malformed protocol line, wrong word count: \""+line+"\"",0);
        if(Arrays.stream(words ).anyMatch(String::isEmpty))
            throw new ParseException("Malformed protocol line, too much spaces: \""+line+"\"",0);
        return words;
    }

    /***
     * Valid header: "word:word\r\n" , where word: [a-zA-Z0-9_-]+
     * else ParseException will be thrown
     * Header names are returned in uppercase
     */
    protected static MultiValueMap<String,String> parseHeaders(ByteBuffer _bb,@Nullable MultiValueMap<String,String> _map)
            throws ParseException
    {
        var hdrs = _map != null ? _map : new LinkedMultiValueMap<String,String>();
        var sb = new StringBuilder(64);
        byte cr='\r', lf='\n';
        while (true) {
            byte b = _bb.get();
            while (b != cr) {
                b = _bb.get();
                sb.append(b);
            }
            if(_bb.get()!=lf) throw new ParseException("Malformed protocol line: bad CRLF", _bb.position());
            if(sb.length()==0) return hdrs;
            var line = sb.toString();
            var pair = line.split(":");
            if(pair.length!=2 || Arrays.stream(pair).anyMatch(String::isEmpty))
                throw new ParseException("Malformed header line: \""+line+"\"",0);
            hdrs.add(pair[0].toUpperCase(),pair[1]);
        }
    }

}
