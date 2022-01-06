package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.BGSProtocol;
import bgu.spl.net.api.bidi.DataBase;
import bgu.spl.net.api.bidi.Message;
import bgu.spl.net.api.bidi.MessageEncoderDecoderImpl;
import bgu.spl.net.srv.Server;

public class ReactorMain {
    public static void main(String[] args) {
        if (args[0] != null) {
            String[] filteredWords = {"war", "trump", "voldemort", "anti-vax", "zuckerberg", "kill", "hitler"};
            DataBase dataBase = DataBase.getInstance();
            dataBase.setFilteredWords(filteredWords);
            try (Server<Message> server = Server.reactor(Integer.parseInt(args[1]),Integer.parseInt(args[0]),
                    ()-> new BGSProtocol(dataBase) {
                    }, MessageEncoderDecoderImpl::new)) {
                server.serve();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            System.out.println("No arguments available");
    }

}
