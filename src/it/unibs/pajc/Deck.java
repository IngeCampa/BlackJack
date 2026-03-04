package it.unibs.pajc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rappresenta il Sabot (Shoe) del tavolo da Blackjack.
 * Thread-safe: i metodi sono sincronizzati per evitare crash se più
 * giocatori pescano nello stesso istante.
 */
public class Deck {

    private List<Card> cards;
    private final int NUMERO_DI_MAZZI = 4;

    public Deck() {
        this.cards = new ArrayList<>();
        reset();
    }

    // Aggiunto "synchronized" per sicurezza multithread
    public synchronized void reset() {
        cards.clear();

        String[] suits = {"Cuori", "Quadri", "Fiori", "Picche"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (int i = 0; i < NUMERO_DI_MAZZI; i++) {
            for (String suit : suits) {
                for (String rank : ranks) {
                    cards.add(new Card(suit, rank));
                }
            }
        }

        Collections.shuffle(cards);
        System.out.println("🎲 Sabot rimescolato con " + NUMERO_DI_MAZZI + " mazzi (" + cards.size() + " carte in totale).");
    }

    // Aggiunto "synchronized" affinché un solo giocatore alla volta peschi
    public synchronized Card drawCard() {
        if (cards.isEmpty()) {
            System.out.println("⚠️ ATTENZIONE: Carte nel sabot esaurite! Rimescolamento automatico d'emergenza.");
            reset();
        }
        return cards.remove(0);
    }

    // Metodo utile al Server per sapere quando inserire il cartoncino rosso
    public synchronized int getRemainingCards() {
        return cards.size();
    }
}