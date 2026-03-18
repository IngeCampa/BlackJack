package it.unibs.pajc;
import java.io.*;
import java.net.Socket;

public class Client {
    private final GameUpdateListener listener;
    private ObjectOutputStream out;
    
    private String nickname;

    public Client(GameUpdateListener listener) { this.listener = listener; }

    public boolean connetti(String ip, int port) {
        try {
            // 1. Invece di usare new Socket(ip, port) che può bloccarsi per 60 secondi,
            // creiamo un socket vuoto e impostiamo un TIMEOUT DI 3 SECONDI (3000 millisecondi)
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), 3000); 
            
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Buona prassi di rete per evitare blocchi
            
            // 2. Se arriviamo a questa riga, il Server esiste ed ha accettato in meno di 3 secondi!
            new Thread(() -> {
                try {
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Object msg;
                    while ((msg = in.readObject()) != null) {
                        if (msg instanceof GameState) {
                            listener.onStateUpdate((GameState) msg);
                        } else if (msg instanceof String) {
                            listener.sulMessaggioDiTesto((String) msg);
                        }
                    }
                } catch (Exception e) { 
                    listener.sulMessaggioDiTesto("Disconnesso dal server."); 
                    System.exit(0);
                }
            }).start();
            
            return true; // Connesso con successo!
            
        } catch (Exception e) {
            // 3. Fallisce ISTANTANEAMENTE (o massimo in 3 secondi) se l'IP non risponde!
            return false; 
        }
    }

    public void inviaComando(String cmd) {
        try { out.writeObject(cmd); out.flush(); } catch (Exception e) { }
    }

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}