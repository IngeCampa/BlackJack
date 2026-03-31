package it.unibs.pajc;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerModel room; 
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    //Un buffer per i messaggi in arrivo, così il thread principale può leggerli con calma e gestire i timeout
    private final BlockingQueue<String> codaMessaggi = new LinkedBlockingQueue<>();
    
    private int fiches = 1000;
    private boolean inGioco = false; 
    private String nickname;
    private int secondiAttesaPersonale = 0;
    private Thread timerThread;

    private GameState.FaseGioco faseAttuale = GameState.FaseGioco.ATTESA; 
    private String msgAttuale = "";
    private boolean fineTurnoAttuale = false;
    private boolean finePartitaAttuale = false;
    private int manoIdxAttuale = 0;

    // Rappresenta una mano di gioco, con le carte, la scommessa e lo stato
    class Mano { 
        List<Card> carte = new ArrayList<>(); 
        int scommessa; 
        boolean sballata = false; 
        boolean blackjack = false;
        
        Mano(int s)
        { 
        	scommessa = s;
        } 
    }
    private List<Mano> mani = new ArrayList<>();

    public ClientHandler(Socket s, ServerModel r) 
    { 
    	this.socket = s;
    	this.room = r; 
    }

    public String getNickname() { return nickname; }
    public int getSecondiAttesaPersonale() { return secondiAttesaPersonale; }
    
    /** Restituisce le mani del giocatore in formato stringa,per inviarle al client senza esporre l'oggetto Card.
	 * Se il giocatore non è in gioco, restituisce una lista vuota.
	 */
    public List<List<String>> getManiInStringhe() {
        List<List<String>> maniStr = new ArrayList<>();
        if (!inGioco) return maniStr;
        for (Mano m : mani) {
            List<String> cStr = new ArrayList<>();
            for (Card c : m.carte) cStr.add(c.toString());
            maniStr.add(cStr);
        }
        return maniStr;
    }
    
    /** Restituisce le scommesse di ogni mano del giocatore, per mostrarle agli avversari.
	 * Se il giocatore non è in gioco, restituisce una lista vuota.
	 */
    public List<Integer> getScommesseAvversario() {
        List<Integer> scom = new ArrayList<>();
        if (!inGioco) 
        	return scom;
        for (Mano m : mani) 
        { 
        	scom.add(m.scommessa); 
        }
        return scom;
    }

    /** Forza un aggiornamento visivo immediato del client, inviando lo stato attuale.
	 * quando qualcosa cambia e vogliamo essere sicuri che il client lo veda subito
	 */
    public synchronized void forzaAggiornamentoVisivo() {
        try { 
        		inviaStato(faseAttuale, msgAttuale, fineTurnoAttuale, finePartitaAttuale, manoIdxAttuale); 
        	} 
        catch (IOException e) { }
    }
    
    /** Invia un messaggio di testo diretto al client, senza cambiare lo stato di gioco.
     *  per comunicazioni istantanee come errori, conferme o notifiche che non richiedono un aggiornamento completo dello stato.
     **/
    public synchronized void inviaTestoDiretto(String msg) {
        try { 
        	out.writeObject(msg); out.reset();
        	} 
        catch (Exception e) {}
    }
    
    private String leggiConTimeout(int secondi) throws Exception {
        String msg = codaMessaggi.poll(secondi, TimeUnit.SECONDS);
        if (msg == null) 
        	throw new Exception("TIMEOUT");
        return msg;
    }

    /** Avvia un timer per il giocatore, che conta alla rovescia i secondi rimasti per una decisione.
	 * Il timer aggiorna continuamente il tempo rimanente e lo comunica a tutti i giocatori, così l'interfaccia può mostrarlo.
	 * Se il timer scade, interrompe l'attesa del giocatore e forza una scelta automatica.
	 **/
    private void avviaTimer(int secondi) {
        fermaTimer(); 
        timerThread = new Thread(() -> {
            try {
                for (int i = secondi; i > 0; i--) {
                    secondiAttesaPersonale = i; 
                    room.aggiornaTavolo(); // Comunica il tempo a TUTTI
                    Thread.sleep(1000); 
                }
            } catch (InterruptedException e) {
            } finally { 
                secondiAttesaPersonale = 0; 
                room.aggiornaTavolo(); 
            }
        });
        timerThread.start();
    }

    /** Ferma il timer personale del giocatore, se è attivo.
     * Usato quando il giocatore prende una decisione prima dello scadere del tempo, per evitare che il timer continui a contare e crei confusione.
     **/
    private void fermaTimer() {
        if (timerThread != null && timerThread.isAlive()) 
        	timerThread.interrupt(); 
        secondiAttesaPersonale = 0;
    }

    /** Il metodo principale del thread, che gestisce l'interazione con il client.
     * 
     */
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            /*il thread secondario si occupa di leggere i messaggi in arrivo dal client e metterli in coda, 
            così il thread principale può gestirli con calma e rispettare i timeout*/
            new Thread(() -> {
                try {
                    while (true) {
                        String input = (String) in.readObject();
                        if (input.equalsIgnoreCase("esci")) {
                            inviaTestoDiretto("DISCONNESSIONE");
                            socket.close(); 
                            break;
                        } else if (input.startsWith("CHAT:")) {
                            room.broadcastChat(nickname, input.substring(5));
                        } else if (input.equalsIgnoreCase("CLASSIFICA")) { 
                            inviaTestoDiretto("CLASSIFICA_DATI:\n" + room.ottieniClassifica());
                        } else {
                            codaMessaggi.put(input);
                        }
                    }
                } catch (Exception e) {}
            }).start();
            
            String nickRichiesto = codaMessaggi.take();
            this.nickname = room.ottieniNicknameUnico(nickRichiesto);
            room.aggiungiSpettatore(this);
           
            /* Se la partita è già in corso, il nuovo arrivato diventa spettatore e aspetta la fine del round per sedersi.
			 * Se la stanza è piena, il nuovo arrivato entra in coda come spettatore e aspetta che si liberi un posto.
			 */
            boolean inCoda = false;
            while (true) {
                if (room.isPartitaInCorso()) {
                    inviaStato(GameState.FaseGioco.ATTESA, "Mano in corso... Attendi la fine del round per sederti.", false, false, 0);
                    try { 
                    	room.attendiProssimoRound(); 
                    	} catch (InterruptedException e) { 
                    		throw new Exception("Disconnesso"); 
                    		}
                }

                int postiOccupati = 0;
                
                synchronized(room.getGiocatoriSeduti()) { 
                	postiOccupati = room.getGiocatoriSeduti().size(); 
                }

                if (postiOccupati < 4) break; 
                
                if (!inCoda) {
                    inviaStato(GameState.FaseGioco.ATTESA, "Stanza Piena (4/4)! Sei in coda come Spettatore...", true, false, 0);
                    inCoda = true;
                }
                forzaAggiornamentoVisivo(); 
                try { 
                		Thread.sleep(2000);
                	} catch (InterruptedException ex) {}
            }
            
            room.rimuoviSpettatore(this);
            room.aggiungiGiocatore(this); 
           
            while (true) {
                mani.clear(); inGioco = false; 
                inviaStato(GameState.FaseGioco.ATTESA, "In attesa del prossimo round...", true, false, 0);
                room.attendiIlTuoTurno(); 

                inGioco = true; 
                int scommessaIniziale = 0;
                
                codaMessaggi.clear(); 
                inviaStato(GameState.FaseGioco.SCOMMESSA, "Hai " + fiches + " fiches. Quanto scommetti?", false, false, 0);
                avviaTimer(20);
                
                try {
                    while (true) {
                        String input = leggiConTimeout(20);
                        if (input.equalsIgnoreCase("esci")) throw new Exception("Uscita Volontaria");
                        
                        try {
                            scommessaIniziale = Integer.parseInt(input.trim());
                            if (scommessaIniziale > 0 && scommessaIniziale <= fiches) {
                                fiches -= scommessaIniziale; 
                                fermaTimer();
                                break;
                            } else {
                                inviaTestoDiretto("Importo non valido. Riprova:");
                            }
                        } catch (NumberFormatException ex) { } 
                    }
                } catch (Exception e) {
                    fermaTimer();
                    if (e.getMessage() != null && e.getMessage().equals("TIMEOUT")) {
                        scommessaIniziale = (fiches < 100) ? fiches : 100;
                        fiches -= scommessaIniziale;
                        inviaTestoDiretto("Tempo scaduto! Scommessa automatica.");
                    } else throw e;
                }

                inviaStato(GameState.FaseGioco.ATTESA, "Scommessa confermata! In attesa degli altri giocatori...", false, false, 0);
                room.confermaScommessaEAttendiCarte();

                mani.clear();
                Mano manoPrincipale = new Mano(scommessaIniziale);
                manoPrincipale.carte.add(room.getDeck().drawCard()); 
                manoPrincipale.carte.add(room.getDeck().drawCard());
                mani.add(manoPrincipale); 
                room.aggiornaTavolo(); 

                boolean dealerHaBlackjack = (room.getDealerScore() == 21);
                int assicurazione = 0;
                
                if (room.getDealerHand().get(0).rank.equals("A")) {
                    int costoAssic = scommessaIniziale / 2;
                    codaMessaggi.clear(); 
                    inviaStato(GameState.FaseGioco.ASSICURAZIONE, "⚠️ Assicurazione per " + costoAssic + " fiches? (si/no)", false, false, 0);
                    avviaTimer(15);
                    try {
                        while(true) {
                            String risp = leggiConTimeout(15);
                            if (risp.equalsIgnoreCase("esci")) 
                            	throw new Exception("Uscita Volontaria");
                            fermaTimer();
                            if (risp.equalsIgnoreCase("si") && fiches >= costoAssic) {
                                assicurazione = costoAssic; fiches -= assicurazione;
                                inviaTestoDiretto(dealerHaBlackjack ? "🛡️ Assicurazione paga!" : "❌ Assicurazione persa.");
                                
                                if (dealerHaBlackjack) 
                                	fiches += (assicurazione * 3);
                            }
                            break;
                        }
                    } catch (Exception e) { 
                        fermaTimer(); 
                        if (e.getMessage() == null || !e.getMessage().equals("TIMEOUT")) throw e; 
                    }
                }

                if (room.getHandValue(manoPrincipale.carte) == 21) 
                	manoPrincipale.blackjack = true;

                if (!dealerHaBlackjack) {
                    for (int i = 0; i < mani.size(); i++) {
                        Mano manoAttuale = mani.get(i);
                        if (manoAttuale.blackjack) continue;
                        
                        boolean turnoAttivo = true;
                        while (turnoAttivo) {
                            boolean canDouble = (manoAttuale.carte.size() == 2 && fiches >= manoAttuale.scommessa);
                            boolean canSplit = (canDouble && manoAttuale.carte.get(0).rank.equals(manoAttuale.carte.get(1).rank) && mani.size() < 4);

                            String opzioni = "Mossa (Mano " + (i+1) + "): 'carta', 'sto'";
                            if (canDouble) opzioni += ", 'raddoppio'"; 
                            if (canSplit) opzioni += ", 'split'";

                            codaMessaggi.clear(); 
                            inviaStato(GameState.FaseGioco.TURNO_GIOCATORE, opzioni, false, false, i);
                            avviaTimer(30);
                            
                            try {
                                String cmd = leggiConTimeout(30);
                                if (cmd.equalsIgnoreCase("esci")) throw new Exception("Uscita Volontaria");
                                fermaTimer();
                                
                                if (cmd.equalsIgnoreCase("carta")) {
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    room.aggiornaTavolo(); // Mostra SUBITO la carta pescata
                                    
                                    if (room.getHandValue(manoAttuale.carte) >= 21) { 
                                        manoAttuale.sballata = (room.getHandValue(manoAttuale.carte) > 21); 
                                        if (manoAttuale.sballata) {
                                            inviaTestoDiretto("Sballato! Hai superato 21.");
                            
                                            try { 
                                            	Thread.sleep(2500);
                                            	} catch (Exception ex) {} 
                                        }
                                        turnoAttivo = false; 
                                    } 
                                } else if (cmd.equalsIgnoreCase("sto")) { 
                                    turnoAttivo = false;
                                } else if (cmd.equalsIgnoreCase("raddoppio") && canDouble) {
                                    fiches -= manoAttuale.scommessa; manoAttuale.scommessa *= 2; 
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    
                                    if (room.getHandValue(manoAttuale.carte) > 21) manoAttuale.sballata = true;
                                    turnoAttivo = false; room.aggiornaTavolo();
                                    
                                } else if (cmd.equalsIgnoreCase("split") && canSplit) {
                                    fiches -= manoAttuale.scommessa; Mano nuovaMano = new Mano(manoAttuale.scommessa);
                                    nuovaMano.carte.add(manoAttuale.carte.remove(1)); 
                                    manoAttuale.carte.add(room.getDeck().drawCard()); nuovaMano.carte.add(room.getDeck().drawCard());
                                    mani.add(nuovaMano); inviaTestoDiretto("Mano divisa!"); room.aggiornaTavolo();
                                } else { inviaTestoDiretto("⚠️ Comando non valido."); }
                            } catch (Exception e) {
                                fermaTimer(); 
                                if (e.getMessage() != null && e.getMessage().equals("TIMEOUT")) { 
                                    inviaTestoDiretto("⏳ Tempo scaduto! Turno saltato."); 
                                    turnoAttivo = false; 
                                } else throw e;
                            }
                        }
                    }
                }

                inviaStato(GameState.FaseGioco.ATTESA, "In attesa del banco...", true, false, 0);
                room.fineTurnoGiocatore(); 
                room.attendiFineMano(); 

                int dealerTot = room.getDealerScore();
                boolean haVintoAlmenoUnaMano = false;
                
                for (Mano mano : mani) {
                    int pTot = room.getHandValue(mano.carte);
                    if (mano.sballata) 
                    	continue; 
                    if (mano.blackjack && !dealerHaBlackjack){
                    	fiches += (int)(mano.scommessa * 2.5); 
                    	haVintoAlmenoUnaMano = true;
                	}
                    else if (dealerTot > 21 || pTot > dealerTot) {
                    	fiches += mano.scommessa * 2; 
                    	haVintoAlmenoUnaMano = true;
                    }
                    else if (pTot == dealerTot) {
                    	fiches += mano.scommessa; 
                    }
                }
                
                if (haVintoAlmenoUnaMano) {
                    room.registraVittoria(this.nickname);
               }

                if (fiches <= 0) {
                    inviaStato(GameState.FaseGioco.ATTESA, "BANCAROTTA! Hai perso tutto. Game Over.", true, true, 0);
                    try { 
                    	Thread.sleep(6000); 
                    	} catch (Exception ex) {} 
                    break; 
                } else {
                    inviaStato(GameState.FaseGioco.FINE_MANO, "Partita conclusa! Prossima mano in arrivo...", true, true, 0);
                    room.attendiFineRisultati(); 
                }
            } 
        } catch (Exception e) { /*e.printStackTrace();*/ } 
        finally { 
            fermaTimer();
            room.aPlayerLeft(inGioco, faseAttuale == GameState.FaseGioco.ATTESA || faseAttuale == GameState.FaseGioco.FINE_MANO); 
            
            room.rimuoviGiocatore(this); 
            room.rimuoviSpettatore(this);
            
            inviaTestoDiretto("DISCONNESSIONE");
            try { 
            	socket.close(); 
            	} catch (IOException ex) {}
        }
    }

    /** Invia lo stato di gioco aggiornato al client, con tutte le informazioni necessarie per visualizzare correttamente la situazione attuale.
	 * Questo metodo è sincronizzato per evitare che più thread inviino stati contrastanti o incompleti.
	 * Il metodo raccoglie tutte le informazioni rilevanti e le invia al client in un unico oggetto GameState.
	 **/
    private synchronized void inviaStato(GameState.FaseGioco fase, String msg, boolean fineTurno, boolean finePartita, int manoIdx) throws IOException {
        this.faseAttuale = fase; this.msgAttuale = msg; this.fineTurnoAttuale = fineTurno;
        this.finePartitaAttuale = finePartita; this.manoIdxAttuale = manoIdx;
        
        Map<String, List<List<String>>> avversari = new HashMap<>();
        Map<String, List<Integer>> scommesseAvversari = new HashMap<>();
        List<ClientHandler> copiaGiocatori;
        synchronized(room.getGiocatoriSeduti()) { 
        	copiaGiocatori = new ArrayList<>(room.getGiocatoriSeduti()); 
        	}
        
        for (ClientHandler altro : copiaGiocatori) {
            if (altro != this && altro.getNickname() != null) {
                avversari.put(altro.getNickname(), altro.getManiInStringhe());
                scommesseAvversari.put(altro.getNickname(), altro.getScommesseAvversario());
            }
        }

        List<String> dealerCards = new ArrayList<>();
        int dealerVisScore = 0;
        if (!room.getDealerHand().isEmpty()) {
            dealerCards.add(room.getDealerHand().get(0).toString());
            dealerVisScore = room.getDealerHand().get(0).getValue();
            if (!finePartita) dealerCards.add("[CARTA COPERTA]");
            else { 
                dealerCards.clear(); 
                for(Card c : room.getDealerHand()) dealerCards.add(c.toString()); 
                dealerVisScore = room.getDealerScore(); 
            }
        }

        List<List<String>> maniStr = new ArrayList<>();
        List<Integer> puntMani = new ArrayList<>();
        List<Integer> scomMani = new ArrayList<>();
        
        for (Mano m : mani) {
            List<String> cStr = new ArrayList<>();
            for (Card c : m.carte) cStr.add(c.toString());
            maniStr.add(cStr); puntMani.add(room.getHandValue(m.carte)); scomMani.add(m.scommessa);
        }

        int maxTimerPersonale = this.secondiAttesaPersonale; 
        for (ClientHandler ch : copiaGiocatori) {
            if (ch.getSecondiAttesaPersonale() > maxTimerPersonale) {
                maxTimerPersonale = ch.getSecondiAttesaPersonale();
            }
        }
        
        int timerVisibile = Math.max(room.getSecondiAttesa(), maxTimerPersonale);
        
        if (fase == GameState.FaseGioco.ATTESA && room.isPartitaInCorso()) {
            timerVisibile = 0;
        }
        
        out.writeObject(new GameState(fase, dealerCards, dealerVisScore, maniStr, puntMani, scomMani, manoIdx, fiches, msg, fineTurno, finePartita, avversari, scommesseAvversari, timerVisibile));
        out.reset();
    }
}