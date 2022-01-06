#include <stdlib.h>
#include <connectionHandler.h>
#include <keyboardReader.h>
#include "socketReader.h"
#include "thread"

using namespace std;
/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
bool shouldTerminate = false;

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    SocketReader socketReader(connectionHandler);
    KeyboardReader keyboardReader(connectionHandler);

    thread socketThread(ref(socketReader));
    //thread keyboardThread(ref(keyboardReader));
    //keyboardThread.join();
    while (!shouldTerminate) {
        keyboardReader();
        sleep(1);
    }
    socketThread.join();
    return 0;
}


