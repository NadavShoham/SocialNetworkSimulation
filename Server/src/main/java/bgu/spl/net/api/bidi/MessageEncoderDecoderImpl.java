package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<Message> {
    int len = 0;
    byte[] bytes = new byte[1<< 10];
    short opcode = -1;

    @Override
    public Message decodeNextByte(byte nextByte) {

        if (opcode == -1){
            if (len == 1) {
                pushByte(nextByte);
                opcode = bytesToShort();
                len = 0;
            } else {
                pushByte(nextByte);
            }
        } else if (nextByte == ';') {
            switch (opcode) {
                case 2:
                    byte captcha = bytes[--len];
                    Message message = oneString(0);
                    message.addToEndMsgArgs(oneByteToString(captcha));
                    opcode = -1;
                    return message;
                case 4:
                    byte follow = bytes[0];
                    Message message1 = oneString(1);
                    message1.addToStartMsgArgs(oneByteToString(follow));
                    opcode = -1;
                    return message1;
                default:
                    Message msg = oneString(0);
                    opcode = -1;
                    return msg;
            }
        } else
            pushByte(nextByte);
        return null; //not a line yet

    }

    @Override
    public byte[] encode(Message message) {
        switch (message.getOpcode()){
            case 9:
                return buildingString(message);
            case 10:
                if (message.isUsingStrings()){
                   if (message.getMsg()[0].equals("4"))
                       return buildingString(message);
                   else
                       return onlyOp(message);
                } else
                    return buildingShort(message);
            case 11:
                return onlyOp(message);
        }
        return null;
    }

    private short bytesToShort() {
        short result = (short)((bytes[0] & 0xff) << 8);
        result += (short)(bytes[1] & 0xff);
        return result;
    }

    private byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private String popString(int start, int end) {
        //notice that we're explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, start, end - start, StandardCharsets.UTF_8);
        return result;
    }

    private Message oneString(int startIdx){
        LinkedList<String> args = new LinkedList<>();
        int start = startIdx;
        int end = start + 1;
        String nextString;
        do {
            while (bytes[end] != 0) {
                end++;
            }
            nextString = popString(start, end);
            args.add(nextString);
            start = end + 1;
            end = start + 1;
        } while (bytes[start] != 0 && start < len);
        Object[] argsObject =  args.toArray();
        String[] msgArgs = Arrays.copyOf(argsObject, argsObject.length, String[].class);
        len = 0;
        return new Message(opcode, msgArgs);
    }

    private String oneByteToString(byte b) {
        if (b == 0)
            return "0";
        else
            return "1";
    }

    private byte[] buildingString(Message msg){
        StringBuilder s = new StringBuilder();
        String[] msgArgs = msg.getMsg();
        for (int i = 1; i < msgArgs.length; i++){
            s.append(msgArgs[i]);
            s.append("\0");
        }
        s.append(";");
        String output = s.toString();
        byte[] tmpArr = output.getBytes(StandardCharsets.UTF_8);
        byte[] result;
        if (msg.getOpcode() == 9){
            result = new byte[tmpArr.length + 3];
            System.arraycopy(tmpArr, 0, result, 3, tmpArr.length);
            byte[] op = shortToBytes(msg.getOpcode());
            result[0] = op[0];
            result[1] = op[1];
            //TODO check if the byte value really works
            result[2] = Short.valueOf(msgArgs[0]).byteValue();
        } else {
            result = new byte[tmpArr.length + 4];
            System.arraycopy(tmpArr, 0, result, 4, tmpArr.length);
            byte[] op = shortToBytes(msg.getOpcode());
            result[0] = op[0];
            result[1] = op[1];
            op = shortToBytes(Short.parseShort(msg.getMsg()[0]));
            result[2] = op[0];
            result[3] = op[1];
        }
        return result;
    }

    private byte[] onlyOp(Message msg) {
        byte[] bytes = new byte[5];
        byte[] op = shortToBytes(msg.getOpcode());
        bytes[0] = op[0];
        bytes[1] = op[1];
        op = shortToBytes(Short.parseShort(msg.getMsg()[0]));
        bytes[2] = op[0];
        bytes[3] = op[1];
        bytes[4] = ';';
        return bytes;
    }

    private byte[] buildingShort(Message msg) {
        byte[] bytes = new byte[13];
        byte[] temp = shortToBytes(msg.getOpcode());
        bytes[0] = temp[0];
        bytes[1] = temp[1];
        int i = 2;
        for (short num : msg.getShortMsg()) {
            temp = shortToBytes(num);
            bytes[i++] = temp[0];
            bytes[i++] = temp[1];
        }
        bytes[12] = ';';
        return bytes;
    }
}
