package it.unibs.pajc;
public interface GameUpdateListener {
    void onStateUpdate(GameState state);
    void sulMessaggioDiTesto(String messaggio);
}