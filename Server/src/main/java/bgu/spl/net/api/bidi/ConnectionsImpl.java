package bgu.spl.net.api.bidi;


import bgu.spl.net.srv.ConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T>{

    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> clientConHandlers = new ConcurrentHashMap<>();

    @Override
    public void addCon(int connectionId, ConnectionHandler<T> handler) {
        clientConHandlers.putIfAbsent(connectionId, handler);
    }

    @Override
    public void transferCon(int fromConId, int toConId) {
        clientConHandlers.put(toConId, clientConHandlers.get(fromConId));
    }

    @Override
    public boolean send(int connectionId, T msg) {
        try {
            ConnectionHandler<T> handler = clientConHandlers.get(connectionId);
            assert handler != null;
            synchronized (handler) {
                handler.send(msg);
            }
        } catch (AssertionError e){
            return false;
        }
        return true;
    }

    @Override
    public void broadcast(T msg) {
        for (ConnectionHandler<T> handler: clientConHandlers.values()) {
            synchronized (handler) {
                handler.send(msg);
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        synchronized (clientConHandlers.get(connectionId)) {
            clientConHandlers.remove(connectionId);
        }
    }
    // TODO think if synchronized is relevant
}
