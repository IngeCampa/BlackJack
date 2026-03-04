package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Classe Client per il gioco del Blackjack.
 * Utilizza un'architettura multithread per gestire in modo asincrono
 * la ricezione dei messaggi dal server e l'invio dei comandi dell'utente.
 */
public class Client {

    // ==========================================
    // COSTANTI DI CONNESSIONE
    // ==========================================
    private static final String SERVER = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {

        // ==========================================
        // INIZIALIZZAZIONE CONNESSIONE E STREAM
        // ==========================================
        // Il costrutto try-with-resources garantisce la chiusura automatica
        // del socket e degli stream di I/O al termine dell'esecuzione o in caso di errore.
        try (
                Socket socket = new Socket(SERVER, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connesso al server di Blackjack!");

            // ==========================================
            // THREAD 1: MOTORE DI ASCOLTO (Background)
            // ==========================================
            // Questo thread separato si occupa esclusivamente di ascoltare
            // ciò che il server invia, stampandolo immediatamente a schermo.
            // In questo modo, l'interfaccia non si blocca in attesa della tastiera.
            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;

                    // Continua a leggere finché il server non chiude la connessione
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("\nDisconnesso dal server.");
                    System.exit(0); // Chiude l'intero programma se il server cade o si scollega
                }
            });

            // Avvia il motore di ascolto
            listener.start();

            // ==========================================
            // THREAD 2: LETTURA DELLA TASTIERA (Main Thread)
            // ==========================================
            // Il thread principale (quello in cui gira il main) si occupa
            // esclusivamente di leggere ciò che scrivi e inviarlo al server.
            String userMessage;

            // Il ciclo si blocca su readLine() aspettando il tuo 'Invio'
            while ((userMessage = userInput.readLine()) != null) {
                out.println(userMessage);
            }

        } catch (IOException e) {
            System.err.println("Impossibile connettersi al server: " + e.getMessage());
        }
    }
}