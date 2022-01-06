package bgu.spl.net.api.bidi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

public class UserData {
    // fields
    private final int connId;
    private String password;
    private final String birthDay;
    private boolean logIn;
    private final HashSet<String> follow;
    private final HashSet<String> followMe;
    private final List<String> posts;
    private final List<Message> pm;
    private final HashSet<String> blocked;
    private final Queue<Message> missedNotifications;

    // methods
    public UserData(int connId, String password, String birthDay){
        this.connId = connId;
        this.password = password;
        this.birthDay = birthDay;
        this.logIn = false;
        this.follow = new HashSet<>();
        this.followMe = new HashSet<>();
        this.posts = new LinkedList<>();
        this.pm = new LinkedList<>();
        this.blocked = new HashSet<>();
        this.missedNotifications = new LinkedList<>();
    }

    public int getConnId() {return connId;}

    public void setLogIn(boolean logIn) {this.logIn = logIn;}

    public String getPassword() {return password;}

    public boolean isLogIn() {return logIn;}

    public HashSet<String> getFollow() {return follow;}

    public HashSet<String> getFollowMe() {return followMe;}

    public List<String> getPosts() {return posts;}

    public Queue<Message> getMissedNotifications() {return missedNotifications;}

    public int getAge() {
        // TODO check me
        Date birthday;
        try {
            birthday = new SimpleDateFormat("dd-MM-yyyy").parse(this.birthDay);
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
        LocalDate birthdate = birthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Period period = Period.between(birthdate, LocalDate.now());
        return period.getYears();
    }

    public boolean addFollow(String username) {
        if (follow.contains(username)) return false;
        else follow.add(username);
        return true;
    }

    public void addFollowMe(String username) {followMe.add(username);}

    public boolean removeFollow(String username) {
        if (!follow.contains(username)) return false;
        else follow.remove(username);
        return true;
    }

    public void removeFollowMe(String username) {followMe.remove(username);}

    public void addPost(String post) {posts.add(post);}

    public void addPM(Message currPM) {
        pm.add(currPM);
    }

    public void addNotification(Message msg) {missedNotifications.add(msg);}

    public void clearNotificationQueue() {missedNotifications.clear();}

    public boolean isBlocked(String username) {return blocked.contains(username);}

    public void addBlock(String username) {blocked.add(username);}
}
