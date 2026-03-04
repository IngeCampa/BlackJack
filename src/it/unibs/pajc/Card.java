package it.unibs.pajc;

/**
 * Rappresenta una singola carta da gioco del mazzo.
 * Contiene le informazioni su seme, valore nominale (rank) e
 * il calcolo del punteggio per le regole del Blackjack.
 */
public class Card {

    // ==========================================
    // ATTRIBUTI IMMUTABILI DELLA CARTA
    // ==========================================
    // Usiamo 'public final' perché le caratteristiche di una carta
    // non cambiano mai dopo la sua creazione.
    public final String seme;
    public final String rank;

    /**
     * Costruttore della classe Card.
     * * @param seme Il seme della carta (es. "Cuori", "Picche").
     * @param rank Il valore nominale o figura (es. "2", "10", "J", "A").
     */
    public Card(String seme, String rank) {
        this.seme = seme;
        this.rank = rank;
    }

    // ==========================================
    // METODI DELLA LOGICA DI GIOCO
    // ==========================================

    /**
     * Calcola e restituisce il valore numerico della carta nel Blackjack.
     * Nota: La flessibilità dell'Asso (1 o 11) viene gestita dal Server
     * nel calcolo totale della mano, qui forniamo solo il valore base.
     * * @return Il valore numerico della carta.
     */
    public int getValue() {
        switch(rank) {
            case "J":
            case "Q":
            case "K":
                // Le figure valgono sempre 10
                return 10;

            case "A":
                // L'asso parte con valore 11 di default
                return 11;

            default:
                // Per le carte numeriche da "2" a "10", convertiamo la stringa in numero intero
                return Integer.parseInt(rank);
        }
    }

    // ==========================================
    // METODI DI UTILITÀ VISIVA
    // ==========================================

    /**
     * Restituisce una rappresentazione testuale della carta.
     * Questo metodo viene richiamato in automatico quando fai System.out.println(carta).
     * * @return Una stringa formattata (es. "A di Picche", "7 di Cuori").
     */
    @Override
    public String toString() {
        return rank + " di " + seme;
    }
}