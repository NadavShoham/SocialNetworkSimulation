#ifndef SPL_HW3_KEYBOARDREADER_H
#define SPL_HW3_KEYBOARDREADER_H
# include "connectionHandler.h"

#endif //SPL_HW3_KEYBOARDREADER_H

class KeyboardReader {
private:
    ConnectionHandler &_handler;
    bool shouldTerminate;
    std::string result;

    void encode(std::string request);

    void shortToBytes(short num, char* bytesArr);

    void addShortAsBytes(short num);

    void addOneByte(short num);

    std::string currTime();

    std::string padding(std::string);

public:
    KeyboardReader(ConnectionHandler &handler);

    void operator()();

};
