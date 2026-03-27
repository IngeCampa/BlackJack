package it.unibs.pajc;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerModel {
    public enum StatoServer { LOBBY, SCOMMESSE, GIOCO, RISULTATI }
    private volatile StatoServer statoServer = StatoServer.LOBBY;
    
    private final Deck deck = new Deck();
    private final List<Card> dealerHand = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> giocatoriSeduti = Collections.synchronizedList(new ArrayList<>());
    
    private final List<ClientHandler> spettatori = Collections.synchronizedList(new ArrayList<>());
    public void aggiungiSpettatore(ClientHandler c) { spettatori.add(c); }
    public void rimuoviSpettatore(ClientHandler c) { spettatori.remove(c); }
    

    private boolean carteDistribuite = false;
    private boolean timerAvviato = false;
    private int giocatoriInAttesa = 0, giocatoriInGioco = 0, giocatoriCheHannoFinito = 0, giocatoriProntiPerCarte = 0;
    private volatile int secondiAttesa = 0;

    public Deck getDeck() { return deck; }
    public List<Card> getDealerHand() { return dealerHand; }
    public int getDealerScore() { return getHandValue(dealerHand); }
    public int getSecondiAttesa() { return secondiAttesa; }
    public List<ClientHandler> getGiocatoriSeduti() { return giocatoriSeduti; }

    public int getHandValue(List<Card> hand) {
        int total = 0, aceCount = 0;
        List<Card> copiaMano;
        synchronized(hand) { copiaMano = new ArrayList<>(hand); }
        for(Card c : copiaMano) {
            total += c.getValue();
            if(c.rank.equals("A")) aceCount++;
        }
        while(total > 21 && aceCount > 0) { total -= 10; aceCount--; }
        return total;
    }

    public String ottieniNicknameUnico(String baseNick) {
        String nick = baseNick;
        int counter = 2;
        boolean duplicato;
        do {
            duplicato = false;
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

    public void aggiungiGiocatore(ClientHandler c) { 
        giocatoriSeduti.add(c); 
        aggiornaTavolo(); 
    }
    
    public synchronized void rimuoviGiocatore(ClientHandler c) {
        giocatoriSeduti.remove(c);
        if (giocatoriSeduti.isEmpty()) {
            statoServer = StatoServer.LOBBY;
            carteDistribuite = false;
            timerAvviato = false;
            giocatoriInAttesa = 0; giocatoriInGioco = 0; 
            giocatoriCheHannoFinito = 0; giocatoriProntiPerCarte = 0;
            secondiAttesa = 0;
            dealerHand.clear(); deck.reset();
            notifyAll(); 
            System.out.println("Stanza vuota. Hard Reset completato.");
        } else {
            aggiornaTavolo();
        }
    }

    public void aggiornaTavolo() {
        List<ClientHandler> copiaSicura;
        synchronized(giocatoriSeduti) { copiaSicura = new ArrayList<>(giocatoriSeduti); }
        for (ClientHandler ch : copiaSicura) ch.forzaAggiornamentoVisivo();
        
        // AGGIUNGI QUESTO BLOCCO: Aggiorna lo schermo anche a chi è in tribuna!
        List<ClientHandler> copiaSpettatori;
        synchronized(spettatori) { copiaSpettatori = new ArrayList<>(spettatori); }
        for (ClientHandler ch : copiaSpettatori) ch.forzaAggiornamentoVisivo();
    }

    // ==========================================
    // IL BUTTAFUORI DELLA SALA D'ATTESA
    // ==========================================
    public boolean isPartitaInCorso() {
        return statoServer != StatoServer.LOBBY;
    }

    public synchronized void attendiProssimoRound() throws InterruptedException {
        while (statoServer != StatoServer.LOBBY) wait();
    }

    // ==========================================
    // FASE 1: LOBBY E AVVIO SCOMMESSE
    // ==========================================
    public synchronized void attendiIlTuoTurno() throws InterruptedException {
        while (statoServer != StatoServer.LOBBY) wait();
        
        giocatoriInAttesa++;
        if (!timerAvviato) {
            timerAvviato = true;
            new Thread(() -> {
                for (int i = 5; i > 0; i--) {
                    secondiAttesa = i; aggiornaTavolo();
                    try { Thread.sleep(1000); } catch (Exception e) {}
                }
                secondiAttesa = 0;
                aggiornaTavolo(); 
                apriScommesse(); 
            }).start();
        }
        while (statoServer == StatoServer.LOBBY) wait(); 
    }

    public synchronized void apriScommesse() {
        if (giocatoriInAttesa == 0) { timerAvviato = false; return; }
        statoServer = StatoServer.SCOMMESSE;
        giocatoriInGioco = giocatoriInAttesa;
        giocatoriInAttesa = 0; giocatoriCheHannoFinito = 0; giocatoriProntiPerCarte = 0;
        timerAvviato = false; carteDistribuite = false;
        notifyAll(); 
    }

    // ==========================================
    // FASE 2: GIOCO E CARTE
    // ==========================================
    public synchronized void confermaScommessaEAttendiCarte() throws InterruptedException {
        giocatoriProntiPerCarte++;
        if (giocatoriProntiPerCarte >= giocatoriInGioco) {
            giocatoriProntiPerCarte = 0;
            statoServer = StatoServer.GIOCO; 
            
            if (deck.getRemainingCards() < 40) deck.reset();
            dealerHand.clear();
            dealerHand.add(deck.drawCard()); dealerHand.add(deck.drawCard());
            
            carteDistribuite = true; notifyAll(); aggiornaTavolo(); 
        } else {
            while (!carteDistribuite) wait();
        }
    }

    public synchronized void attendiFineMano() throws InterruptedException { 
        while (statoServer == StatoServer.GIOCO) wait(); 
    }

    // ==========================================
    // FASE 3 E 4: FINE TURNO E RISULTATI
    // ==========================================
    public synchronized void fineTurnoGiocatore() { 
        giocatoriCheHannoFinito++; 
        if (statoServer == StatoServer.GIOCO && giocatoriCheHannoFinito >= giocatoriInGioco) {
            if (giocatoriInGioco > 0) {
                while (getHandValue(dealerHand) < 17) {
                    try { Thread.sleep(1000); } catch (Exception e) {} 
                    dealerHand.add(deck.drawCard()); aggiornaTavolo(); 
                }
            }
            
            statoServer = StatoServer.RISULTATI;
            carteDistribuite = false;
            notifyAll(); aggiornaTavolo();
            
            new Thread(() -> {
                for (int i = 10; i > 0; i--) {
                    secondiAttesa = i; aggiornaTavolo();
                    try { Thread.sleep(1000); } catch (Exception e) {}
                }
                secondiAttesa = 0;
                aggiornaTavolo(); 
                chiudiRisultatiETornaInLobby();
            }).start();
        }
    }

    public synchronized void attendiFineRisultati() throws InterruptedException {
        while (statoServer == StatoServer.RISULTATI) wait();
    }

    private synchronized void chiudiRisultatiETornaInLobby() {
        statoServer = StatoServer.LOBBY;
        dealerHand.clear();
        notifyAll(); 
        aggiornaTavolo();
    }

    // ==========================================
    // FIX TAVOLO FANTASMA E DISCONNESSIONI (Blindato!)
    // ==========================================
    public synchronized void aPlayerLeft(boolean eraInGioco, boolean avevaFinitoIlTurno) {
        // Se a uscire è stato qualcuno che stava effettivamente giocando la mano
        if (eraInGioco) {
            if (statoServer == StatoServer.SCOMMESSE || statoServer == StatoServer.GIOCO) {
                giocatoriInGioco--;
                if (avevaFinitoIlTurno) giocatoriCheHannoFinito--;

                if (giocatoriInGioco <= 0) {
                    statoServer = StatoServer.LOBBY;
                    carteDistribuite = false;
                    timerAvviato = false;
                    secondiAttesa = 0;
                    dealerHand.clear();
                    giocatoriProntiPerCarte = 0;
                    giocatoriCheHannoFinito = 0;
                    notifyAll(); 
                    aggiornaTavolo();
                } else {
                    if (statoServer == StatoServer.SCOMMESSE && !carteDistribuite) {
                        if (giocatoriProntiPerCarte >= giocatoriInGioco) {
                            statoServer = StatoServer.GIOCO;
                            if (deck.getRemainingCards() < 40) deck.reset();
                            dealerHand.clear();
                            dealerHand.add(deck.drawCard()); dealerHand.add(deck.drawCard());
                            carteDistribuite = true; notifyAll();
                        }
                    }
                    if (giocatoriCheHannoFinito >= giocatoriInGioco) fineTurnoGiocatore();
                }
            }
        } else {
            // Se a uscire è stato uno spettatore, non distruggiamo la partita di chi gioca!
            // Lo togliamo solo dalla coda di attesa, se ci era entrato.
            if (statoServer == StatoServer.LOBBY && giocatoriInAttesa > 0) {
                giocatoriInAttesa--;
            }
        }
    }
    
    // ==========================================
    // GESTIONE CHAT MULTIPLAYER
    // ==========================================
    public void broadcastChat(String mittente, String messaggio) {
        String formatoChat = "CHATMSG:💬 [" + mittente + "]: " + messaggio;
        List<ClientHandler> copiaSicura;
        synchronized(giocatoriSeduti) { copiaSicura = new ArrayList<>(giocatoriSeduti); }
        for (ClientHandler ch : copiaSicura) {
            ch.inviaTestoDiretto(formatoChat); 
        }
    }
    
 // ==========================================
    // GESTIONE CLASSIFICA (LEADERBOARD)
    // ==========================================
    public synchronized void registraVittoria(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) return;
        File file = new File("classifica.txt");
        Map<String, Integer> vittorie = new HashMap<>();
        
        // 1. Legge il file esistente
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] parti = linea.split(":");
                    if (parti.length == 2) vittorie.put(parti[0], Integer.parseInt(parti[1]));
                }
            } catch (Exception e) {}
        }
        
        // 2. Aggiunge +1 vittoria al giocatore
        vittorie.put(nickname, vittorie.getOrDefault(nickname, 0) + 1);
        
        // 3. Riscrive il file aggiornato
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Integer> entry : vittorie.entrySet()) {
                bw.write(entry.getKey() + ":" + entry.getValue());
                bw.newLine();
            }
        } catch (Exception e) {}
    }

    // gestione classifica
    public synchronized String ottieniClassifica() {
        File file = new File("classifica.txt");
        if (!file.exists()) return "Nessuna vittoria registrata finora.\nGioca per essere il primo!";
        
        List<String> righe = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String linea;
            while ((linea = br.readLine()) != null) righe.add(linea);
        } catch (Exception e) {}
        
        if (righe.isEmpty()) return "Classifica vuota.";
        
        // Ordina dal più forte al più scarso
        righe.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(b.split(":")[1]), Integer.parseInt(a.split(":")[1]));
            } catch (Exception e) { return 0; }
        });
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < righe.size(); i++) {
            String[] p = righe.get(i).split(":");
            sb.append(i + 1).append("° Posto - ").append(p[0]).append(" (Vittorie: ").append(p[1]).append(")\n");
        }
        return sb.toString();
    }
}