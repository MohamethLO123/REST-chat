package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class ChatClient {

    private static int lastCount = 0;
    private static final Type MESSAGE_LIST_TYPE = new TypeToken<List<Message>>(){}.getType();

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.print("Pseudo : ");
        final String pseudo = sc.nextLine();

        startPoller(pseudo);

        Gson gson = new Gson();

        while (true) {
            System.out.print("> ");
            String texte = sc.nextLine();

            byte[] body = gson.toJson(new Message(pseudo, texte)).getBytes("UTF-8");

            URL url = new URL("http://localhost:8080/messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));

            OutputStream os = conn.getOutputStream();
            os.write(body);
            os.close();
            conn.getInputStream().close();
            conn.disconnect();

            lastCount++;
            System.out.println("Message envoyé !");
        }
    }

    private static void startPoller(String pseudo) {
        Thread poller = new Thread(new Runnable() {
            public void run() {
                Gson gson = new Gson();
                while (true) {
                    try {
                        URL url = new URL("http://localhost:8080/messages");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Accept", "application/json");

                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8")
                        );
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        conn.disconnect();

                        List<Message> messages = gson.fromJson(sb.toString(), MESSAGE_LIST_TYPE);
                        if (messages != null && messages.size() > lastCount) {
                            for (int i = lastCount; i < messages.size(); i++) {
                                Message m = messages.get(i);
                                if (!m.getPseudo().equals(pseudo)) {
                                    System.out.println("\n[" + m.getPseudo() + "] " + m.getContenu());
                                    System.out.print("> ");
                                }
                            }
                            lastCount = messages.size();
                        }
                    } catch (Exception ignored) {}

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        poller.setDaemon(true);
        poller.start();
    }
}