package bgu.spl.net.api.bidi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class DataBase {
    // fields
    private final ConcurrentHashMap<String, UserData> users = new ConcurrentHashMap<>();
    private String[] filteredWords;
    private final HashMap<String, Short> opcodes = new HashMap<>();

    // singleton
    private static class SingletonHolder {
        private static final DataBase instance = new DataBase();
    }
    private DataBase(){
        opcodes.put("register", (short)1);
        opcodes.put("login", (short)2);
        opcodes.put("logout", (short)3);
        opcodes.put("follow", (short)4);
        opcodes.put("post", (short)5);
        opcodes.put("pm", (short)6);
        opcodes.put("logstat", (short)7);
        opcodes.put("stat", (short)8);
        opcodes.put("notification", (short)9);
        opcodes.put("ack", (short)10);
        opcodes.put("error", (short)11);
        opcodes.put("block", (short)12);
    }
    public static DataBase getInstance() {return SingletonHolder.instance;}

    // methods
    public String[] getFilteredWords() {return filteredWords;}

    public void setFilteredWords(String[] filteredWords) {this.filteredWords = filteredWords;}

    public short getOpcode(String type) {return opcodes.get(type);}

    public UserData getUserData(String username) {return users.get(username);}

    public void addUser(String username, int conId, String password, String birthday) {
        users.put(username, new UserData(conId, password, birthday));
    }

    public boolean isRegistered(String username) {return users.containsKey(username);}

    public boolean isLogin(String username) {return isRegistered(username) && getUserData(username).isLogIn();}

    public boolean passwordMatch(String username, String password) {
        return getUserData(username).getPassword().equals(password);
    }

    public void login(String username) {getUserData(username).setLogIn(true);}

    public void logout(String username) {getUserData(username).setLogIn(false);}

    public boolean follow(String username, String toFollow) {
        if (isBlocked(toFollow, username) || isBlocked(username, toFollow))
            return false;
        else{
            getUserData(toFollow).addFollowMe(username);
            return getUserData(username).addFollow(toFollow);
        }
    }

    public boolean unfollow(String username, String toUnfollow) {
        if (getUserData(username).removeFollow(toUnfollow)){
            getUserData(toUnfollow).removeFollowMe(username);
            return true;
        } else
            return false;
    }

    public List<Integer> publishPost(String username, String post, List<String> extraUsers) {
        getUserData(username).addPost(post);
        List<Integer> recipients = new LinkedList<>();
        for (String user : extraUsers) {
            if (isLogin(user) && !isFollow(user,username) && !isBlocked(user, username))
                recipients.add(getUserData(user).getConnId());
            else {
                if (isRegistered(user) && !isFollow(user,username) && !isBlocked(user, username))
                    addNotification(user, new Message(opcodes.get("notification"), new String[]{"1", username, post}));
            }
        }
        for (String user : getUserData(username).getFollowMe()) {
            if (isLogin(user))
                recipients.add(getUserData(user).getConnId());
            else if (isRegistered(user))
                addNotification(user, new Message(opcodes.get("notification"), new String[]{"1", username, post}));
        }
        return recipients;
    }

    public int sendPM(String username, String sendTo, String msg, String sendingTime) {
        getUserData(username).addPM(new Message(opcodes.get("pm"), new String[] {sendTo, msg, sendingTime}));
        if (isLogin(sendTo))
            return getConId(sendTo);
        else{
            msg = msg + sendingTime.substring(0, sendingTime.length() - 6);
           addNotification(sendTo, new Message(opcodes.get("notification"), new String[]{"0", username, msg}));
           return -1;
        }
    }

    public boolean isFollow(String username, String isFollowed){
        return getUserData(username).getFollow().contains(isFollowed);
    }

    public int getConId(String username) {return getUserData(username).getConnId();}

    public List<Short> stat(String username) {
        List<Short> stat = new LinkedList<>();
        UserData userData = getUserData(username);
        stat.add((short)userData.getAge());
        stat.add((short)userData.getPosts().size());
        stat.add((short)userData.getFollowMe().size());
        stat.add((short)userData.getFollow().size());
        return stat;
    }

    public List<List<Short>> statAll(String username) {
        List<List<Short>> stats = new LinkedList<>();
        for (String user : users.keySet()) {
            if (!isBlocked(username, user) && !isBlocked(user, username) && isLogin(user))
                stats.add(stat(user));
        }
        return stats;
    }

    public boolean isBlocked(String username, String toCheck) {return getUserData(username).isBlocked(toCheck);}

    public void block(String username, String toBlock) {
        getUserData(username).addBlock(toBlock);
        unfollow(username, toBlock);
        unfollow(toBlock, username);
    }

    private void addNotification(String username, Message msg) {getUserData(username).addNotification(msg);}

    public Queue<Message> getNotification(String username) {return getUserData(username).getMissedNotifications();}

    public void clearNotificationQueue(String username) {getUserData(username).clearNotificationQueue();}
}
