package server;

public class Message {

    private String pseudo;
    private String contenu;

    public Message(String pseudo, String contenu) {
        this.pseudo = pseudo;
        this.contenu = contenu;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getContenu() {
        return contenu;
    }
}