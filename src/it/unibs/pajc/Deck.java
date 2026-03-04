package it.unibs.pajc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Deck {
    private List<Card> carte;

    public Deck() {
        carte = new ArrayList<>();
        reset(); // Usiamo il metodo per riempire il mazzo fin da subito
    }

    // NOVITÀ: Metodo per svuotare, riempire e mescolare il mazzo attuale
    public synchronized void reset() {
        carte.clear(); // Svuota eventuali carte rimaste
        String[] semi = {"Cuori", "Quadri", "Fiori", "Picche"};
        String[] ranks = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};

        for (String seme : semi) {
            for (String rank : ranks) {
                carte.add(new Card(seme, rank));
            }
        }
        Collections.shuffle(carte);
        System.out.println("Il mazzo è stato mescolato e ripristinato (52 carte).");
    }

    public synchronized Card drawCard() {
        if (carte.isEmpty()) {
            System.out.println("Mazzo esaurito durante la pescata! Ricreazione mazzo in corso...");
            reset(); // RICHIAMA IL METODO PER RIEMPIRE L'OGGETTO CORRENTE!
        }
        return carte.remove(0); // Rimuove e restituisce sempre la prima carta (indice 0)
    }
}