package it.unibs.pajc;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 12345;

    private Deck deck = new Deck();
    private List<Card> sharedDealerHand;
    private boolean dealerPlayed = false;

    // Variabili per il flusso del server
    private boolean giocoInCorso = false;
    private boolean carteDistribuite = false;
    private boolean timerAvviato = false; // NOVITÀ: Evita che partano 10 timer diversi
    private final int MAX_PLAYERS = 4;

    // NOVITÀ: Separiamo chi aspetta da chi gioca, risolve i bug di conteggio!
    private int giocatoriInAttesa = 0;
    private int giocatoriInGioco = 0;
    private int giocatoriCheHannoFinito = 0;

    public Server() {
        sharedDealerHand = new ArrayList<>();
    }

    // ==========================================
    // 1. AVVIO E GESTIONE CONNESSIONI
    // ==========================================
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

    // ==========================================
    // 2. LOGICA DEL TIMER E SALA D'ATTESA
    // ==========================================
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

    public synchronized void attendiIlTuoTurno(PrintWriter out) {
        // 1. Se c'è una partita in corso, il giocatore aspetta che finisca
        while (giocoInCorso) {
            out.println("Mano in corso. Attendi la fine del turno degli altri per giocare...");
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // 2. Si unisce ai giocatori in attesa della nuova mano
        giocatoriInAttesa++;
        out.println("Sei seduto al tavolo. Attendi la distribuzione delle carte...");

        // 3. Se il timer non è ancora partito, lo facciamo partire (una sola volta!)
        if (!timerAvviato) {
            timerAvviato = true;
            avviaTimerInizioMano(5);
        }

        // 4. Blocca il giocatore finché il banco non dà le carte
        while (!carteDistribuite) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public synchronized void iniziaMano() {
        if (giocatoriInAttesa == 0) {
            timerAvviato = false; // Se tutti sono usciti prima del timer, abortiamo
            return;
        }

        giocoInCorso = true;

        // Trasferiamo i giocatori in attesa al tavolo di gioco
        giocatoriInGioco = giocatoriInAttesa;
        giocatoriInAttesa = 0;
        giocatoriCheHannoFinito = 0;

        dealerPlayed = false;
        carteDistribuite = false;
        timerAvviato = false; // Reset per la mano successiva

        // IMPORTANTE: Se non resetti il mazzo, dopo qualche mano il gioco andrà in crash per carte finite!
        // deck.reset(); // SCOMMENTA O AGGIUNGI UN METODO NELLA TUA CLASSE DECK PER RIMESCOLARE

        sharedDealerHand.clear();
        sharedDealerHand.add(deck.drawCard());
        sharedDealerHand.add(deck.drawCard());
        System.out.println("Banco pronto. Carte distribuite a " + giocatoriInGioco + " giocatori.");

        carteDistribuite = true;
        notifyAll(); // Sblocca i giocatori fermi in attendiIlTuoTurno()
    }

    // ==========================================
    // 3. LOGICA DI FINE TURNO E GIOCO DEL BANCO
    // ==========================================

    public synchronized void fineTurnoGiocatore() {
        giocatoriCheHannoFinito++;
        System.out.println("Un giocatore ha terminato il turno. (" + giocatoriCheHannoFinito + "/" + giocatoriInGioco + ")");
        controllaFineMano();
    }

    private synchronized void controllaFineMano() {
        // Se tutti i giocatori in gioco hanno finito, gioca il banco
        if (giocoInCorso && giocatoriCheHannoFinito >= giocatoriInGioco) {
            if (giocatoriInGioco > 0) {
                System.out.println("Tutti i giocatori attivi hanno terminato! Tocca al banco.");
                playDealer();
            }
            terminaMano();
        }
    }

    public synchronized void terminaMano() {
        System.out.println("Fine della mano. Risveglio eventuali giocatori in attesa...");
        giocoInCorso = false;
        carteDistribuite = false;
        notifyAll(); // Sveglia chi aspetta in attendiFineMano() per vedere i risultati
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

    // Blocca i giocatori che hanno finito, finché il dealer non gioca
    public synchronized void attendiFineMano() {
        while (giocoInCorso) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void aPlayerLeft() {
        // Se c'è una partita e il giocatore che se ne va ne fa parte, aggiorniamo il contatore
        if (giocoInCorso) {
            giocatoriInGioco--;
            System.out.println("Un giocatore si è disconnesso a partita in corso. Giocatori rimasti in gioco: " + giocatoriInGioco);
            controllaFineMano(); // Verifica se il suo abbandono permette di chiudere la mano
        } else {
            // Se si disconnette mentre aspetta il timer, si toglie dalla fila
            if (giocatoriInAttesa > 0) {
                giocatoriInAttesa--;
            }
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