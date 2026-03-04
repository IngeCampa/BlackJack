package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

class ClientHandler implements Runnable {

    // Variabili di rete e di gioco
    private Socket socket;
    private Deck deck;
    private BufferedReader in;
    private PrintWriter out;
    private List<Card> dealerHand;
    private Server server;

    // Il portafoglio del giocatore
    private int fiches = 1000;

    /**
     * Classe interna per gestire le mani multiple.
     * Necessaria per implementare la meccanica dello "Split".
     */
    private class ManoGiocatore {
        List<Card> carte = new ArrayList<>();
        int scommessa;
        boolean sballata = false;
        boolean blackjack = false;

        public ManoGiocatore(int scommessa) {
            this.scommessa = scommessa;
        }
    }

    // Costruttore
    public ClientHandler(Socket socket, Deck deck, List<Card> sharedDealerHand, Server server) throws IOException {
        this.socket = socket;
        this.deck = deck;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.dealerHand = sharedDealerHand;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            boolean keepGoing = true;

            // ==========================================
            // CICLO PRINCIPALE DELLE PARTITE
            // ==========================================
            while (keepGoing) {
                // Sincronizza il giocatore con il tavolo
                server.attendiIlTuoTurno(out);

                // ==========================================
                // FASE DI SCOMMESSA (CON TIMEOUT 30 SECONDI)
                // ==========================================
                out.println("Hai " + fiches + " fiches. Quanto vuoi scommettere? Hai 30 secondi. (digita l'importo):");
                int scommessaIniziale = 0;

                try {
                    // Imposta il timer di 30 secondi per la socket
                    socket.setSoTimeout(30000);
                    while (true) {
                        String input = in.readLine();
                        if (input == null) {
                            keepGoing = false;
                            break;
                        }

                        try {
                            scommessaIniziale = Integer.parseInt(input.trim());
                            if (scommessaIniziale > 0 && scommessaIniziale <= fiches) {
                                fiches -= scommessaIniziale;
                                out.println("Hai scommesso " + scommessaIniziale + " fiches. Fiches rimanenti: " + fiches);
                                break;
                            } else {
                                out.println("Importo non valido. Hai " + fiches + " fiches. Riprova:");
                            }
                        } catch (NumberFormatException e) {
                            out.println("Per favore, digita un numero intero valido:");
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Se il timer scade, forza una scommessa automatica
                    out.println("\nTempo scaduto! Il croupier piazza la scommessa per te.");
                    scommessaIniziale = (fiches < 100) ? fiches : 100; // All-in se ha meno di 100
                    fiches -= scommessaIniziale;
                    out.println("Hai scommesso automaticamente " + scommessaIniziale + " fiches.");
                } finally {
                    // Ripristina il timeout a infinito per il resto della partita
                    try { socket.setSoTimeout(0); } catch (IOException e) { }
                }

                // Se il client si è disconnesso durante la scommessa, esci dal ciclo principale
                if (!keepGoing) break;

                out.println("Benvenuto al Blackjack!");

                // ==========================================
                // DISTRIBUZIONE CARTE INIZIALI
                // ==========================================
                List<ManoGiocatore> mani = new ArrayList<>();
                ManoGiocatore manoPrincipale = new ManoGiocatore(scommessaIniziale);
                manoPrincipale.carte.add(deck.drawCard());
                manoPrincipale.carte.add(deck.drawCard());
                mani.add(manoPrincipale);

                out.println("\nCarta visibile del dealer: " + dealerHand.get(0));

                // ==========================================
                // L'ASSICURAZIONE (Se il dealer mostra un Asso)
                // ==========================================
                int assicurazione = 0;
                boolean dealerHaBlackjack = (getHandValue(dealerHand) == 21);

                if (dealerHand.get(0).rank.equals("A")) {
                    int costoAssicurazione = scommessaIniziale / 2;
                    out.println("⚠️ Il banco ha un Asso! Vuoi comprare l'assicurazione per " + costoAssicurazione + " fiches? (si/no)");
                    String risp = in.readLine();

                    if (risp != null && risp.equalsIgnoreCase("si") && fiches >= costoAssicurazione) {
                        assicurazione = costoAssicurazione;
                        fiches -= assicurazione;
                        out.println("Assicurazione acquistata. Fiches rimanenti: " + fiches);
                    } else if (risp != null && risp.equalsIgnoreCase("si")) {
                        out.println("Non hai abbastanza fiches per l'assicurazione!");
                    }

                    out.println("Il croupier controlla la carta coperta...");

                    // Risoluzione immediata dell'assicurazione
                    if (dealerHaBlackjack) {
                        out.println("🚨 È UN BLACKJACK NATURALE DEL BANCO! 🚨");
                        if (assicurazione > 0) {
                            out.println("🛡️ L'assicurazione ti salva! (Pagamento 2:1)");
                            fiches += (assicurazione * 3); // Restituisce il costo + paga 2:1
                        }
                    } else {
                        out.println("Nessun Blackjack per il banco. Il gioco continua.");
                        if (assicurazione > 0) {
                            out.println("❌ Perdi i soldi dell'assicurazione.");
                            assicurazione = 0; // Soldi persi
                        }
                    }
                }

                // ==========================================
                // CONTROLLO BLACKJACK NATURALE DEL GIOCATORE
                // ==========================================
                if (getHandValue(manoPrincipale.carte) == 21) {
                    manoPrincipale.blackjack = true;
                    out.println("🔥 BLACKJACK NATURALE! 21! Le tue carte: " + manoPrincipale.carte + " 🔥");
                }

                // ==========================================
                // CICLO DI GIOCO (Richiesta Carte, Split, Raddoppio)
                // ==========================================
                // Se il banco ha Blackjack, il turno del giocatore viene saltato
                if (!dealerHaBlackjack) {
                    // Ciclo for sulle mani (possono aumentare se si usa lo Split)
                    for (int i = 0; i < mani.size(); i++) {
                        ManoGiocatore manoAttuale = mani.get(i);

                        // Salta se ha già Blackjack
                        if (manoAttuale.blackjack) continue;

                        boolean accesso = true;
                        while (accesso) {
                            out.println("\n[Mano " + (i + 1) + "/" + mani.size() + "] Carte: " + manoAttuale.carte + " | Totale: " + getHandValue(manoAttuale.carte));

                            // Valutazione opzioni disponibili
                            boolean canDouble = (manoAttuale.carte.size() == 2 && fiches >= manoAttuale.scommessa);

                            // Limite massimo di 4 mani per lo Split (regola standard da casinò)
                            boolean canSplit = (canDouble && manoAttuale.carte.get(0).rank.equals(manoAttuale.carte.get(1).rank) && mani.size() < 4);

                            // Composizione del menu dinamico
                            String opzioni = "Digita 'carta', 'sto'";
                            if (canDouble) opzioni += ", 'raddoppio'";
                            if (canSplit) opzioni += ", 'split'";
                            opzioni += ":";

                            out.println(opzioni);

                            String comando = null;

                            // ==========================================
                            // TIMER AFK (Away From Keyboard)
                            // Se il giocatore non fa scelte per 60s, forza lo "Sto"
                            // ==========================================
                            try {
                                socket.setSoTimeout(60000); // 60 secondi di attesa
                                comando = in.readLine();
                            } catch (SocketTimeoutException e) {
                                out.println("\n⏳ Tempo scaduto! Il croupier ti impone di fermarti (Sto automatico).");
                                accesso = false; // Forza la fine del turno
                            } finally {
                                try { socket.setSoTimeout(0); } catch (IOException e) { } // Ripristina timeout
                            }

                            // Disconnessione
                            if (comando == null && accesso) {
                                accesso = false;
                                keepGoing = false;
                                break;
                            }

                            // GESTIONE COMANDI
                            if (accesso) {
                                if (comando.equalsIgnoreCase("carta")) {
                                    Card carta = deck.drawCard();
                                    manoAttuale.carte.add(carta);

                                    int totaleDopoPescata = getHandValue(manoAttuale.carte);
                                    if (totaleDopoPescata > 21) {
                                        out.println("Hai pescato: " + carta + " | Totale: " + totaleDopoPescata + " -> Hai sballato!");
                                        manoAttuale.sballata = true;
                                        accesso = false; // Fine turno per sballo
                                    } else if (totaleDopoPescata == 21) {
                                        out.println("Hai pescato: " + carta + " | Hai raggiunto 21 perfetto! Ti fermi in automatico.");
                                        accesso = false; // Fine turno automatico
                                    } else {
                                        out.println("Hai pescato: " + carta);
                                    }

                                } else if (comando.equalsIgnoreCase("sto")) {
                                    accesso = false; // Fine turno volontaria

                                } else if (comando.equalsIgnoreCase("raddoppio") && canDouble) {
                                    fiches -= manoAttuale.scommessa;
                                    manoAttuale.scommessa *= 2; // Raddoppia la puntata
                                    Card carta = deck.drawCard();
                                    manoAttuale.carte.add(carta); // Una sola carta pescata

                                    out.println("Hai RADDOPPIATO! Hai pescato: " + carta + " | Totale finale: " + getHandValue(manoAttuale.carte));
                                    if (getHandValue(manoAttuale.carte) > 21) manoAttuale.sballata = true;
                                    accesso = false; // Il raddoppio forza la fine del turno

                                } else if (comando.equalsIgnoreCase("split") && canSplit) {
                                    fiches -= manoAttuale.scommessa;

                                    // Crea la nuova mano rubando una carta dalla mano attuale
                                    ManoGiocatore nuovaMano = new ManoGiocatore(manoAttuale.scommessa);
                                    nuovaMano.carte.add(manoAttuale.carte.remove(1));

                                    // Distribuisce una nuova carta a entrambe le mani
                                    manoAttuale.carte.add(deck.drawCard());
                                    nuovaMano.carte.add(deck.drawCard());

                                    mani.add(nuovaMano);
                                    out.println("Mano divisa! Continuiamo con la prima mano...");

                                } else {
                                    out.println("Comando non valido o non consentito in questo momento.");
                                }
                            }
                        }
                        if (!keepGoing) break;
                    }
                } else {
                    out.println("\nIl turno dei giocatori viene saltato per via del Blackjack del banco.");
                }

                if (!keepGoing) break;

                // ==========================================
                // ATTESA DEL BANCO E FINE MANO
                // ==========================================
                out.println("\nHai terminato i tuoi turni. In attesa del banco...");
                server.fineTurnoGiocatore(); // Notifica il server che ha finito
                server.attendiFineMano();    // Attende che anche gli altri (e il banco) finiscano

                // ==========================================
                // CALCOLO E STAMPA DEI RISULTATI FINALI
                // ==========================================
                int dealerTotal = getHandValue(dealerHand);
                out.println("\n--- RISULTATI FINALI ---");
                out.println("Mano finale del dealer: " + dealerHand + " | Totale: " + dealerTotal);

                // Cicla su tutte le mani giocate per calcolare le vincite
                for (int i = 0; i < mani.size(); i++) {
                    ManoGiocatore m = mani.get(i);
                    int playerTotal = getHandValue(m.carte);
                    out.print("Mano " + (i + 1) + " (" + playerTotal + "): ");

                    if (m.sballata) {
                        out.println("Persa! (Hai sballato)");
                    } else if (m.blackjack && dealerTotal != 21) {
                        // Paga 3:2 per Blackjack Naturale
                        out.println("Vinta con Blackjack Naturale! (+ " + (m.scommessa * 2.5) + ")");
                        fiches += (m.scommessa + (m.scommessa * 1.5));
                    } else if (dealerTotal > 21 || playerTotal > dealerTotal) {
                        // Paga 1:1 per vittoria normale
                        out.println("Vinta! (+ " + (m.scommessa * 2) + ")");
                        fiches += (m.scommessa * 2);
                    } else if (playerTotal < dealerTotal) {
                        out.println("Persa! (Dealer vince)");
                    } else {
                        // Restituisce la puntata in caso di pareggio
                        out.println("Pareggio! (+ " + m.scommessa + ")");
                        fiches += m.scommessa;
                    }
                }

                out.println("\nIl tuo saldo attuale è di: " + fiches + " fiches.");

                // ==========================================
                // CONTROLLO BANCAROTTA E SCELTA
                // ==========================================
                if (fiches <= 0) {
                    out.println("Hai esaurito le tue fiches! Game Over.");
                    keepGoing = false;
                    server.sceltaEffettuata();
                    break;
                }

                out.println("Vuoi restare al tavolo per un'altra partita? (si/no)");
                String choice = in.readLine();

                if (choice == null || !choice.equalsIgnoreCase("si")) {
                    keepGoing = false;
                    out.println("Grazie per aver giocato! Arrivederci.");
                    server.sceltaEffettuata();
                } else {
                    out.println("Ottima scelta! Preparo il prossimo round...\n---");
                    server.sceltaEffettuata();
                }
            }

        } catch (IOException e) {
            // Se un client chiude bruscamente la connessione, il codice salta qui
            System.err.println("Un giocatore si è disconnesso bruscamente: " + e.getMessage());

        } finally {
            // ==========================================
            // FIX: SCUDO ANTI-RAGEQUIT (Blocco Finally)
            // ==========================================
            // Questo blocco viene eseguito SEMPRE alla fine del thread,
            // garantendo che le risorse del Server vengano sbloccate.
            System.out.println("Esecuzione pulizia risorse per il giocatore...");

            // 1. Sblocca la fine della mano se il giocatore era in fase di scelta
            server.sceltaEffettuata();

            // 2. Decrementa i contatori dei giocatori in gioco/attesa
            server.aPlayerLeft();

            // 3. Chiude in sicurezza il socket
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                System.err.println("Errore durante la chiusura del socket.");
            }
        }
    }

    /**
     * Calcola il valore totale di una mano secondo le regole del Blackjack.
     * Gestisce dinamicamente il valore dell'Asso (1 o 11).
     *
     * @param mano La lista di carte da valutare.
     * @return Il punteggio ottimale della mano.
     */
    private int getHandValue(List<Card> mano) {
        int tot = 0;
        int assi = 0;
        for (Card c : mano) {
            tot += c.getValue();
            if (c.rank.equals("A")) assi++;
        }
        // Trasforma il valore dell'Asso da 11 a 1 per non sballare
        while (tot > 21 && assi > 0) {
            tot -= 10;
            assi--;
        }
        return tot;
    }
}