package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Classe Client aggiornata (MVC-Style).
 * Gestisce la connessione di rete e traduce i GameState ricevuti dal Server
 * in un'interfaccia visiva (in questo caso, la Console).
 */
public class Client {

    private static final String SERVER = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        try (Socket socket = new Socket(SERVER, PORT)) {
            System.out.println("Connesso al server di Blackjack!");

            // ==========================================
            // 1. SETUP DEGLI STREAM A OGGETTI (La "Cassetta della Posta")
            // ==========================================
            // REGOLA D'ORO JAVA: Creare SEMPRE prima l'ObjectOutputStream dell'ObjectInputStream,
            // altrimenti Client e Server si bloccano a vicenda aspettando gli header.
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Per leggere l'input della tastiera
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // ==========================================
            // 2. IL NETWORK THREAD (L'Observer)
            // ==========================================
            Thread listener = new Thread(() -> {
                try {
                    Object messaggioDalServer;

                    // Continua ad ascoltare finché la connessione è aperta
                    while ((messaggioDalServer = in.readObject()) != null) {

                        // Il Server ci ha mandato la "Cartolina" con i dati aggiornati!
                        if (messaggioDalServer instanceof GameState) {
                            GameState state = (GameState) messaggioDalServer;

                            // Chiamiamo la Vista per aggiornare lo schermo
                            aggiornaSchermo(state);
                        }
                        // Il Server ci ha mandato un semplice messaggio di testo
                        else if (messaggioDalServer instanceof String) {
                            System.out.println("\nSERVER: " + messaggioDalServer);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("\nDisconnesso dal server.");
                    System.exit(0);
                }
            });
            listener.start();

            // ==========================================
            // 3. IL CONTROLLER (Legge la tastiera e invia al Server)
            // ==========================================
            String comandoUtente;
            while ((comandoUtente = userInput.readLine()) != null) {
                // Inviamo il comando come oggetto String al Server
                out.writeObject(comandoUtente);
                out.flush(); // Assicura che il messaggio parta immediatamente
            }

        } catch (IOException e) {
            System.err.println("Impossibile connettersi al server: " + e.getMessage());
        }
    }

    // ==========================================
    // 4. LA VIEW (L'unica parte che tocca lo schermo)
    // ==========================================
    /**
     * Questo metodo è l'unica parte del codice che sa di essere in una Console.
     * Se un domani farai una GUI JavaFX, cambierai solo questo metodo!
     */
    private void aggiornaSchermo(GameState state) {
        System.out.println("\n======================================");

        if (state.getMessaggioAvviso() != null && !state.getMessaggioAvviso().isEmpty()) {
            System.out.println("📢 " + state.getMessaggioAvviso());
        }

        System.out.println("\n🏦 BANCO: " + state.getCarteDealer());
        if (state.getPunteggioDealer() > 0) {
            System.out.println("   Punteggio: " + state.getPunteggioDealer());
        }

        System.out.println("\n👤 LE TUE MANI:");
        List<List<String>> mani = state.getManiGiocatore();

        // Cicla attraverso tutte le mani (Se non hai splittato, sarà solo 1)
        for (int i = 0; i < mani.size(); i++) {
            String freccia = (i == state.getIndiceManoAttuale() && !state.isTurnoFinito() && !state.isFinePartita()) ? "👉 " : "   ";
            System.out.println(freccia + "Mano " + (i+1) + ": " + mani.get(i));
            System.out.println("      Punteggio: " + state.getPunteggiMani().get(i) + " | Scommessa: " + state.getScommesseMani().get(i));
        }

        System.out.println("\n💰 Fiches totali: " + state.getFiches());
        System.out.println("======================================\n");
    }
}