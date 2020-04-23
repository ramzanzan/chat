import RimpMessage from "./RimpMessage.js";

export default class RimpClient{

    config = {
        GLOB_CHAR: undefined,
        ID_MAX_CHARS: undefined,
        ID_MIN_CHARS: undefined,
        ID_PATTERN: undefined,
    };
    idRegExp;
    receiveCallbacks = new Map();
    transferCallbacks = new Map();

    constructor(){
    }

    async transferMessageImpl(msg, priority){
        throw Error('implementation must be inserted')
    }

    async transferMessage(msg, priority = 0){
        //here can be priority queue but there are no need
        await this.transferMessageImpl(msg,priority);

        if (cbs !== undefined)
            // cbs.forEach(()=>new Promise(()=>cb(msg)));
            cbs.forEach((cb)=>cb(msg));
        let cbs = this.transferCallbacks.get(msg.method);
        let to = msg.headers.get(RimpMessage.HEADER.TO).map;
        if(to !== undefined && (cbs = this.transferCallbacks)!==undefined)
            // cbs.forEach(()=>new Promise(()=>cb(msg)));
            cbs.forEach((cb)=>cb(msg));
    }

    receiveMessage(msg){
        //todo
        // switch (msg.method) {
        //     case RimpMessage.METHOD.FIND: this.handleFindResponse(msg);
        // }

        let cbs = this.receiveCallbacks.get(msg.method);
        if (cbs !== undefined)
            cbs.forEach((cb)=>cb(msg));
        let from = msg.headers.get(RimpMessage.HEADER.FROM).map;
        if(from !== undefined && (cbs = this.receiveCallbacks)!==undefined)
            cbs.forEach((cb)=>cb(msg));
    }

    addTransferListener(cb, method, to = ''){
        if(!RimpMessage.METHOD.hasOwnProperty(method.toUpperCase())) throw Error("bad method");
        let key = method.toUpperCase()+to;
        let cbSet = this.transferCallbacks.get(key);
        if(cbSet === undefined){
            this.transferCallbacks.set(key, cbSet = new Set());
        }
        cbSet.add(cb);
    }

    addReceiveListener(cb, method, from = ''){
        if(!RimpMessage.METHOD.hasOwnProperty(method.toUpperCase())) throw Error("bad method");
        let key = method.toUpperCase()+from;
        let cbSet = this.receiveCallbacks.get(key);
        if(cbSet === undefined){
            this.receiveCallbacks.set(key, cbSet = new Set());
        }
        cbSet.add(from);
    }

    removeListener(cb, method, transferNotReceive, toOrFrom = ''){
        if(!RimpMessage.METHOD.hasOwnProperty(method.toUpperCase())) throw Error("bad method");
        let key = method.toUpperCase() + toOrFrom;
        (transferNotReceive ? this.transferCallbacks : this.receiveCallbacks).get(key).delete(cb);
    }

    doFind(query, glob = false){
        if(query.length<this.config.ID_MIN_CHARS
            || query.length>this.config.ID_MAX_CHARS
            || !this.idRegExp.test(query)) throw Error("bad query");
        let msg = RimpMessage.newRequest(RimpMessage.METHOD.FIND);
        msg.addHeader(RimpMessage.HEADER.QUERY,query+(glob ? '' : this.config.GLOB_CHAR));
        this.transferMessage(msg).catch(console.log.bind(console));//todo !!
    }

    doJoin(){

    }

    doInvite(){

    }

    doSend(){

    }

    doClose(){

    }




}