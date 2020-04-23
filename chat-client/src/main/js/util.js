'use strict';

export function onEnter(cb) {
    return (event)=>{
        if(event.key!=='Enter') return;
        cb(event);
    }
}