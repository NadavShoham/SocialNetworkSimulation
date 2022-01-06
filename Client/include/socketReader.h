//
// Created by shoha on 03/01/2022.
//

#ifndef SPL_HW3_SOCKETREADER_H
#define SPL_HW3_SOCKETREADER_H
#include "connectionHandler.h"
#endif //SPL_HW3_SOCKETREADER_H

using namespace std;

class SocketReader {
private:
    ConnectionHandler &_handler;

    string decode(string &answer);

    short bytesToShort(const char* bytesArr);

    string notification(int len, const char *bytes);

    string errorOrSimpleAck(short opcode, short opcode2);

    string ackFollow(int len, const char *bytes);

    string ackShort(int len, const char *bytes, short opcode2);

public:
    SocketReader(ConnectionHandler &handler);

    void operator()();
};