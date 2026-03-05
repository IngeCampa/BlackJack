package it.unibs.pajc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BlackjackRoom room;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private int fiches = 1000;
    private List<ManoGiocatore> mani;

    // La tua classe interna per gestire le mani multiple!
    private class ManoGiocatore {
        List<Card> carte = new ArrayList<>();
        int scommessa;
        boolean sballata = false;
        boolean blackjack = false;
        public ManoGiocatore(int scommessa) { this.scommessa = scommessa; }
    }

    public ClientHandler(Socket socket, BlackjackRoom room) {
        this.socket = socket;
        this.room = room;
        this.mani = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            boolean keepGoing = true;

            while (keepGoing) {
                out.writeObject("Benvenuto! Attendi il tuo turno al tavolo...");
                room.attendiIlTuoTurno();

                // ==========================================
                // 1. FASE SCOMMESSA (Timeout 30s)
                // ==========================================
                int scommessaIniziale = 0;
                out.writeObject("Hai " + fiches + " fiches. Quanto scommetti? (Hai 30 secondi):");
                try {
                    socket.setSoTimeout(30000);
                    while (true) {
                        String input = (String) in.readObject();
                        try {
                            scommessaIniziale = Integer.parseInt(input.trim());
                            if (scommessaIniziale > 0 && scommessaIniziale <= fiches) {
                                fiches -= scommessaIniziale;
                                break;
                            } else {
                                out.writeObject("Importo non valido. Riprova:");
                            }
                        } catch (NumberFormatException e) {
                            out.writeObject("Inserisci un numero valido:");
                        }
                    }
                } catch (SocketTimeoutException e) {
                    scommessaIniziale = (fiches < 100) ? fiches : 100;
                    fiches -= scommessaIniziale;
                    out.writeObject("⏳ Tempo scaduto! Il croupier piazza " + scommessaIniziale + " fiches per te.");
                } finally {
                    socket.setSoTimeout(0); // Ripristina il timeout
                }

                // ==========================================
                // 2. DISTRIBUZIONE CARTE
                // ==========================================
                mani.clear();
                ManoGiocatore manoPrincipale = new ManoGiocatore(scommessaIniziale);
                manoPrincipale.carte.add(room.getDeck().drawCard());
                manoPrincipale.carte.add(room.getDeck().drawCard());
                mani.add(manoPrincipale);

                boolean dealerHaBlackjack = (room.getDealerScore() == 21);

                // ==========================================
                // 3. ASSICURAZIONE
                // ==========================================
                if (room.getDealerHand().get(0).rank.equals("A")) {
                    int costoAssic = scommessaIniziale / 2;
                    inviaStatoAlClient("⚠️ Il banco ha un Asso! Vuoi l'assicurazione per " + costoAssic + " fiches? (si/no)", false, false, 0);

                    String risp = (String) in.readObject();
                    if (risp != null && risp.equalsIgnoreCase("si") && fiches >= costoAssic) {
                        fiches -= costoAssic;
                        if (dealerHaBlackjack) {
                            fiches += (costoAssic * 3);
                            out.writeObject("🛡️ L'assicurazione ti salva! Pagamento 2:1.");
                        } else {
                            out.writeObject("❌ Nessun Blackjack per il banco. Assicurazione persa.");
                        }
                    }
                }

                if (room.getHandValue(manoPrincipale.carte) == 21) {
                    manoPrincipale.blackjack = true;
                }

                // ==========================================
                // 4. CICLO DI GIOCO (Split, Raddoppio, Timeout 60s)
                // ==========================================
                if (!dealerHaBlackjack) {
                    for (int i = 0; i < mani.size(); i++) {
                        ManoGiocatore manoAttuale = mani.get(i);
                        if (manoAttuale.blackjack) continue;

                        boolean accesso = true;
                        while (accesso) {
                            boolean canDouble = (manoAttuale.carte.size() == 2 && fiches >= manoAttuale.scommessa);
                            boolean canSplit = (canDouble && manoAttuale.carte.get(0).rank.equals(manoAttuale.carte.get(1).rank) && mani.size() < 4);

                            String opzioni = "Mossa (Mano " + (i+1) + "): 'carta', 'sto'";
                            if (canDouble) opzioni += ", 'raddoppio'";
                            if (canSplit) opzioni += ", 'split'";

                            inviaStatoAlClient(opzioni, false, false, i);
                            String comando = null;

                            try {
                                socket.setSoTimeout(60000);
                                comando = (String) in.readObject();
                            } catch (SocketTimeoutException e) {
                                inviaStatoAlClient("⏳ Tempo scaduto! Sto automatico.", true, false, i);
                                accesso = false;
                            } finally {
                                socket.setSoTimeout(0);
                            }

                            if (comando != null && accesso) {
                                if (comando.equalsIgnoreCase("carta")) {
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    if (room.getHandValue(manoAttuale.carte) >= 21) {
                                        manoAttuale.sballata = (room.getHandValue(manoAttuale.carte) > 21);
                                        accesso = false;
                                    }
                                } else if (comando.equalsIgnoreCase("sto")) {
                                    accesso = false;
                                } else if (comando.equalsIgnoreCase("raddoppio") && canDouble) {
                                    fiches -= manoAttuale.scommessa;
                                    manoAttuale.scommessa *= 2;
                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    if (room.getHandValue(manoAttuale.carte) > 21) manoAttuale.sballata = true;
                                    accesso = false;
                                } else if (comando.equalsIgnoreCase("split") && canSplit) {
                                    fiches -= manoAttuale.scommessa;
                                    ManoGiocatore nuovaMano = new ManoGiocatore(manoAttuale.scommessa);
                                    nuovaMano.carte.add(manoAttuale.carte.remove(1));

                                    manoAttuale.carte.add(room.getDeck().drawCard());
                                    nuovaMano.carte.add(room.getDeck().drawCard());
                                    mani.add(nuovaMano);
                                    out.writeObject("Mano divisa con successo!");
                                } else {
                                    out.writeObject("Comando non valido.");
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 5. ATTESA BANCO E PAGAMENTI
                // ==========================================
                inviaStatoAlClient("In attesa del banco e degli altri giocatori...", true, false, 0);
                room.fineTurnoGiocatore();
                room.attendiFineMano();

                int dealerTotal = room.getDealerScore();
                StringBuilder risultati = new StringBuilder("RISULTATI:\n");

                for (int i = 0; i < mani.size(); i++) {
                    ManoGiocatore m = mani.get(i);
                    int playerTotal = room.getHandValue(m.carte);
                    risultati.append("- Mano ").append(i+1).append(": ");

                    if (m.sballata) {
                        risultati.append("Persa (Sballato).\n");
                    } else if (m.blackjack && !dealerHaBlackjack) {
                        risultati.append("Vinta! (Blackjack Naturale!)\n");
                        fiches += (m.scommessa * 2.5);
                    } else if (dealerTotal > 21 || playerTotal > dealerTotal) {
                        risultati.append("Vinta!\n");
                        fiches += (m.scommessa * 2);
                    } else if (playerTotal < dealerTotal) {
                        risultati.append("Persa.\n");
                    } else {
                        risultati.append("Pareggio.\n");
                        fiches += m.scommessa;
                    }
                }

                inviaStatoAlClient(risultati.toString(), true, true, 0);

                if (fiches <= 0) {
                    out.writeObject("Game Over. Fiches esaurite.");
                    keepGoing = false;
                } else {
                    out.writeObject("Vuoi restare al tavolo? (si/no)");
                    String choice = (String) in.readObject();
                    if (choice == null || !choice.equalsIgnoreCase("si")) keepGoing = false;
                }
                room.sceltaEffettuata();
            }
        } catch (Exception e) {
            System.out.println("Giocatore disconnesso.");
        } finally {
            room.sceltaEffettuata();
            room.aPlayerLeft();
            try { socket.close(); } catch (IOException ex) { }
        }
    }

    private void inviaStatoAlClient(String messaggio, boolean turnoFinito, boolean finePartita, int manoAttualeIndex) throws IOException {
        List<String> nomiBanco = new ArrayList<>();
        int punteggioVisibileBanco = 0;

        // Anti-Cheat: Copri la seconda carta del banco se si sta ancora giocando
        List<Card> carteRealiBanco = room.getDealerHand();
        if (!carteRealiBanco.isEmpty()) {
            nomiBanco.add(carteRealiBanco.get(0).toString());
            punteggioVisibileBanco = carteRealiBanco.get(0).getValue();
            if (!finePartita && carteRealiBanco.size() > 1) {
                nomiBanco.add("[CARTA COPERTA]");
            } else {
                for (int i = 1; i < carteRealiBanco.size(); i++) nomiBanco.add(carteRealiBanco.get(i).toString());
                punteggioVisibileBanco = room.getDealerScore();
            }
        }

        // Impacchetta tutte le mani del giocatore
        List<List<String>> stringheMani = new ArrayList<>();
        List<Integer> punteggiMani = new ArrayList<>();
        List<Integer> scommesseMani = new ArrayList<>();

        for (ManoGiocatore m : mani) {
            List<String> carteStr = new ArrayList<>();
            for (Card c : m.carte) carteStr.add(c.toString());
            stringheMani.add(carteStr);
            punteggiMani.add(room.getHandValue(m.carte));
            scommesseMani.add(m.scommessa);
        }

        GameState state = new GameState(nomiBanco, punteggioVisibileBanco, stringheMani, punteggiMani, scommesseMani, manoAttualeIndex, fiches, messaggio, turnoFinito, finePartita);
        out.writeObject(state);
        out.reset();
    }
}