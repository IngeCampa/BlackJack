package it.unibs.pajc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 2L; // Aggiornato alla v2!

    private final List<String> carteDealer;
    private final int punteggioDealer;

    // ==========================================
    // FIX: SUPPORTO PER LO SPLIT (Mani Multiple)
    // ==========================================
    private final List<List<String>> maniGiocatore;
    private final List<Integer> punteggiMani;
    private final List<Integer> scommesseMani;
    private final int indiceManoAttuale; // Dice alla UI quale mano stiamo giocando ora

    private final int fiches;
    private final String messaggioAvviso;
    private final boolean turnoFinito;
    private final boolean finePartita;

    public GameState(List<String> carteDealer, int punteggioDealer,
                     List<List<String>> maniGiocatore, List<Integer> punteggiMani, List<Integer> scommesseMani,
                     int indiceManoAttuale, int fiches, String messaggioAvviso,
                     boolean turnoFinito, boolean finePartita) {

        this.carteDealer = new ArrayList<>(carteDealer);
        this.punteggioDealer = punteggioDealer;
        this.maniGiocatore = maniGiocatore;
        this.punteggiMani = punteggiMani;
        this.scommesseMani = scommesseMani;
        this.indiceManoAttuale = indiceManoAttuale;
        this.fiches = fiches;
        this.messaggioAvviso = messaggioAvviso;
        this.turnoFinito = turnoFinito;
        this.finePartita = finePartita;
    }

    public List<String> getCarteDealer() { return carteDealer; }
    public int getPunteggioDealer() { return punteggioDealer; }
    public List<List<String>> getManiGiocatore() { return maniGiocatore; }
    public List<Integer> getPunteggiMani() { return punteggiMani; }
    public List<Integer> getScommesseMani() { return scommesseMani; }
    public int getIndiceManoAttuale() { return indiceManoAttuale; }
    public int getFiches() { return fiches; }
    public String getMessaggioAvviso() { return messaggioAvviso; }
    public boolean isTurnoFinito() { return turnoFinito; }
    public boolean isFinePartita() { return finePartita; }
}