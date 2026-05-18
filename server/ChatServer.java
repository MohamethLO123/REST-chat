package server;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class ChatServer {

    public static void main(String[] args) {
        try {

            HttpServer server =
                HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/messages", new ChatService());

            server.setExecutor(null);

            server.start();

            System.out.println(
                "✅ Serveur REST démarré sur http://localhost:8080/messages"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}