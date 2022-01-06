#include "keyboardReader.h"
#include <boost/algorithm/string.hpp>

using namespace std;


KeyboardReader::KeyboardReader(ConnectionHandler &handler) : _handler(handler), shouldTerminate(false), result(""){}

void KeyboardReader::operator()() {
    while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        cin.getline(buf, bufsize); // blocking
        string line(buf);

        // ours
        encode(line);
        // ours

        if (!_handler.sendLine(result)) {
            cout << "Couldn't send line... Exiting\n" << endl;
            break;
        }
        result = "";
        // connectionHandler.sendLine(line) appends '\n' to the message. Therefor we send len+1 bytes.

        // ours
        if (shouldTerminate) break;
    }
}

void KeyboardReader::encode(string request) {
    vector<string> separated;
    boost::split(separated, request, boost::is_any_of(" "));
    string command = separated[0];

    if (command == "REGISTER"){
        addShortAsBytes(1); //adding the opcode
        for (unsigned int i = 1; i < separated.size(); i++){
            result += separated[i];
            result += '\0';
        }
    } else if(command == "LOGIN"){
        addShortAsBytes(2); //adding the opcode
        for (int i = 1; i < 3; i++){
            result += separated[i];
            result += '\0';
        }
        //adding the captcha
        if (separated[3] == "1")
            addOneByte(1);
        else
            addOneByte(0);

    } else if(command == "LOGOUT"){
        addShortAsBytes(3); //adding the opcode
        shouldTerminate = true;

    } else if(command == "FOLLOW"){
        addShortAsBytes(4); //adding the opcode

        //adding the follow/unfollow sign
        if (separated[1] == "0")
            addOneByte(0);
        else
            addOneByte(1);

        result += separated[2];
        result += '\0';

    } else if(command == "POST"){
        addShortAsBytes(5); //adding the opcode
        for (unsigned int i = 1; i < separated.size(); i++) {
            result += separated[i];
            result += " ";
        }
        result += '\0';

    } else if(command == "PM"){
        addShortAsBytes(6); //adding the opcode
        result += separated[1] + '\0';
        for (unsigned int i = 2; i < separated.size(); i++) {
            result += separated[i];
            result += " ";
        }
        result += '\0';
        result += currTime();
        result += '\0';

    } else if(command == "LOGSTAT"){
        addShortAsBytes(7);//adding the opcode

    } else if(command == "STAT"){
        addShortAsBytes(8); //adding the opcode
        result += separated[1];
        result += '\0';

    } else if(command == "BLOCK"){
        addShortAsBytes(12); //adding the opcode
        result += separated[1];
        result += '\0';
    }
}
void KeyboardReader::addShortAsBytes(short num){
    char shortToAdd[2];
    shortToBytes(num, shortToAdd);
    for (char c: shortToAdd)
        result += c;
}

void KeyboardReader::addOneByte(short num) {
    result += (num & 0xFF);
}
void KeyboardReader::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

string KeyboardReader::currTime(){
    time_t currT = time(nullptr);
    tm* now = localtime(&currT);
    string year = to_string(now->tm_year + 1900);
    string month = to_string(now->tm_mon + 1);
    string day = to_string(now->tm_mday);
    string hour = to_string(now->tm_hour);
    string minute = to_string(now->tm_min);
    return padding(day) + "-" + padding(month) + "-" + year + " " + padding(hour) + ":" + padding(minute);
    //TODO delete this after checking this func
    /*
     * vector<string> separatedTime;
    boost::split(separatedTime, now, boost::is_any_of(" "));
    string month = separatedTime[1];
    string day = separatedTime[2];
    string hour = separatedTime[3];
    string year = separatedTime[4];
    */

}
string KeyboardReader:: padding(string s) {
    if (s.length() < 2)
        s = "0" + s;
    return s;
}

