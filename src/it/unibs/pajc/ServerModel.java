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
    
    // FIX DEADLOCK 1: Usiamo volatile invece di synchronized per evitare che il Server si blocchi per leggere il tempo!
    private volatile int secondiAttesa = 0;

    public Deck getDeck() { return deck; }
    public List<Card> getDealerHand() { return dealerHand; }
    public int getDealerScore() { return getHandValue(dealerHand); }
    public int getSecondiAttesa() { return secondiAttesa; }

    public String ottieniNicknameUnico(String baseNick) {
        String nick = baseNick;
        int counter = 2;
        boolean duplicato;
        do {
            duplicato = false;
            // Accesso sicuro alla lista
            synchronized(giocatoriSeduti) {
                for (ClientHandler ch : giocatoriSeduti) {
                    if (nick.equalsIgnoreCase(ch.getNickname())) {
                        duplicato = true;
                        nick = baseNick + "_" + counter;
                        counter++;
                        break;
                    }
                }
            }
        } while (duplicato);
        return nick;
    }

    public void aggiungiGiocatore(ClientHandler c) { giocatoriSeduti.add(c); aggiornaTavolo(); }
    
    public synchronized void rimuoviGiocatore(ClientHandler c) {
        giocatoriSeduti.remove(c);
        if (giocatoriSeduti.isEmpty()) {
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
            notifyAll(); 
            System.out.println("Stanza vuota. Hard Reset completato.");
        } else {
            aggiornaTavolo();
        }
    }

    public List<ClientHandler> getGiocatoriSeduti() { return giocatoriSeduti; }

    // FIX DEADLOCK 2: Rimosso il 'synchronized' per evitare collisioni col ClientHandler
    public void aggiornaTavolo() {
        List<ClientHandler> copiaSicura;
        // Copia protetta per evitare ConcurrentModificationException se uno si disconnette ora
        synchronized(giocatoriSeduti) {
            copiaSicura = new ArrayList<>(giocatoriSeduti);
        }
        for (ClientHandler ch : copiaSicura) {
            ch.forzaAggiornamentoVisivo();
        }
    }

    public int getHandValue(List<Card> hand) {
        int total = 0, aceCount = 0;
        // Copia sicura per evitare crash se il banco pesca mentre noi leggiamo
        List<Card> copiaMano;
        synchronized(hand) { copiaMano = new ArrayList<>(hand); }
        
        for(Card c : copiaMano) {
            total += c.getValue();
            if(c.rank.equals("A")) aceCount++;
        }
        while(total > 21 && aceCount > 0) { total -= 10; aceCount--; }
        return total;
    }

    public synchronized void attendiIlTuoTurno() throws InterruptedException {
        while (giocoInCorso || giocatoriInScelta > 0) wait();
        giocatoriInAttesa++;
        
        if (!timerAvviato) {
            timerAvviato = true;
            new Thread(() -> {
                for (int i = 5; i > 0; i--) {
                    secondiAttesa = i;
                    aggiornaTavolo();
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
                secondiAttesa = 0;
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

    // FIX DOPPIO SCONTO: Aggiunto il controllo 'sceltaGiaFatta' per evitare matematiche negative
    public synchronized void aPlayerLeft(boolean avevaFinitoIlTurno, boolean sceltaGiaFatta) {
        if (giocoInCorso) {
            giocatoriInGioco--;
            if (avevaFinitoIlTurno) giocatoriCheHannoFinito--;
            controllaFineMano();
        } else if (giocatoriInAttesa > 0) {
            giocatoriInAttesa--;
        }
        
        if (!sceltaGiaFatta && giocatoriInScelta > 0) {
            giocatoriInScelta--;
            if (giocatoriInScelta == 0) notifyAll();
        }
    }
}