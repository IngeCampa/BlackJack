package it.unibs.pajc;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 12345;
    private final int MAX_PLAYERS = 4;

    private Deck deck = new Deck();
    private List<Card> sharedDealerHand;
    private boolean dealerPlayed = false;

    private boolean giocoInCorso = false;
    private boolean carteDistribuite = false;
    private boolean timerAvviato = false;

    private int giocatoriInAttesa = 0;
    private int giocatoriInGioco = 0;
    private int giocatoriCheHannoFinito = 0;
    private int giocatoriInScelta = 0;

    public Server() {
        // ==========================================
        // FIX: LISTA THREAD-SAFE
        // Impedisce il crash se due thread leggono/scrivono il banco contemporaneamente
        // ==========================================
        sharedDealerHand = Collections.synchronizedList(new ArrayList<>());;
    }

    public void start() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PLAYERS);
        int connectedPlayers = 0;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server Blackjack avviato sulla porta " + PORT);

            while (connectedPlayers < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());
                executor.execute(new ClientHandler(clientSocket, deck, sharedDealerHand, this));
                connectedPlayers++;
            }

            System.out.println("Tavolo pieno. Non si accettano più connessioni.");
            executor.shutdown();

            if (executor.awaitTermination(2, TimeUnit.HOURS)) {
                System.out.println("Tutti i giocatori hanno terminato. Server in chiusura.");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }

    public synchronized void attendiIlTuoTurno(PrintWriter out) {
        while (giocoInCorso || giocatoriInScelta > 0) {
            out.println("Tavolo occupato o in attesa di conferme. Attendi il prossimo round...");
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        giocatoriInAttesa++;
        out.println("Sei seduto al tavolo. Attendi la distribuzione delle carte...");

        if (!timerAvviato) {
            timerAvviato = true;
            avviaTimerInizioMano(5);
        }

        while (!carteDistribuite) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void avviaTimerInizioMano(int secondi) {
        System.out.println("Un giocatore si è seduto! La prossima mano inizierà tra " + secondi + " secondi...");
        new Thread(() -> {
            try {
                Thread.sleep(secondi * 1000L);
                System.out.println("Tempo scaduto! Inizio della mano in corso.");
                iniziaMano();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public synchronized void iniziaMano() {
        if (giocatoriInAttesa == 0) {
            timerAvviato = false;
            return;
        }

        giocoInCorso = true;
        giocatoriInGioco = giocatoriInAttesa;
        giocatoriInAttesa = 0;
        giocatoriCheHannoFinito = 0;
        dealerPlayed = false;
        carteDistribuite = false;
        timerAvviato = false;

        // ==========================================
        // FIX: IL CARTONCINO ROSSO
        // Rimescola solo se mancano meno di 40 carte
        // ==========================================
        if (deck.getRemainingCards() < 40) {
            System.out.println("⚠️ Poche carte nel Sabot. Inserimento del cartoncino rosso: si rimescola!");
            deck.reset();
        }

        sharedDealerHand.clear();
        sharedDealerHand.add(deck.drawCard());
        sharedDealerHand.add(deck.drawCard());
        System.out.println("Banco pronto. Carte distribuite a " + giocatoriInGioco + " giocatori.");

        carteDistribuite = true;
        notifyAll();
    }

    public synchronized void fineTurnoGiocatore() {
        giocatoriCheHannoFinito++;
        System.out.println("Un giocatore ha terminato il turno. (" + giocatoriCheHannoFinito + "/" + giocatoriInGioco + ")");
        controllaFineMano();
    }

    private synchronized void controllaFineMano() {
        if (giocoInCorso && giocatoriCheHannoFinito >= giocatoriInGioco) {
            if (giocatoriInGioco > 0) {
                System.out.println("Tutti i giocatori attivi hanno terminato! Tocca al banco.");
                playDealer();
            }
            terminaMano();
        }
    }

    public synchronized void playDealer() {
        if (!dealerPlayed) {
            System.out.println("Il banco sta giocando...");
            while (getHandValue(sharedDealerHand) < 17) {
                sharedDealerHand.add(deck.drawCard());
            }
            dealerPlayed = true;
            System.out.println("Turno del banco finito. Punteggio: " + getHandValue(sharedDealerHand));
        }
    }

    public synchronized void attendiFineMano() {
        while (giocoInCorso) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public synchronized void terminaMano() {
        System.out.println("Fine della mano. Risveglio eventuali giocatori in attesa...");
        giocatoriInScelta = giocatoriInGioco;
        giocoInCorso = false;
        carteDistribuite = false;
        notifyAll();
    }

    public synchronized void sceltaEffettuata() {
        if (giocatoriInScelta > 0) {
            giocatoriInScelta--;
            if (giocatoriInScelta == 0) {
                notifyAll();
            }
        }
    }

    public synchronized void aPlayerLeft() {
        if (giocoInCorso) {
            giocatoriInGioco--;
            System.out.println("Un giocatore si è disconnesso a partita in corso. Giocatori rimasti in gioco: " + giocatoriInGioco);
            controllaFineMano();
        }
    }

    private int getHandValue(List<Card> hand) {
        int total = 0;
        int aceCount = 0;
        for(Card c : hand) {
            total += c.getValue();
            if(c.rank.equals("A")) {
                aceCount++;
            }
        }
        while(total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }
        return total;
    }
}