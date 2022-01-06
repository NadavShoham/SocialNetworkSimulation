package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;

import java.io.IOException;

public interface Connections<T> {

    void addCon(int connectionId, ConnectionHandler<T> handler);

    void transferCon(int FromConId, int toConId);

    boolean send(int connectionId, T msg);

    void broadcast(T msg);

    void disconnect(int connectionId);
}
