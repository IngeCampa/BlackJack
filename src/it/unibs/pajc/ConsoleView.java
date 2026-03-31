package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleView implements GameUpdateListener {
    private Client client;
    private GameState statoAttuale; 

    public static void main(String[] args) { new ConsoleView().avvia(); }

    /** Avvia il client e gestisce l'input dell'utente in base alla fase del gioco.
	 * 1. FASE SCOMMESSA: accetta solo input numerici per le scommesse.
	 * 2. FASE ATTESA: blocca qualsiasi input e mostra un messaggio di attesa.
	 * 3. FASE FINE MANO: accetta solo "si" o "no" per continuare o uscire.
	 * Per tutte le altre fasi, invia semplicemente il comando al server.
	 */
    public void avvia() {
        client = new Client(this);
        client.connetti("localhost", 12345);
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String cmd;
            while ((cmd = in.readLine()) != null) {
                if (statoAttuale != null) {
                   
                    if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.SCOMMESSA) {
                        try {
                            Integer.parseInt(cmd); 
                            client.inviaComando(cmd); 
                        } catch (NumberFormatException e) {
                            System.out.println("⚠️ ERRORE: Devi inserire un numero valido per scommettere!");
                        }
                    } 
                    
                    else if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.ATTESA) {
                        System.out.println("⏳ Shhh! Non è il tuo turno, non puoi inviare comandi ora.");
                    } 
                   
                    else if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.FINE_MANO) {
                        if (cmd.equalsIgnoreCase("si") || cmd.equalsIgnoreCase("no")) {
                            client.inviaComando(cmd);
                        } else {
                            System.out.println("⚠️ Scrivi 'si' per restare o 'no' per uscire.");
                        }
                    }
                    else {
                        client.inviaComando(cmd);
                    }
                }
            }
        } catch (Exception e) { }
    }

    @Override
    public void sulMessaggioDiTesto(String msg) { 
        System.out.println("\nSERVER: " + msg); 
    }

    
    /** Aggiorna la visualizzazione del gioco in console ogni volta che riceve un nuovo stato dal server.
	 * Mostra:
	 * - Le carte e il punteggio del dealer.
	 * - Le mani, i punteggi e le scommesse del giocatore, evidenziando la mano attuale.
	 * - Un messaggio di avviso se presente.
	 * - Il totale delle fiches del giocatore.
	 * Se il giocatore è uno spettatore (non ha mani), nasconde il tavolo e mostra solo il messaggio di avviso.
	 */
    @Override
    public void onStateUpdate(GameState state) {
        this.statoAttuale = state; 

        System.out.println("\n======================================");
        
        if(state.getMessaggioAvviso() != null) {
            System.out.println("" + state.getMessaggioAvviso());
        }

        
        if (state.getManiGiocatore().isEmpty()) {
            System.out.println("======================================\n");
            return; 
        }

        System.out.println("\n BANCO: " + state.getCarteDealer() + " | Punti: " + state.getPunteggioDealer());
        
        System.out.println("\n LE TUE MANI:");
        for (int i = 0; i < state.getManiGiocatore().size(); i++) {
            String pointer = (i == state.getIndiceManoAttuale() && !state.isTurnoFinito() && !state.isFinePartita()) ? "👉 " : "   ";
            System.out.println(pointer + "Mano " + (i+1) + ": " + state.getManiGiocatore().get(i) + 
                               " | Punti: " + state.getPunteggiMani().get(i) + 
                               " | Scommessa: " + state.getScommesseMani().get(i));
        }
        System.out.println("\n Fiches totali: " + state.getFiches());
        System.out.println("======================================\n");
    }
}