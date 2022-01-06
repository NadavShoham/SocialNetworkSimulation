package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.*;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        if (args[0] != null) {
            String[] filteredWords = {"war", "trump", "voldemort", "anti-vax", "zuckerberg", "kill", "hitler"};
            DataBase dataBase = DataBase.getInstance();
            dataBase.setFilteredWords(filteredWords);
            try (Server<Message> server = Server.threadPerClient(Integer.parseInt(args[0]),
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
