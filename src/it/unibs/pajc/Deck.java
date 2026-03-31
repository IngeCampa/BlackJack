package it.unibs.pajc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> cards = new ArrayList<>();
    private final int NUMERO_DI_MAZZI = 4;

    public Deck() { 
    	reset();
    	}

    /** Ricostruisce il mazzo con 4 mazzi standard (52 carte ciascuno) e mescola le carte. 
	 * Viene chiamato quando il mazzo è vuoto o quasi vuoto per garantire che ci siano sempre abbastanza carte per giocare.
	 */
    public synchronized void reset() {
        cards.clear();
        String[] suits = {"Cuori", "Quadri", "Fiori", "Picche"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        for (int i = 0; i < NUMERO_DI_MAZZI; i++) {
            for (String suit : suits) {
                for (String rank : ranks) cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);
    }

    public synchronized Card drawCard() {
        if (cards.isEmpty()) reset();
        return cards.remove(0);
    }

    public synchronized int getRemainingCards() { 
    	return cards.size(); 
    	}
}