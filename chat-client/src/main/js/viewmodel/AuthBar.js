import {onEnter} from "../util.js";
import RimpMessage from "../model/RimpMessage.js";

export class AuthBar {
    static STATE ={
        empty: 'empty',
        filled_invalid: 'invalid',
        filled_valid: 'valid',
        id_available: 'id_available',
        id_unavailable: 'id_unavailable',
        sent: 'sent'
    };
    static SENT_AUTO_TIMEOUT= 300;
    static M_PROMPT = 'enter id';
    static M_INVALID = 'bad id';
    static M_UNAVAILABLE = 'id taken';
    static M_AVAILABLE= 'id free';
    static M_SENT = 'wait';

    state;
    chatClient;
    authElement;
    inputElement;
    messageElement;

    constructor(client, authElement){
        this.chatClient=client;
        client.addReceiveListener(this.handleFindResponse.bind(this),RimpMessage.METHOD.FIND);
        client.addReceiveListener(this.handleJoinResponse.bind(this),RimpMessage.METHOD.JOIN);
        this.authElement=authElement;
        this.inputElement=authElement.querySelector('.auth-bar');
        this.inputElement.addEventListener('input', this.onInputChanged.bind(this));
        this.inputElement.addEventListener('keyup', onEnter(this.onInputEnter.bind(this)));
        this.messageElement=authElement.querySelector('.auth-text');

        this.state=AuthBar.STATE.empty;
    }

    set state(val){
        this.authElement.setAttribute('data-state',val); //todo ?
        switch (val) {
            case AuthBar.STATE.filled_valid:
                setTimeout(this.sendTimedFind.bind(this, this.inputElement.value), AuthBar.SENT_AUTO_TIMEOUT);
            case AuthBar.STATE.filled_invalid:
            case AuthBar.STATE.empty:
                this.setMessage(AuthBar.M_PROMPT);
                break;
            case AuthBar.STATE.id_unavailable:
                this.setMessage(AuthBar.M_UNAVAILABLE);
                break;
            case AuthBar.STATE.id_available:
                this.setMessage(AuthBar.M_INVALID);
                break;
            case AuthBar.STATE.sent:
                this.setMessage(AuthBar.M_SENT);
                break;
        }
    }

    setMessage(text){
        this.messageElement.innerHTML=text;
    }

    onInputChanged(){
        if (this.inputElement.value==='')
            this.state = AuthBar.STATE.empty;
        else
            this.state = this.chatClient.idRegExp.test(this.inputElement.value)
                ? AuthBar.STATE.filled_valid
                : AuthBar.STATE.filled_invalid;
    }

    onInputEnter(){
        if(this.state===AuthBar.STATE.filled_valid || this.state===AuthBar.STATE.id_available){
            this.state = AuthBar.STATE.sent;
            this.sendJoin(this.inputElement.value);
        }
    }

    sendTimedFind(id){
        if(id!==this.inputElement.value) return;
        this.chatClient.doFind(id);
    }

    handleFindResponse(msg){
        if(msg.headers.get(RimpMessage.HEADER.QUERY)===this.inputElement.value && this.state===AuthBar.STATE.filled_valid){
            this.state = msg.status === RimpMessage.STATUS.NOT_FOUND ? AuthBar.STATE.id_available : AuthBar.STATE.id_unavailable;
        }
    }

    sendJoin(id){
        this.state=AuthBar.STATE.sent;
        this.chatClient.doJoin(id);
    }

    handleJoinResponse(msg){
        if(msg.status!==RimpMessage.STATUS.OK)
            this.state=AuthBar.STATE.id_unavailable;
        // else
            //todo cleanup
    }


}