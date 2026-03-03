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

//    public  String scelta() {
//        return this.in.readLine();
//    }

    @Override
    public void run() {
        try {
            // ==========================================
            // 1. SALA D'ATTESA E INIZIO
            // ==========================================
            // Il giocatore aspetta che ci siano le condizioni per iniziare (es. fine timer)
            boolean keepGoing = true;
            while (keepGoing) {
                server.attendiIlTuoTurno(out);
//                out.println("Vuoi uscire?");
//                String line = "si";
//                out.println(line);
//                if (line.equalsIgnoreCase("si")) {
//                    server.aPlayerLeft();
//                    socket.close();
//                    return;
//                }

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

                    if (comando == null) break; // Evita crash se il client si disconnette di botto

                    if (comando.equalsIgnoreCase("carta")) {
                        Card carta = deck.drawCard();
                        playerHand.add(carta);
                        out.println("Hai pescato: " + carta);
                        int total = getHandValue(playerHand);
                        out.println("Totale mano: " + total);

                        if (total > 21) {
                            out.println("Hai sballato!");
                            accesso = false; // Esce dal ciclo, ma NON fa return!
                        }
                    } else if(comando.equalsIgnoreCase("sto")) {
                        accesso = false;
                    } else {
                        out.println("Comando non valido!");
                    }
                }

                // ==========================================
                // 3. FINE DELLE AZIONI, ATTESA DEL BANCO
                // ==========================================
                out.println("Hai terminato il turno. In attesa degli altri giocatori e del banco...");

                // Avvisa il server che questo giocatore ha finito
                server.fineTurnoGiocatore();

                // Aspetta che il server giochi le carte del dealer e dichiari la fine della mano
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
                out.println("Vuoi uscire? si o no");
//                String choice = in.readLine();
                out.println("aaaaaa");
//                if (choice.equalsIgnoreCase("si")) {
//                    keepGoing = false;
//                }
//                out.println(keepGoing + " " + choice);
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
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
            tot -= 10; // Asso vale 1 invece di 11
            assi--;
        }
        return tot;
    }
}