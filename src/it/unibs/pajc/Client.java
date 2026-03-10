package it.unibs.pajc;
import java.io.*;
import java.net.Socket;

public class Client {
    private final GameUpdateListener listener;
    private ObjectOutputStream out;

    public Client(GameUpdateListener listener) { this.listener = listener; }

    public void connetti(String ip, int port) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port)) {
                out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Object msg;
                while ((msg = in.readObject()) != null) {
                    if (msg instanceof GameState) listener.onStateUpdate((GameState) msg);
                    else if (msg instanceof String) listener.sulMessaggioDiTesto((String) msg);
                }
            } catch (Exception e) { listener.sulMessaggioDiTesto("Disconnesso."); System.exit(0); }
        }).start();
    }

    public void inviaComando(String cmd) {
        try { out.writeObject(cmd); out.flush(); } catch (Exception e) { }
    }
}