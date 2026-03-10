package it.unibs.pajc;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    public enum FaseGioco { ATTESA, SCOMMESSA, ASSICURAZIONE, TURNO_GIOCATORE, FINE_MANO }

    private FaseGioco faseAttuale;
    private List<String> carteDealer;
    private int punteggioDealer;
    private List<List<String>> maniGiocatore;
    private List<Integer> punteggiMani;
    private List<Integer> scommesseMani;
    private int indiceManoAttuale;
    private int fiches;
    private String messaggioAvviso;
    private boolean turnoFinito;
    private boolean finePartita;
    private Map<String, List<List<String>>> altriGiocatori;

    // NUOVO: Timer per l'interfaccia
    private int secondiAttesa;

    public GameState(FaseGioco faseAttuale, List<String> carteDealer, int punteggioDealer,
                     List<List<String>> maniGiocatore, List<Integer> punteggiMani, List<Integer> scommesseMani,
                     int indiceManoAttuale, int fiches, String messaggioAvviso, boolean turnoFinito, boolean finePartita,
                     Map<String, List<List<String>>> altriGiocatori, int secondiAttesa) {
        this.faseAttuale = faseAttuale;
        this.carteDealer = carteDealer;
        this.punteggioDealer = punteggioDealer;
        this.maniGiocatore = maniGiocatore;
        this.punteggiMani = punteggiMani;
        this.scommesseMani = scommesseMani;
        this.indiceManoAttuale = indiceManoAttuale;
        this.fiches = fiches;
        this.messaggioAvviso = messaggioAvviso;
        this.turnoFinito = turnoFinito;
        this.finePartita = finePartita;
        this.altriGiocatori = altriGiocatori;
        this.secondiAttesa = secondiAttesa; // Salvato!
    }

    public FaseGioco getFaseAttuale() { return faseAttuale; }
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
    public Map<String, List<List<String>>> getAltriGiocatori() { return altriGiocatori; }
    public int getSecondiAttesa() { return secondiAttesa; }
}