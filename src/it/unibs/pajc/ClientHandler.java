package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class ClientHandler implements Runnable {
    private Socket socket;
    private Deck deck;
    private BufferedReader in;
    private PrintWriter out;
    private List<Card> playerHand;
    private List<Card> dealerHand;
    private Server server;

    public ClientHandler(Socket socket, Deck deck, List<Card> sharedDealerHand, Server server) throws IOException {
        this.socket = socket;
        this.deck = deck;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.playerHand = new ArrayList<>();
        this.dealerHand = sharedDealerHand;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            boolean keepGoing = true;

            // ==========================================
            // 1. CICLO PRINCIPALE DELLE PARTITE
            // ==========================================
            while (keepGoing) {
                // FONDAMENTALE: Pulisci la mano all'inizio di ogni nuova partita!
                playerHand.clear();

                server.attendiIlTuoTurno(out);

                out.println("Benvenuto al Blackjack!");

                // Distribuisci 2 carte al giocatore
                playerHand.add(deck.drawCard());
                playerHand.add(deck.drawCard());

                out.println("Le tue carte: " + playerHand + " | Totale: " + getHandValue(playerHand));
                out.println("Carta visibile del dealer: " + dealerHand.get(0));

                // ==========================================
                // 2. CICLO DI GIOCO DEL CLIENT
                // ==========================================
                boolean accesso = true;
                while (accesso) {
                    out.println("Digita 'carta' per pescare o 'sto' per fermarti:");
                    String comando = in.readLine();

                    // Se il client si disconnette di botto
                    if (comando == null) {
                        accesso = false;
                        keepGoing = false;
                        break;
                    }

                    if (comando.equalsIgnoreCase("carta")) {
                        Card carta = deck.drawCard();
                        playerHand.add(carta);
                        out.println("Hai pescato: " + carta);
                        int total = getHandValue(playerHand);
                        out.println("Totale mano: " + total);

                        if (total > 21) {
                            out.println("Hai sballato!");
                            accesso = false;
                        }
                    } else if(comando.equalsIgnoreCase("sto")) {
                        accesso = false;
                    } else {
                        out.println("Comando non valido!");
                    }
                }

                // Se il client è crashato durante il suo turno, usciamo dal loop principale
                if (!keepGoing) break;

                // ==========================================
                // 3. FINE DELLE AZIONI, ATTESA DEL BANCO
                // ==========================================
                out.println("Hai terminato il turno. In attesa degli altri giocatori e del banco...");
                server.fineTurnoGiocatore();
                server.attendiFineMano();

                // ==========================================
                // 4. CALCOLO E STAMPA DEI RISULTATI FINALI
                // ==========================================
                int playerTotal = getHandValue(playerHand);
                int dealerTotal = getHandValue(dealerHand);
                out.println("Mano finale del dealer: " + dealerHand + " | Totale: " + dealerTotal);

                if (playerTotal > 21) {
                    out.println("Hai perso (hai sballato).");
                } else if (dealerTotal > 21 || playerTotal > dealerTotal) {
                    out.println("Complimenti! Hai vinto!");
                } else if (playerTotal < dealerTotal) {
                    out.println("Hai perso! Dealer vince.");
                } else {
                    out.println("Pareggio!");
                }

                // ==========================================
                // 5. SCELTA: RESTARE O USCIRE
                // ==========================================
                out.println("Vuoi restare al tavolo per un'altra partita? (si/no)");
                String choice = in.readLine();

                // Se l'utente chiude il terminale (null) o digita qualcosa diverso da "si"
                if (choice == null || !choice.equalsIgnoreCase("si")) {
                    keepGoing = false; // Ferma il loop
                    out.println("Grazie per aver giocato! Arrivederci.");
                } else {
                    out.println("Ottima scelta! Preparo il prossimo round...\n---");
                }
            }

            // Una volta fuori dal loop, il giocatore se n'è andato.
            // Avvisiamo il server e chiudiamo la socket.
            server.aPlayerLeft();
            socket.close();

        } catch (IOException e) {
            System.err.println("Errore di comunicazione: " + e.getMessage());
        }
    }

    private int getHandValue(List<Card> mano) {
        int tot = 0;
        int assi = 0;
        for (Card c : mano) {
            tot += c.getValue();
            if (c.rank.equals("A")) assi++;
        }
        while (tot > 21 && assi > 0) {
            tot -= 10;
            assi--;
        }
        return tot;
    }
}