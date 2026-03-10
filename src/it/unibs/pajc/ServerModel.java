package it.unibs.pajc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerModel {
    private final Deck deck = new Deck();
    private final List<Card> dealerHand = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> giocatoriSeduti = Collections.synchronizedList(new ArrayList<>());

    private boolean giocoInCorso = false;
    private boolean carteDistribuite = false;
    private boolean timerAvviato = false;
    private int giocatoriInAttesa = 0, giocatoriInGioco = 0, giocatoriCheHannoFinito = 0, giocatoriInScelta = 0;

    // NUOVO: Timer visibile
    private int secondiAttesa = 0;

    public Deck getDeck() { return deck; }
    public List<Card> getDealerHand() { return dealerHand; }
    public int getDealerScore() { return getHandValue(dealerHand); }
    public synchronized int getSecondiAttesa() { return secondiAttesa; }

    // NUOVO: Rinomina in automatico se il nome è già preso (es. Marco -> Marco_2)
    public String ottieniNicknameUnico(String baseNick) {
        String nick = baseNick;
        int counter = 2;
        boolean duplicato;
        do {
            duplicato = false;
            for (ClientHandler ch : giocatoriSeduti) {
                if (nick.equalsIgnoreCase(ch.getNickname())) {
                    duplicato = true;
                    nick = baseNick + "_" + counter;
                    counter++;
                    break;
                }
            }
        } while (duplicato);
        return nick;
    }

    public void aggiungiGiocatore(ClientHandler c) { giocatoriSeduti.add(c); aggiornaTavolo(); }
    public synchronized void rimuoviGiocatore(ClientHandler c) {
        giocatoriSeduti.remove(c);

        if (giocatoriSeduti.isEmpty()) {
            // ==========================================
            // HARD RESET: LA STANZA È COMPLETAMENTE VUOTA
            // ==========================================
            giocoInCorso = false;
            carteDistribuite = false;
            timerAvviato = false;
            giocatoriInAttesa = 0;
            giocatoriInGioco = 0;
            giocatoriCheHannoFinito = 0;
            giocatoriInScelta = 0;
            secondiAttesa = 0;
            dealerHand.clear();
            deck.reset();
            notifyAll(); // Sblocca categoricamente eventuali thread rimasti appesi

            System.out.println("Tutti i giocatori sono usciti. Stanza formattata e pronta per nuovi arrivi.");
        } else {
            // Se c'è ancora qualcuno, aggiorniamo semplicemente i loro schermi
            aggiornaTavolo();
        }
    }
    public List<ClientHandler> getGiocatoriSeduti() { return giocatoriSeduti; }

    public synchronized void aggiornaTavolo() {
        List<ClientHandler> copiaSicura = new ArrayList<>(giocatoriSeduti);
        for (ClientHandler ch : copiaSicura) ch.forzaAggiornamentoVisivo();
    }

    public int getHandValue(List<Card> hand) {
        int total = 0, aceCount = 0;
        for(Card c : hand) {
            total += c.getValue();
            if(c.rank.equals("A")) aceCount++;
        }
        while(total > 21 && aceCount > 0) { total -= 10; aceCount--; }
        return total;
    }

    // NUOVO: Timer Dinamico!
    public synchronized void attendiIlTuoTurno() throws InterruptedException {
        while (giocoInCorso || giocatoriInScelta > 0) wait();
        giocatoriInAttesa++;

        if (!timerAvviato) {
            timerAvviato = true;
            // Fa partire un thread che conta da 5 a 0 aggiornando la grafica
            new Thread(() -> {
                for (int i = 5; i > 0; i--) {
                    synchronized(this) { secondiAttesa = i; }
                    aggiornaTavolo();
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
                synchronized(this) { secondiAttesa = 0; }
                iniziaMano();
            }).start();
        }
        while (!carteDistribuite) wait();
    }

    public synchronized void iniziaMano() {
        if (giocatoriInAttesa == 0) { timerAvviato = false; return; }
        giocoInCorso = true;
        giocatoriInGioco = giocatoriInAttesa;
        giocatoriInAttesa = 0; giocatoriCheHannoFinito = 0; timerAvviato = false; carteDistribuite = false;

        if (deck.getRemainingCards() < 40) deck.reset();
        dealerHand.clear();
        dealerHand.add(deck.drawCard());
        dealerHand.add(deck.drawCard());

        carteDistribuite = true;
        notifyAll();
        aggiornaTavolo();
    }

    private synchronized void controllaFineMano() {
        if (giocoInCorso && giocatoriCheHannoFinito >= giocatoriInGioco) {
            if (giocatoriInGioco > 0) {
                while (getHandValue(dealerHand) < 17) {
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                    dealerHand.add(deck.drawCard());
                    aggiornaTavolo();
                }
            }
            giocatoriInScelta = giocatoriInGioco;
            giocoInCorso = false;
            carteDistribuite = false;
            notifyAll();
            aggiornaTavolo();
        }
    }

    public synchronized void fineTurnoGiocatore() { giocatoriCheHannoFinito++; controllaFineMano(); }
    public synchronized void attendiFineMano() throws InterruptedException { while (giocoInCorso) wait(); }
    public synchronized void sceltaEffettuata() {
        if (giocatoriInScelta > 0) {
            giocatoriInScelta--;
            if (giocatoriInScelta == 0) notifyAll();
        }
    }

    public synchronized void aPlayerLeft(boolean avevaFinitoIlTurno) {
        if (giocoInCorso) {
            giocatoriInGioco--;
            if (avevaFinitoIlTurno) giocatoriCheHannoFinito--;
            controllaFineMano();
        } else if (giocatoriInAttesa > 0) {
            giocatoriInAttesa--;
        }
        if (giocatoriInScelta > 0) {
            giocatoriInScelta--;
            if (giocatoriInScelta == 0) notifyAll();
        }
    }
}