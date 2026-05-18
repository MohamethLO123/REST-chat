package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ChatService implements HttpHandler {

    private static List<Message> messages = new ArrayList<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        // POST -> envoyer message
        if (method.equalsIgnoreCase("POST")) {

            InputStreamReader reader =
                new InputStreamReader(exchange.getRequestBody());

            Message msg = gson.fromJson(reader, Message.class);

            messages.add(msg);

            byte[] response = "Message reçu".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }

        // GET -> récupérer messages
        else if (method.equalsIgnoreCase("GET")) {

            byte[] json = gson.toJson(messages).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length);
            OutputStream os = exchange.getResponseBody();
            os.write(json);
            os.close();
        }
    }
}