package it.unibs.pajc;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerModel room;
    private ObjectOutputStream out;
    private ObjectInputStream in;

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

    class Mano {
        List<Card> carte = new ArrayList<>();
        int scommessa; boolean sballata = false; boolean blackjack = false;
        Mano(int s) { scommessa = s; }
    }
    private List<Mano> mani = new ArrayList<>();

    public ClientHandler(Socket s, ServerModel r) { this.socket = s; this.room = r; }

    public String getNickname() { return nickname; }
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

    public synchronized void forzaAggiornamentoVisivo() {
        try { inviaStato(faseAttuale, msgAttuale, fineTurnoAttuale, finePartitaAttuale, manoIdxAttuale); }
        catch (IOException e) { }
    }

    // === METODI HELPER PER IL TIMER ===
    private void avviaTimer(int secondi) {
        fermaTimer(); // Pulisce eventuali timer precedenti
        timerThread = new Thread(() -> {
            try {
                for (int i = secondi; i > 0; i--) {
                    secondiAttesaPersonale = i;
                    forzaAggiornamentoVisivo(); // Invia lo schermo aggiornato col tempo
                    Thread.sleep(1000); // Aspetta un secondo
                }
            } catch (InterruptedException e) {
                // Timer fermato dall'utente
            } finally {
                secondiAttesaPersonale = 0;
            }
        });
        timerThread.start();
    }

    private void fermaTimer() {
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt(); // Spegne il thread del timer
        }
        secondiAttesaPersonale = 0;
    }
    // ==================================

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // FIX NICKNAME: Attende in silenzio il nome dalla GUI e verifica che sia unico!
            String nickRichiesto = (String) in.readObject();
            this.nickname = room.ottieniNicknameUnico(nickRichiesto);
            room.aggiungiGiocatore(this);

            while (true) {
                mani.clear();
                inGioco = false;
                inviaStato(GameState.FaseGioco.ATTESA, "In attesa del prossimo round...", true, false, 0);
                room.aggiornaTavolo();

                room.attendiIlTuoTurno();

                inGioco = true;
                room.aggiornaTavolo();

                int scommessaIniziale = 0;
                inviaStato(GameState.FaseGioco.SCOMMESSA, "Hai " + fiches + " fiches. Quanto scommetti?", false, false, 0);
                avviaTimer(30);

                try {
                    socket.setSoTimeout(30000);
                    while (true) {
                        String input = (String) in.readObject();
                        scommessaIniziale = Integer.parseInt(input.trim());
                        if (scommessaIniziale > 0 && scommessaIniziale <= fiches) {
                            fiches -= scommessaIniziale;
                            fermaTimer();
                            break;
                        } else {
                            out.writeObject("⚠️ Importo non valido. Riprova:");
                        }
                    }
                } catch (SocketTimeoutException e) {
                    fermaTimer();
                    scommessaIniziale = (fiches < 100) ? fiches : 100;
                    fiches -= scommessaIniziale;
                    out.writeObject("⏳ Tempo scaduto! Scommessa automatica.");
                } finally {
                    socket.setSoTimeout(0);
                }

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
                    inviaStato(GameState.FaseGioco.ASSICURAZIONE, "⚠️ Assicurazione per " + costoAssic + " fiches? (si/no)", false, false, 0);
                    avviaTimer(30);
                    try {
                        socket.setSoTimeout(30000);
                        String risp = (String) in.readObject();
                        fermaTimer();
                        if (risp.equalsIgnoreCase("si") && fiches >= costoAssic) {
                            assicurazione = costoAssic;
                            fiches -= assicurazione;
                            out.writeObject(dealerHaBlackjack ? "🛡️ Assicurazione paga!" : "❌ Assicurazione persa.");
                            if (dealerHaBlackjack) fiches += (assicurazione * 3);
                        }
                    } catch (SocketTimeoutException e) {fermaTimer(); }
                    finally { socket.setSoTimeout(0); }
                }

                if (room.getHandValue(manoPrincipale.carte) == 21) manoPrincipale.blackjack = true;

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

                            inviaStato(GameState.FaseGioco.TURNO_GIOCATORE, opzioni, false, false, i);
                            avviaTimer(60);

                            try {
                                socket.setSoTimeout(60000);
                                String cmd = (String) in.readObject();
                                fermaTimer();

                                if (cmd.equalsIgnoreCase("carta")) {
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    if (room.getHandValue(manoAttuale.carte) >= 21) {
                                        manoAttuale.sballata = (room.getHandValue(manoAttuale.carte) > 21);
                                        turnoAttivo = false;
                                    }
                                    room.aggiornaTavolo();
                                } else if (cmd.equalsIgnoreCase("sto")) {
                                    turnoAttivo = false;
                                } else if (cmd.equalsIgnoreCase("raddoppio") && canDouble) {
                                    fiches -= manoAttuale.scommessa; manoAttuale.scommessa *= 2;
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    if (room.getHandValue(manoAttuale.carte) > 21) manoAttuale.sballata = true;
                                    turnoAttivo = false;
                                    room.aggiornaTavolo();
                                } else if (cmd.equalsIgnoreCase("split") && canSplit) {
                                    fiches -= manoAttuale.scommessa;
                                    Mano nuovaMano = new Mano(manoAttuale.scommessa);
                                    nuovaMano.carte.add(manoAttuale.carte.remove(1));
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    nuovaMano.carte.add(room.getDeck().drawCard());
                                    mani.add(nuovaMano);
                                    out.writeObject("♠️ Mano divisa!");
                                    room.aggiornaTavolo();
                                } else {
                                    out.writeObject("⚠️ Comando non valido.");
                                }
                            } catch (SocketTimeoutException e) {
                                fermaTimer();
                                out.writeObject("⏳ Tempo scaduto!");
                                turnoAttivo = false;
                            } finally { socket.setSoTimeout(0); }
                        }
                    }
                }

                inviaStato(GameState.FaseGioco.ATTESA, "In attesa del banco...", true, false, 0);
                room.fineTurnoGiocatore();
                room.attendiFineMano();

                int dealerTot = room.getDealerScore();
                for (Mano mano : mani) {
                    int pTot = room.getHandValue(mano.carte);
                    if (mano.sballata) continue;
                    if (mano.blackjack && !dealerHaBlackjack) fiches += (int)(mano.scommessa * 2.5);
                    else if (dealerTot > 21 || pTot > dealerTot) fiches += mano.scommessa * 2;
                    else if (pTot == dealerTot) fiches += mano.scommessa;
                }

                if (fiches <= 0) {
                    inviaStato(GameState.FaseGioco.ATTESA, "💸 BANCAROTTA! Game Over.", true, true, 0);
                    room.sceltaEffettuata();
                    break;
                } else {
                    inviaStato(GameState.FaseGioco.FINE_MANO, "Partita conclusa! Scrivi 'si' o 'no'.", true, true, 0);
                    avviaTimer(30);
                    String choice = null;
                    try {
                        socket.setSoTimeout(30000);
                        choice = (String) in.readObject();
                        fermaTimer();
                    } catch (SocketTimeoutException e) {
                        fermaTimer();
                        out.writeObject("⏳ Tempo scaduto!");
                    } finally { socket.setSoTimeout(0); }

                    room.sceltaEffettuata();
                    if (choice == null || !choice.equalsIgnoreCase("si")) break;
                }
            }
        } catch (Exception e) { }
        finally {
            fermaTimer();
            boolean turnoCompletato = (faseAttuale == GameState.FaseGioco.ATTESA || faseAttuale == GameState.FaseGioco.FINE_MANO);
            room.aPlayerLeft(turnoCompletato);
            room.rimuoviGiocatore(this);
            try { socket.close(); } catch (IOException ex) {}
        }
    }

    private synchronized void inviaStato(GameState.FaseGioco fase, String msg, boolean fineTurno, boolean finePartita, int manoIdx) throws IOException {
        this.faseAttuale = fase; this.msgAttuale = msg; this.fineTurnoAttuale = fineTurno;
        this.finePartitaAttuale = finePartita; this.manoIdxAttuale = manoIdx;

        Map<String, List<List<String>>> avversari = new HashMap<>();
        List<ClientHandler> copiaGiocatori = new ArrayList<>(room.getGiocatoriSeduti());
        for (ClientHandler altro : copiaGiocatori) {
            if (altro != this && altro.getNickname() != null) {
                avversari.put(altro.getNickname(), altro.getManiInStringhe());
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
            maniStr.add(cStr);
            puntMani.add(room.getHandValue(m.carte));
            scomMani.add(m.scommessa);
        }

        // SPEDISCE ANCHE I SECONDI DI ATTESA
        int timerVisibile = Math.max(room.getSecondiAttesa(), this.secondiAttesaPersonale);
        out.writeObject(new GameState(fase, dealerCards, dealerVisScore, maniStr, puntMani, scomMani, manoIdx, fiches, msg, fineTurno, finePartita, avversari,timerVisibile));
        out.reset();
    }
}