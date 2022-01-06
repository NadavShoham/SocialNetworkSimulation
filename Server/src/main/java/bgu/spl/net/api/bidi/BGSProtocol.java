package bgu.spl.net.api.bidi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BGSProtocol implements BidiMessagingProtocol<Message>{
    // fields
    private boolean shouldTerminate = false;
    private Connections<Message> connections;
    private int connectionId;
    private final DataBase dataBase;
    private String username;
    private String opcode;
    private String[] msgArgs;

    // methods
    public BGSProtocol(DataBase dataBase){
        this.dataBase = dataBase;
        this.username = " "; // to avoid null pointer
    }

    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connections = connections;
        this.connectionId = connectionId;
    }

    @Override
    public void process(Message message) {
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.println("[" + time + "]: " + "Client #" + connectionId + " <-- " + message);
        this.opcode = Short.toString(message.getOpcode());
        this.msgArgs = message.getMsg();
        switch (message.getType()) {
            case REGISTER: register();
                break;
            case LOGIN: login();
                break;
            case LOGOUT: logout();
                break;
            case FOLLOW: follow();
                break;
            case POST: post();
                break;
            case PM: pm();
                break;
            case LOGSTAT: logstat();
                break;
            case STAT: stat();
                break;
            case BLOCK: block();
        }
    }

    @Override
    public boolean shouldTerminate() {return shouldTerminate;}

    private void send(int connectionId, Message msg) {
        connections.send(connectionId, msg);
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.println("[" + time + "]: " + "Client #" + connectionId + " --> " + msg);
    }

    /**
     * Registers the user to the server
     * Error if already registered
     * */
    private void register(){
        Message response;
        if (dataBase.isRegistered(msgArgs[0]))
            response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        else {
            dataBase.addUser(msgArgs[0], connectionId, msgArgs[1], msgArgs[2]);
            response = new Message(dataBase.getOpcode("ack"), new String[] {opcode});
        }
        send(connectionId, response);

    }

    /**
     * Logs the user in to the server, sends all missed notifications
     * Error if not registered, already logged in, password doesn't match or capcha is 0.
     * */
    private void login(){
        this.username = msgArgs[0];
        if (!dataBase.isRegistered(username) || dataBase.isLogin(username) ||
                !dataBase.passwordMatch(username, msgArgs[1]) || msgArgs[2].equals("0"))
            send(connectionId, new Message(dataBase.getOpcode("error"), new String[] {opcode}));
        else {
            dataBase.login(username);

            // in case of re-login after logout
            int newId = connectionId;
            connectionId = dataBase.getConId(username);
            connections.transferCon(newId, connectionId);
            send(connectionId, new Message(dataBase.getOpcode("ack"), new String[] {opcode}));

            // sends all missed notifications to user
            Queue<Message> notifications = dataBase.getNotification(username);
            for (Message notification :notifications) send(connectionId, notification);
            dataBase.clearNotificationQueue(username);
        }
    }

    /**
     * Logs the user out of the server
     * Error if not logged in
     * */
    private void logout(){
        Message response;
        if (!dataBase.isLogin(username)) response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        else {
            shouldTerminate = true;
            dataBase.logout(username);
            response = new Message(dataBase.getOpcode("ack"), new String[] {opcode});
        }
        send(connectionId, response);
    }

    /**
     * update user's follow list
     * Error if the user is not logged in
     *       or is already follows/unfollows him
     *       or if the one to follow blocked this user
     */
    private void follow(){
        String targetUser = msgArgs[1];
        Message response;
        //user wants to follow someone
        if (msgArgs[0].equals("0")){
            if(dataBase.isLogin(username) && dataBase.isRegistered(targetUser) && dataBase.follow(username, targetUser))
                response = new Message(dataBase.getOpcode("ack"), new String[] {opcode, targetUser});
            else
                response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        } //user wants to unfollow someone
        else{
            if (dataBase.isLogin(username) && dataBase.isRegistered(targetUser) && dataBase.unfollow(username, targetUser))
                response = new Message(dataBase.getOpcode("ack"), new String[] {opcode, targetUser});
            else
                response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        }
        send(connectionId, response);
    }
    // TODO continue check from here
    private void post(){
        Message response;
        if (!dataBase.isLogin(username))
            response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        else {
           List<String> extraNames = new LinkedList<>();
           String post = msgArgs[0];
           String[] findNames = post.split("@");
           for (String potentialName : findNames){
               int endInd = potentialName.indexOf(" ");
               if (endInd != -1) extraNames.add(potentialName.substring(0, endInd));
               else extraNames.add(potentialName);
           }
           extraNames.remove(0);
           for (Integer connId: dataBase.publishPost(username, post, extraNames))
               send(connId, new Message(dataBase.getOpcode("notification"), new String[] {"1", username, post}));
           response = new Message(dataBase.getOpcode("ack"), new String[] {opcode});
        }
        send(connectionId, response);
    }

    private void pm() {
        Message response;
        String recipient = msgArgs[0];
        String pm = msgArgs[1];
        String sendingTime = msgArgs[2];
        if (!dataBase.isLogin(username) || !dataBase.isRegistered(recipient) || !dataBase.isFollow(username, recipient))
            response = new Message(dataBase.getOpcode("error"), new String[]{opcode});
        else {
            String[] filteredWords = dataBase.getFilteredWords();
            for (String forbidden : filteredWords)
                pm = pm.replace(forbidden, "<filtered>");
            response = new Message(dataBase.getOpcode("ack"), new String[]{opcode});
            int idToSend = dataBase.sendPM(username, recipient, pm, sendingTime);
            if (idToSend != -1){
                pm = pm + sendingTime.substring(0, sendingTime.length() - 6);
                send(idToSend, new Message(dataBase.getOpcode("notification"), new String[]{"0", username, pm}));
            }

        }
        send(connectionId, response);
    }

    /**
     * Returns the stats of all users that didnt block the user
     * Error if user is not logged in
     * */
    private void logstat(){
        if (!dataBase.isLogin(username))
            send(connectionId, new Message(dataBase.getOpcode("error"), new String[] {opcode}));
        else {
            List<List<Short>> stats = dataBase.statAll(username);
            for (List<Short> stat : stats) {
                stat.add(0, Short.valueOf(opcode));
                send(connectionId, new Message(dataBase.getOpcode("ack"), stat.toArray(new Short[0])));
            }
        }
    }

    /**
     * Returns the stats of a given list of users that didnt block the user
     * Error if user is not logged in or one of the given users is not registered
     * */
    private void stat(){
        // error if not logged in
        if (!dataBase.isLogin(username))
            send(connectionId, new Message(dataBase.getOpcode("error"), new String[] {opcode}));
        else {
            String[] users = msgArgs[0].split("\\|");
            boolean invalidUser = false;

            // first loop check of every user exists
            for (String user: users) {
                if (!dataBase.isRegistered(user) || dataBase.isBlocked(username, user) || dataBase.isBlocked(user, username)) {
                    send(connectionId, new Message(dataBase.getOpcode("error"), new String[] {opcode}));
                    invalidUser = true;
                    break;
                }
            }

            // second loop sends the relevant stats
            if (!invalidUser) {
                for (String user : users) {
                    if (!dataBase.isBlocked(user, username)) {
                        List<Short> stat = dataBase.stat(user);
                        stat.add(0, Short.valueOf(opcode));
                        send(connectionId, new Message(dataBase.getOpcode("ack"), stat.toArray(new Short[0])));
                    }
                }
            }
        }
    }

    /**
     * Blocks a user from sending messages, following and getting info on username
     * Error if user to block doesn't exist or username isnt logged in or user already blocked
     * */
    private void block(){
        Message response;
        String userToBlock = msgArgs[0];
        if (!dataBase.isLogin(username) || !dataBase.isRegistered(userToBlock) || dataBase.isBlocked(username, userToBlock))
            response = new Message(dataBase.getOpcode("error"), new String[] {opcode});
        else {
            dataBase.block(username, userToBlock);
            response = new Message(dataBase.getOpcode("ack"), new String[] {opcode});
        }
        send(connectionId, response);

    }
}
