import {Buffer} from 'buffer';

export default class RimpMessage{

    static METHOD = {
        FIND: "FIND",
        JOIN: "JOIN",
        INVITE: "INVITE",
        SEND: "SEND",
        CLOSE: "CLOSE"
    };

    static HEADER = {
        QUERY: 'QUERY',
        FROM: 'FROM',
        TO: 'TO',
        MESSAGE_ID: 'MESSAGE-ID',
        CONTENT_TYPE: 'CONTENT-TYPE',
        BYTE_RANGE: 'BYTE-RANGE',
        ORDER: 'ORDER',
        REPORT: 'REPORT'
    };

    static RIMP = "RIMP";
    static CRLF = '\r\n';

    static newRequest(method, headers, data){
        if(!headers) throw Error("headers is empty");
        if(data!==undefined && (!data instanceof Buffer)) throw Error("data must be Buffer");
        //mass of checks
        //but not today
        let request = new RimpMessage();
        request.method=method;
        request.headers=headers;
        request.data=data;
        return request;
    }

    static newResponse(request, status){
        if(request.method!==this.METHOD.SEND || request.method!==this.METHOD.INVITE)
            throw Error("bad request method");
        let response = new RimpMessage();
        response.method=request.method;
        response.status=status;
        response.headers = new Map();
        response.headers.set(this.HEADER.FROM, request.headers.get(this.HEADER.FROM));
        response.headers.set(this.HEADER.TO, request.headers.get(this.HEADER.TO));
        if(request.method!==this.METHOD.SEND) {
            response.headers.set(this.HEADER.MESSAGE_ID, request.headers.get(this.HEADER.MESSAGE_ID));
        }
        return response;
    }

    get isRequestNotResponse(){
        return this.status===undefined;
    }
    method;
    status;
    headers = new Map();
    data;
    get hasData(){
        return this.data!==undefined;
    }

    set status(value){
        //todo test
        this.status = value;
    }

    set method(value){
        value = value.toUpperCase();
        if(!RimpMessage.METHOD.hasOwnProperty(value))
           throw Error("There aren't such RIMP's method: "+value);
        this.method = value;
    }

    addHeader(name, value){
        name = name.toUpperCase();
        if(!RimpMessage.HEADER.hasOwnProperty(name))
            throw Error("There aren't such RIMP's header: "+name);
        this.headers.set(name,value);
    }

    static deserialize(buffer){
        if(!(buffer instanceof ArrayBuffer)) throw Error("buffer isn't ArrayBuffer");
        let buf = Buffer.from(buffer);
        let msg = new RimpMessage();
        let idx, preIdx;
        idx = preIdx = buf.indexOf(this.CRLF,0,"utf8");
        if(idx===-1) throw Error('first crlf not found');
        let tmp2, tmp1 = buf.toString("utf8",0,idx).toUpperCase();
        [tmp2, msg.method, msg.status] = tmp1.split(' ');
        if (tmp2!==this.RIMP) throw Error("It is not RIMP: "+tmp2);
        for(preIdx+=2, idx=buf.indexOf(this.CRLF,preIdx); preIdx<idx; preIdx=idx+2,idx=buf.indexOf(this.CRLF,preIdx)){
           tmp1 = buf.toString('utf8',preIdx,idx);
           msg.addHeader(...tmp1.split(':',2));
        }
        if (idx===-1) throw Error(`Bad headers sequence, bufIndex: ${preIdx}`);
        preIdx+=2;
        if (buffer.byteLength>preIdx)
            msg.data = Buffer.from(buffer,preIdx);
        return msg;
    }


    serialize(){
        let head = RimpMessage.RIMP.concat(' ',this.method);
        if(!this.isRequestNotResponse)
            head = head.concat(' ',this.status);
        head += RimpMessage.CRLF;
        for(let [k,v] of this.headers){
            head = head.concat(k,':',v,RimpMessage.CRLF)
        }
        head += RimpMessage.CRLF;
        let headBuf = Buffer.from(head);
        let res = this.hasData ? Buffer.concat([headBuf, this.data]) : headBuf;
        return res;
        //todo del or not to del
        // return res.buffer.slice(res.byteOffset,res.byteOffset+res.byteLength);
    }

    toString(){
        return `Method: ${this.method} Status: ${this.status}\n` + this.headers;
    }
}