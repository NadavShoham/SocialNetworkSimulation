#include "socketReader.h"
#include "iterator"

using namespace std;
SocketReader::SocketReader(ConnectionHandler &handler): _handler(handler){}
extern bool shouldTerminate;
void SocketReader::operator()() {
    int len;
    while (true) {
        string answer;

        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
        if (!_handler.getLine(answer)) {
            cout << "Couldn't receive line... Exiting\n" << endl;
            break;
        }
        len = answer.length();
        answer.resize(len-1);

        string line = decode(answer);
        //len = line.length();

        cout << line << endl;
        if (line == "ACK 3") {
            shouldTerminate = true;
            cout << "Exiting...\n" << endl;
            break;
        }
    }
}

std::string SocketReader::decode(std::string &answer) {
    // convert to char array
    int len = answer.length();
    char bytes[len];
    for (int i = 0; i < len; i++) {
        bytes[i] = answer.c_str()[i];
    }

    // get opcodes (if exist)
    char op1[2];
    char op2[2];
    op1[0] = bytes[0];
    op1[1] = bytes[1];
    op2[0] = bytes[2];
    op2[1] = bytes[3];
    short opcode = bytesToShort(op1);
    short opcode2 = bytesToShort(op2);;

    // different cases fill the line to be printed
    string line;
    switch (opcode) {
        // notification
        case 9:
            line = notification(len, bytes);
            break;
        // ack
        case 10:
            switch (opcode2) {
                // follow
                case 4:
                    line = ackFollow(len, bytes);
                    break;
                // log/stat
                case 7:
                    line = ackShort(len, bytes, opcode2);
                    break;
                case 8:
                    line = ackShort(len, bytes, opcode2);
                    break;
                // all the rest
                default:
                    line = errorOrSimpleAck(opcode, opcode2);
            }
            break;
        // error
        case 11:
            line = errorOrSimpleAck(opcode, opcode2);
            break;
        default:
            break;
    }
    return line;
}

short SocketReader::bytesToShort(const char *bytesArr){
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

string SocketReader::notification(int len, const char *bytes) {
    string line = "NOTIFICATION ";

    // which notification
    if (bytes[2] == 0) line += "PM ";
    else line += "Public ";

    // add username and content
    char acc[len - 3];
    vector<string> seperated;
    for (int i = 0, j = 0; i < len - 3; i++, j++) {
        acc[j] = bytes[i + 3];
        if (acc[j] == '\0'){
            seperated.emplace_back(string(acc));
            j = -1;
        }
    }
    return  line + seperated[0] + " " + seperated[1];
}

string SocketReader::errorOrSimpleAck(short opcode, short opcode2) {
    string response;
    if (opcode == 10) response = "ACK ";
    else response = "ERROR ";
    return response + to_string(opcode2);
}

string SocketReader::ackFollow(int len, const char *bytes) {
    string line = "ACK 4 ";
    char remaining[len - 4];

    // only one string to add so without last char \0
    for (int i = 0; i < len - 5; i++) remaining[i] = bytes[i + 4];
    string remain(remaining);
    return  line + remain;
}

string SocketReader::ackShort(int len, const char *bytes, short opcode2) {
    string line = "ACK " + to_string(opcode2) + " ";

    // all args are shorts
    char nextShort[2];
    for (int i = 4; i < len; i = i + 2) {
        nextShort[0] = bytes[i];
        nextShort[1] = bytes[i + 1];
        line += to_string(bytesToShort(nextShort)) + " ";
    }
    return  line;
}

