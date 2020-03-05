package dev.ramzanzan.chat.service;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class PeerRegistry<T> {

    private Map<String,T> sessions = new ConcurrentHashMap<>(1<<10);
    private PatriciaTrie<T> trie = new PatriciaTrie<>();
    private ReadWriteLock trieLock = new ReentrantReadWriteLock(true);

    public boolean tryAdd(String id, T userSession){
        trieLock.writeLock().lock();
        if(sessions.containsKey(id)) {
            trieLock.writeLock().unlock();
            return false;
        }
        sessions.put(id,userSession);
        trie.put(id,userSession);
        trieLock.writeLock().unlock();
        return true;
    }

    @Nullable
    public T get(String id){
        return sessions.get(id);
    }

    @Nullable
    public T remove(String id){
        if (!sessions.containsKey(id)) return null;
        trieLock.writeLock().lock();
        trie.remove(id);
        var r = sessions.remove(id);
        trieLock.writeLock().unlock();
        return r;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T[] find(String idPrefix, int maxCount){
        if(maxCount<1) throw new IllegalArgumentException("max < 1");
        trieLock.readLock().lock();
        var sessions = trie.prefixMap(idPrefix).values();
        if(sessions.isEmpty()) {
            trieLock.readLock().unlock();
            return null;
        }
        int count = Math.min(sessions.size(), maxCount);
        T[] res = (T[]) new Object[maxCount];
        var iter = sessions.iterator();
        for(int i=0; i<count; ++i )
            res[i]=iter.next();
        trieLock.readLock().unlock();
        return res;
    }

    public boolean contains(String id){
        return sessions.containsKey(id);
    }

}
