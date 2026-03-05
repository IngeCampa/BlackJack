package it.unibs.pajc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Il Server MVC (Gestore della Rete).
 * Il suo unico scopo è aprire la porta TCP, creare il "Tavolo" (Il Modello)
 * e far sedere i giocatori assegnando a ciascuno un ClientHandler (Il Controller).
 */
public class Server {

    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4;

    public void start() {
        // ==========================================
        // 1. CREAZIONE DEL MODELLO (La "Truth")
        // ==========================================
        // C'è un solo tavolo condiviso per tutti i giocatori.
        // Tutta la logica (regole, mazzi, turni) ora vive qui dentro.
        BlackjackRoom gameRoom = new BlackjackRoom();

        // Pool di thread per gestire i giocatori in parallelo
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PLAYERS);
        int connectedPlayers = 0;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("♠️♥️ Server Blackjack MVC avviato sulla porta " + PORT + " ♦️♣️");
            System.out.println("In attesa di giocatori...");

            // ==========================================
            // 2. CICLO DI ACCETTAZIONE (La "Porta Girevole")
            // ==========================================
            while (connectedPlayers < MAX_PLAYERS) {
                // Il server si blocca qui finché qualcuno non si connette
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo giocatore connesso da: " + clientSocket.getInetAddress());

                // ==========================================
                // 3. CREAZIONE DEL CONTROLLER
                // ==========================================
                // Passiamo al ClientHandler il socket (per parlare col giocatore)
                // e il gameRoom (per interagire con le regole del gioco).
                ClientHandler handler = new ClientHandler(clientSocket, gameRoom);

                // Avvia il thread del giocatore in background
                executor.execute(handler);

                connectedPlayers++;
            }

            System.out.println("Tavolo pieno (" + MAX_PLAYERS + "/" + MAX_PLAYERS + "). Non si accettano più connessioni.");

            // Smette di accettare nuovi thread, ma lascia finire la partita in corso
            executor.shutdown();

            // Attesa di sicurezza (es. il server si spegne se tutti se ne vanno)
            if (executor.awaitTermination(2, TimeUnit.HOURS)) {
                System.out.println("Tutti i giocatori hanno terminato. Server in chiusura.");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Errore critico nel server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}