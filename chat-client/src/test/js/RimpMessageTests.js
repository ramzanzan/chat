'use strict';
import test from 'ava';
import {Buffer} from "buffer";
import RimpMessage from "../../main/js/model/RimpMessage.js";

function nb(...args){
    return new Buffer.from(args.join(''),'utf8');
}

function nab(...args){
    let b = new Buffer.from(args.join(''),'utf8');
    return b.buffer.slice(b.byteOffset,b.byteOffset+b.byteLength);
}

let crlf = '\r\n';
let send = 'SEND';
let protoStrReq = 'rimp send\r\n';
let protoStrResp = 'rimp send 200\r\n';
let bProtoStrReq = 'rimp send\n';
let gFrom = 'from:kek'+crlf;
let data = 'data';

test('good empty',t=>{
    let m = RimpMessage.deserialize(nab(protoStrReq,crlf));
    t.is(send,m.method);
    t.is(undefined,m.data);
});

test('bad empty',t=>{
    let f = RimpMessage.deserialize.bind(RimpMessage,nab(bProtoStrReq+crlf));
    t.throws(f,{instanceOf: Error});
});

test('header and data',t=>{
    let m = RimpMessage.deserialize(nab(protoStrReq+gFrom+crlf+data));
    t.is('kek',m.headers.get(RimpMessage.HEADER.FROM));
    t.is(data,m.data.toString())
});

test('header, NO crlf, data',t=>{
    try {
        let m = RimpMessage.deserialize(nab(protoStrReq+gFrom+data));
    }catch (e) {
        t.true(e instanceof Error, e.message);
    }
});

test('serialize',t=>{
    let m = RimpMessage.newRequest('INVITE', new Map(), Buffer.from("some_мультикультурал_DAta"));
    m.addHeader('To','kek');
    let s = m.serialize();
    t.is('RIMP INVITE\r\nTO:kek\r\n\r\nsome_мультикультурал_DAta', s.toString());
});
