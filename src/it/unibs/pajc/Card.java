package it.unibs.pajc;
import java.io.Serializable;

public class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String seme;
    public final String rank;

    public Card(String seme, String rank) {
        this.seme = seme;
        this.rank = rank;
    }

    public int getValue() {
        switch(rank) {
            case "J": case "Q": case "K": return 10;
            case "A": return 11;
            default: return Integer.parseInt(rank);
        }
    }

    @Override
    public String toString() { return rank + " di " + seme; }
}