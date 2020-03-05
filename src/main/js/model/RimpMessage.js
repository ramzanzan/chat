class RimpMessage{

    static METHOD = {
        FIND: "FIND",
        JOIN: "JOIN",
        INVITE: "INVITE",
        SEND: "SEND",
        CLOSE: "CLOSE"
    };

    static RIMP = "RIMP";
    static MESSAGE_ID = "MESSAGE-ID";

    static makeRequest(method,data,headers){
        if(!headers) throw Error("headers is empty");
        //mass of checks
        //but not today
        let request = new RimpMessage();
        request.method=method;
        request.headers=headers;
        request.data=data;
        request.isRequestNotResponse=true;
        return request;
    }

    static makeResponse(request,statusCode){
        if(request.method!==this.METHOD.SEND || request.method!==this.METHOD.INVITE)
            throw Error("bad request method");
        let response = new RimpMessage();
        response.method=request.method;
        response.statusCode=statusCode;
        response.headers = { FROM:null, TO:null, MESSAGE_ID:null };
        response.headers.FROM = request.headers.FROM;
        response.headers.TO = request.headers.TO;
        if(request.method!==this.METHOD.SEND) {
            response.headers.MESSAGE_ID = request.headers.MESSAGE_ID;
        }
        response.isRequestNotResponse=false;
        return response;
    }

    isRequestNotResponse;
    method;
    headers;
    statusCode;
    data;

    deserialize(buffer){
        if(!(buffer instanceof ArrayBuffer)) throw Error("buffer isn't ArrayBuffer");
        let view = new Uint8Array(buffer);
        String.fr
    }

    serialize(){
        return new ArrayBuffer();
    }
}