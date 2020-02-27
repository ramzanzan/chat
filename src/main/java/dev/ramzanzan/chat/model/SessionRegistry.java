package dev.ramzanzan.chat.model;

public class SessionRegistry<T> {


    public void addUser(String id, T userSession){

    }

    public T getUser(String id){
        return null;
    }

    public T removeUser(String id){
        return null;
    }

    public T[] findUsers(String idPrefix, int max){
        return null;
    }

    public boolean containsUser(String id){
        return false;
    }

}
