package it.unibs.pajc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class Deck {
    private List<Card> carte;

    public Deck() {
        carte = new ArrayList<>();
        String[] semi = {"Cuori", "Quadri", "Fiori", "Picche"};
        String[] ranks = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
        
        for (String seme : semi) {
            for (String rank : ranks) {
                carte.add(new Card(seme, rank));
            }
        }
        Collections.shuffle(carte);
    }

    public synchronized Card drawCard() {
        if (carte.isEmpty()) {
            System.out.println("Mazzo esaurito, ricreazione mazzo.");
            new Deck();
        }
        return carte.remove(0);
    }
}
