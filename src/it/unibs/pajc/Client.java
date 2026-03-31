package it.unibs.pajc;
import java.io.*;
import java.net.Socket;

public class Client {
    private final GameUpdateListener listener;
    private ObjectOutputStream out;
    
    private String nickname;

    public Client(GameUpdateListener listener) 
    { 
    	this.listener = listener; 
    }

    /**
	 * 1. Tenta di connettersi al server all'IP e porta specificati.
	 * 2. Se la connessione è stabilita, avvia un thread per ascoltare i messaggi dal server.
	 * 3. Se l'IP non risponde entro 3 secondi, restituisce false.
	 */
    public boolean connetti(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), 3000); 
            
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Assicura che l'header dell'ObjectOutputStream venga inviato subito
            
            
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
                	listener.sulMessaggioDiTesto("DISCONNESSIONE");
                }
            }).start();
            return true;
            
        } catch (Exception e) {
            return false; 
        }
    }

    /** Invia un comando al server. 
        Il comando è una stringa che rappresenta l'azione del giocatore (es. "hit", "stand", "raddoppia", o una scommessa numerica).
	 **/
    public void inviaComando(String cmd) {
        try { 
        	out.writeObject(cmd); 
        	out.flush(); 
        	} 
        catch (Exception e) { }
    }

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}