import RimpClient from './model/RimpClient.js';
import RimpMessage from "./model/RimpMessage.js";

function panic(message) {
    console.log('PANIC '+message);
}

function dispose() {

}

function bindTransport() {
    chatClient = new RimpClient();
    chatClient.transferMessageImpl = (msg)=>{
        try {
            webSocket.send(msg.serialize());
        }catch (e) {
            console.log(e)
        }
    };
    webSocket.onmessage = (e)=>{
        // let msg;
        try {
            let msg = RimpMessage.deserialize(e.data);
            chatClient.receiveMessage(msg);
        }catch (e) {
           console.log(e) 
        }
    };
}

function start(){
    bindTransport();
}

let chatClient; //= new RimpClient();
const webSocketUrl = 'ws://'+window.location.host;
const webSocket = new WebSocket(webSocketUrl);
webSocket.onopen = start;
webSocket.onerror = ()=>dispose(),panic();
webSocket.onclose = ()=>dispose(),panic();

//elements:
const chat = document.querySelector('.chat');
const auth = document.querySelector('.auth');
