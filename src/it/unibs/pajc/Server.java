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
    private boolean carteDistribuite = false; // Novità: serve a bloccare i giocatori finché non scatta il timer
    private final int MAX_PLAYERS = 4;
    
    // Contatori per sincronizzare la fine della mano
    private int giocatoriAttivi = 0; 
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

                if (connectedPlayers == 1) {
                    avviaTimerInizioMano(15);
                }
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
        System.out.println("Primo giocatore entrato! La mano inizierà tra " + secondi + " secondi...");
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
        // 1. Se il gioco è già iniziato, il "ritardatario" aspetta qui la prossima mano
        while (giocoInCorso) {
            out.println("Mano in corso. Attendi la fine del turno per giocare...");
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // 2. Il giocatore si siede ufficialmente al tavolo per questa mano
        giocatoriAttivi++;
        out.println("Sei seduto al tavolo. Attendi la distribuzione delle carte...");

        // 3. Aspetta che il timer finisca e il banco dia le carte
        while (!carteDistribuite) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        out.println("Le carte sono state distribuite! Inizia il tuo turno.");
    }

    public synchronized void iniziaMano() {
        giocoInCorso = true;
        
        // Reset delle variabili per la nuova partita
        sharedDealerHand.clear();
        dealerPlayed = false;
        giocatoriCheHannoFinito = 0; // Azzera chi ha finito
        
        sharedDealerHand.add(deck.drawCard());
        sharedDealerHand.add(deck.drawCard());
        System.out.println("Banco pronto. Carte distribuite a " + giocatoriAttivi + " giocatori.");
        
        carteDistribuite = true; // Sblocca i giocatori fermi al punto 3 di attendiIlTuoTurno()
        notifyAll();
    }

    // ==========================================
    // 3. LOGICA DI FINE TURNO E GIOCO DEL BANCO
    // ==========================================
    
    // NOVITÀ: Il ClientHandler chiama questo metodo quando finisce le sue mosse
    public synchronized void fineTurnoGiocatore() {
        giocatoriCheHannoFinito++;
        System.out.println("Un giocatore ha terminato il turno. (" + giocatoriCheHannoFinito + "/" + giocatoriAttivi + ")");

        // Controlliamo se è stato l'ultimo giocatore a finire
        if (giocatoriCheHannoFinito == giocatoriAttivi) {
            System.out.println("Tutti i giocatori attivi hanno terminato! Tocca al banco.");
            playDealer();
            terminaMano();
        }
    }

    public synchronized void terminaMano() {
        System.out.println("Fine della mano. Risveglio eventuali giocatori in attesa...");
        
        // Reset totale per permettere l'inizio di una nuova mano
        giocoInCorso = false;
        carteDistribuite = false;
        giocatoriAttivi = 0; 
        
        notifyAll(); // Sveglia chi era bloccato in "sala d'attesa"
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
    
 // Blocca i thread dei giocatori che hanno già finito, in attesa che il dealer giochi
    public synchronized void attendiFineMano() {
        while (giocoInCorso) {
            try { 
                wait(); 
            } catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); 
            }
        }
    }
}