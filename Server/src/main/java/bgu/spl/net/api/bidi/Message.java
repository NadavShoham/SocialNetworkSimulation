package bgu.spl.net.api.bidi;

import java.util.Arrays;

public class Message {
    enum Type {REGISTER, LOGIN, LOGOUT, FOLLOW, POST, PM, LOGSTAT, STAT, NOTIFICATION, ACK, ERROR, BLOCK};
    private short opcode;
    private String[] msg;
    private Short[] stats;
    private Type type;
    private boolean usingStrings;
    // TODO timestamps?

    public Message(short opcode, Short[] stats){
        this.opcode = opcode;
        this.stats = stats;
        this.usingStrings = false;
        initiateType();
    }

    public Message(short opcode, String[] msg) {
        this.opcode = opcode;
        this.msg = msg;
        this.usingStrings = true;
        initiateType();

    }

    private void initiateType() {
        switch (opcode){
            case 1:
                type = Type.REGISTER;
                break;
            case 2:
                type = Type.LOGIN;
                break;
            case 3:
                type = Type.LOGOUT;
                break;
            case 4:
                type = Type.FOLLOW;
                break;
            case 5:
                type = Type.POST;
                break;
            case 6:
                type = Type.PM;
                break;
            case 7:
                type = Type.LOGSTAT;
                break;
            case 8:
                type = Type.STAT;
                break;
            case 9:
                type = Type.NOTIFICATION;
                break;
            case 10:
                type = Type.ACK;
                break;
            case 11:
                type = Type.ERROR;
                break;
            case 12:
                type = Type.BLOCK;
                break;
        }
    }

    public Type getType() {
        return type;
    }

    public short getOpcode() {
        return opcode;
    }

    public String[] getMsg() {
        return msg;
    }

    public Short[] getShortMsg() {return stats;}

    public boolean isUsingStrings() {
        return usingStrings;
    }

    @Override
    public String toString() {
        String result;
        switch (type) {
            case LOGOUT:
            case LOGSTAT:
                result = type.toString();
                break;
            default:
                if (msg != null) result = type + ": msg=" + Arrays.toString(msg);
                else result = type + ":msg=" + Arrays.toString(stats);
        }
        return result;
    }

    public void addToEndMsgArgs(String s) {
        String[] newMsg = new String[msg.length + 1];
        System.arraycopy(msg, 0, newMsg, 0, msg.length);
        newMsg[newMsg.length - 1] = s;
        msg = newMsg;
    }

    public void addToStartMsgArgs(String s) {
        String[] newMsg = new String[msg.length + 1];
        System.arraycopy(msg, 0, newMsg, 1, msg.length);
        newMsg[0] = s;
        msg = newMsg;
    }
}
