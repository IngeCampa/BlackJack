package it.unibs.pajc;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleView implements GameUpdateListener {
    private Client client;
    private GameState statoAttuale;

    public static void main(String[] args) { new ConsoleView().avvia(); }

    public void avvia() {
        client = new Client(this);
        client.connetti("localhost", 12345);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String cmd;
            while ((cmd = in.readLine()) != null) {
                if (statoAttuale != null) {
                    // 1. FASE SCOMMESSA (Deve essere un numero)
                    if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.SCOMMESSA) {
                        try {
                            Integer.parseInt(cmd);
                            client.inviaComando(cmd);
                        } catch (NumberFormatException e) {
                            System.out.println("⚠️ ERRORE: Devi inserire un numero valido per scommettere!");
                        }
                    }
                    // 2. FASE ATTESA (Blocca tutto lo spam della tastiera)
                    else if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.ATTESA) {
                        System.out.println("⏳ Shhh! Non è il tuo turno, non puoi inviare comandi ora.");
                    }
                    // 3. FASE FINE MANO (Permetti solo di rispondere 'si' o 'no')
                    else if (statoAttuale.getFaseAttuale() == GameState.FaseGioco.FINE_MANO) {
                        if (cmd.equalsIgnoreCase("si") || cmd.equalsIgnoreCase("no")) {
                            client.inviaComando(cmd);
                        } else {
                            System.out.println("⚠️ Scrivi 'si' per restare o 'no' per uscire.");
                        }
                    }
                    // 4. TUTTE LE ALTRE FASI (Invia normalmente)
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

    @Override
    public void onStateUpdate(GameState state) {
        this.statoAttuale = state;

        System.out.println("\n======================================");

        if(state.getMessaggioAvviso() != null) {
            System.out.println("📢 " + state.getMessaggioAvviso());
        }

        // Fix visivo: nascondi il tavolo se sei spettatore
        if (state.getManiGiocatore().isEmpty()) {
            System.out.println("======================================\n");
            return;
        }

        System.out.println("\n🏦 BANCO: " + state.getCarteDealer() + " | Punti: " + state.getPunteggioDealer());

        System.out.println("\n👤 LE TUE MANI:");
        for (int i = 0; i < state.getManiGiocatore().size(); i++) {
            String pointer = (i == state.getIndiceManoAttuale() && !state.isTurnoFinito() && !state.isFinePartita()) ? "👉 " : "   ";
            System.out.println(pointer + "Mano " + (i+1) + ": " + state.getManiGiocatore().get(i) +
                    " | Punti: " + state.getPunteggiMani().get(i) +
                    " | Scommessa: " + state.getScommesseMani().get(i));
        }
        System.out.println("\n💰 Fiches totali: " + state.getFiches());
        System.out.println("======================================\n");
    }
}