package it.unibs.pajc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PURE MODEL: Il "Tavolo" da Blackjack.
 * Gestisce esclusivamente le regole del gioco, le carte e la sincronizzazione
 * dei turni (State Machine). Non sa nulla di Socket o di interfacce grafiche.
 */
public class BlackjackRoom {

    // ==========================================
    // 1. GLI OGGETTI DEL GIOCO
    // ==========================================
    private Deck deck;
    private List<Card> dealerHand;

    // ==========================================
    // 2. LE VARIABILI DI STATO (La tua ottima logica originale!)
    // ==========================================
    private boolean giocoInCorso = false;
    private boolean carteDistribuite = false;
    private boolean timerAvviato = false;

    private int giocatoriInAttesa = 0;
    private int giocatoriInGioco = 0;
    private int giocatoriCheHannoFinito = 0;
    private int giocatoriInScelta = 0;

    public BlackjackRoom() {
        this.deck = new Deck();
        // Thread-safe list per il banco
        this.dealerHand = Collections.synchronizedList(new ArrayList<>());
    }

    // ==========================================
    // 3. REGOLE E PUNTEGGI
    // ==========================================
    public Deck getDeck() { return deck; }
    public List<Card> getDealerHand() { return dealerHand; }
    public int getDealerScore() { return getHandValue(dealerHand); }

    /**
     * Il calcolo del punteggio (con la logica dell'Asso 1 o 11)
     * è ora centralizzato qui. Nessun altro deve preoccuparsene!
     */
    public int getHandValue(List<Card> hand) {
        int total = 0;
        int aceCount = 0;
        for(Card c : hand) {
            total += c.getValue();
            if(c.rank.equals("A")) aceCount++;
        }
        while(total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }
        return total;
    }

    // ==========================================
    // 4. LA MACCHINA A STATI (Sincronizzazione Thread)
    // ==========================================

    public synchronized void attendiIlTuoTurno() throws InterruptedException {
        // Se c'è una partita in corso, il thread (ClientHandler) si mette in pausa
        while (giocoInCorso || giocatoriInScelta > 0) {
            wait();
        }

        giocatoriInAttesa++;

        // Il primo che si siede fa partire il timer
        if (!timerAvviato) {
            timerAvviato = true;
            avviaTimerInizioMano(5);
        }

        // Mette in pausa finché il dealer non dà le carte
        while (!carteDistribuite) {
            wait();
        }
    }

    private void avviaTimerInizioMano(int secondi) {
        new Thread(() -> {
            try {
                Thread.sleep(secondi * 1000L);
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
        carteDistribuite = false;
        timerAvviato = false;

        // Il famoso "Cartoncino Rosso"
        if (deck.getRemainingCards() < 40) {
            deck.reset();
        }

        dealerHand.clear();
        dealerHand.add(deck.drawCard());
        dealerHand.add(deck.drawCard());

        carteDistribuite = true;

        // Risveglia tutti i giocatori: "Le carte sono pronte!"
        notifyAll();
    }

    public synchronized void fineTurnoGiocatore() {
        giocatoriCheHannoFinito++;
        controllaFineMano();
    }

    private synchronized void controllaFineMano() {
        // Se tutti hanno finito, tocca al banco
        if (giocoInCorso && giocatoriCheHannoFinito >= giocatoriInGioco) {
            if (giocatoriInGioco > 0) {
                playDealer();
            }
            terminaMano();
        }
    }

    public synchronized void playDealer() {
        // Il banco pesca finché non ha almeno 17
        while (getHandValue(dealerHand) < 17) {
            dealerHand.add(deck.drawCard());
        }
    }

    public synchronized void attendiFineMano() throws InterruptedException {
        while (giocoInCorso) {
            wait();
        }
    }

    public synchronized void terminaMano() {
        giocatoriInScelta = giocatoriInGioco;
        giocoInCorso = false;
        carteDistribuite = false;
        notifyAll(); // Risveglia chi aspetta i risultati
    }

    public synchronized void sceltaEffettuata() {
        if (giocatoriInScelta > 0) {
            giocatoriInScelta--;
            if (giocatoriInScelta == 0) {
                notifyAll(); // Tutti hanno scelto se restare/uscire, si ricomincia!
            }
        }
    }

    public synchronized void aPlayerLeft() {
        if (giocoInCorso) {
            giocatoriInGioco--;
            controllaFineMano();
        } else if (giocatoriInAttesa > 0) {
            giocatoriInAttesa--;
        }
    }
}