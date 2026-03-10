package it.unibs.pajc;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class BlackjackGUI extends JFrame implements GameUpdateListener {

    private Client client; // <-- NOTA: Usa "Client" o "GameClient" a seconda di come si chiama la tua classe
    private GameState statoAttuale;

    // Componenti dell'Interfaccia
    private JLabel lblMessaggioServer;
    private JLabel lblFiches;
    private JPanel panelBanco;
    private JPanel panelGiocatore;
    private JPanel panelAvversari;

    private JPanel panelComandi;
    private JLabel lblTestoComando; // <--- AGGIUNGI QUESTA RIGA
    private JButton btnCarta, btnSto, btnRaddoppio, btnSplit;
    private JButton btnSi, btnNo;
    private JTextField txtScommessa;
    private JButton btnScommetti;

    private final int LARGHEZZA_CARTA = 100;
    private final int ALTEZZA_CARTA = 145;

    // Costruttore: riceve il nickname direttamente dalla schermata di Login
    public BlackjackGUI(String nickname) {
        super("Casinò MVC - Blackjack (" + nickname + ")");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Centra la finestra sullo schermo

        inizializzaInterfaccia();

        client = new Client(this);
        client.connetti("localhost", 12345);

        // Un piccolissimo ritardo per assicurarsi che la connessione di rete sia pronta
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        // Invia SILENZIOSAMENTE il nickname al Server appena connesso
        client.inviaComando(nickname);

        setVisible(true);
    }

    private void inizializzaInterfaccia() {
        JPanel panelInfo = new JPanel(new GridLayout(2, 1));
        panelInfo.setBackground(new Color(34, 40, 49));

        lblMessaggioServer = new JLabel("Connessione in corso...", SwingConstants.CENTER);
        lblMessaggioServer.setForeground(Color.WHITE);
        lblMessaggioServer.setFont(new Font("Arial", Font.BOLD, 18));

        lblFiches = new JLabel("Fiches: ---", SwingConstants.CENTER);
        lblFiches.setForeground(new Color(255, 215, 0));
        lblFiches.setFont(new Font("Arial", Font.BOLD, 16));

        panelInfo.add(lblMessaggioServer);
        panelInfo.add(lblFiches);
        add(panelInfo, BorderLayout.NORTH);

        JPanel tavoloVerde = new JPanel(new GridLayout(3, 1));
        tavoloVerde.setBackground(new Color(0, 100, 0));

        panelBanco = new JPanel(new FlowLayout());
        panelBanco.setOpaque(false);
        panelBanco.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "BANCO", 0, 0, null, Color.WHITE));

        panelAvversari = new JPanel(new GridLayout(1, 3, 10, 0));
        panelAvversari.setOpaque(false);
        panelAvversari.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "AVVERSARI AL TAVOLO", 0, 0, null, Color.LIGHT_GRAY));

        panelGiocatore = new JPanel(new FlowLayout());
        panelGiocatore.setOpaque(false);
        panelGiocatore.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "LE TUE MANI", 0, 0, null, Color.WHITE));

        tavoloVerde.add(panelBanco);
        tavoloVerde.add(panelAvversari);
        tavoloVerde.add(panelGiocatore);
        add(tavoloVerde, BorderLayout.CENTER);

        panelComandi = new JPanel(new FlowLayout());
        panelComandi.setBackground(Color.LIGHT_GRAY);

        btnCarta = new JButton("Carta");
        btnSto = new JButton("Sto");
        btnRaddoppio = new JButton("Raddoppio");
        btnSplit = new JButton("Split");
        btnSi = new JButton("Sì");
        btnNo = new JButton("No");
        txtScommessa = new JTextField(5);
        btnScommetti = new JButton("Punta");

        // IL FIX ASSOLUTO: Nascondiamo tutti i bottoni all'avvio!
        // Appariranno solo quando il Server manderà l'autorizzazione.
        btnCarta.setVisible(false);
        btnSto.setVisible(false);
        btnRaddoppio.setVisible(false);
        btnSplit.setVisible(false);
        btnSi.setVisible(false);
        btnNo.setVisible(false);
        txtScommessa.setVisible(false);
        btnScommetti.setVisible(false);

        btnCarta.addActionListener(e -> client.inviaComando("carta"));
        btnSto.addActionListener(e -> client.inviaComando("sto"));
        btnRaddoppio.addActionListener(e -> client.inviaComando("raddoppio"));
        btnSplit.addActionListener(e -> client.inviaComando("split"));
        btnSi.addActionListener(e -> client.inviaComando("si"));
        btnNo.addActionListener(e -> client.inviaComando("no"));

        btnScommetti.addActionListener(e -> {
            try {
                Integer.parseInt(txtScommessa.getText());
                client.inviaComando(txtScommessa.getText());
                txtScommessa.setText("");
            } catch (Exception ex) {
                lblMessaggioServer.setText("⚠️ Inserisci un numero valido!");
            }
        });

        lblTestoComando = new JLabel("Scommessa:");
        lblTestoComando.setFont(new Font("Arial", Font.BOLD, 14));
        panelComandi.add(lblTestoComando);
        panelComandi.add(txtScommessa);
        panelComandi.add(btnScommetti);
        panelComandi.add(btnCarta);
        panelComandi.add(btnSto);
        panelComandi.add(btnRaddoppio);
        panelComandi.add(btnSplit);
        panelComandi.add(btnSi);
        panelComandi.add(btnNo);

        add(panelComandi, BorderLayout.SOUTH);
    }

    @Override
    public void sulMessaggioDiTesto(String messaggio) {
        SwingUtilities.invokeLater(() -> lblMessaggioServer.setText(messaggio));
    }

    @Override
    public void onStateUpdate(GameState state) {
        SwingUtilities.invokeLater(() -> aggiornaSchermo(state));
    }

    private void aggiornaSchermo(GameState state) {
        this.statoAttuale = state;


        // ===============================================
        // LA MAGIA UNIVERSALE DEL TIMER: Se c'è tempo da mostrare, lo mostriamo!
        // ===============================================
        if (state.getSecondiAttesa() > 0) {
            lblMessaggioServer.setText(state.getMessaggioAvviso() + "  ⏳ " + state.getSecondiAttesa() + "s");
        } else if (state.getMessaggioAvviso() != null) {
            lblMessaggioServer.setText(state.getMessaggioAvviso());
        }

        lblFiches.setText("Fiches: " + state.getFiches() + " 💰");

        panelBanco.removeAll();
        panelGiocatore.removeAll();
        panelAvversari.removeAll();

        if (!state.getCarteDealer().isEmpty()) {
            JLabel lblPuntiBanco = new JLabel(" (Punti: " + state.getPunteggioDealer() + ") ");
            lblPuntiBanco.setForeground(Color.WHITE);
            panelBanco.add(lblPuntiBanco);
            for (String nomeCarta : state.getCarteDealer()) panelBanco.add(creaLabelCarta(nomeCarta));
        }

        List<List<String>> mani = state.getManiGiocatore();
        for (int i = 0; i < mani.size(); i++) {
            JPanel singolaMano = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            singolaMano.setOpaque(false);

            Color coloreTesto = (i == state.getIndiceManoAttuale() && !state.isTurnoFinito() && !state.isFinePartita()) ? Color.YELLOW : Color.WHITE;
            JLabel lblPunti = new JLabel("Mano " + (i+1) + " (Punti: " + state.getPunteggiMani().get(i) + ") ");
            lblPunti.setForeground(coloreTesto);
            singolaMano.add(lblPunti);

            for (String nomeCarta : mani.get(i)) singolaMano.add(creaLabelCarta(nomeCarta));

            int scommessa = state.getScommesseMani().get(i);
            if (scommessa > 0) singolaMano.add(creaLabelFiche(scommessa));

            panelGiocatore.add(singolaMano);
        }

        int sedieOccupate = 0;
        if (state.getAltriGiocatori() != null) {
            for (Map.Entry<String, List<List<String>>> entry : state.getAltriGiocatori().entrySet()) {
                if (sedieOccupate >= 3) break;

                String nick = entry.getKey();
                List<List<String>> maniAvversario = entry.getValue();

                JPanel slotGiocatore = new JPanel(new FlowLayout(FlowLayout.CENTER));
                slotGiocatore.setOpaque(false);
                slotGiocatore.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), nick, 0, 0, null, Color.ORANGE));

                if (maniAvversario.isEmpty()) {
                    JLabel lblAttesa = new JLabel("In attesa...");
                    lblAttesa.setForeground(Color.LIGHT_GRAY);
                    slotGiocatore.add(lblAttesa);
                } else {
                    for (List<String> mano : maniAvversario) {
                        for (String carta : mano) slotGiocatore.add(creaLabelCartaPiccola(carta));
                    }
                }
                panelAvversari.add(slotGiocatore);
                sedieOccupate++;
            }
        }

        while (sedieOccupate < 3) {
            JPanel slotVuoto = new JPanel(new BorderLayout());
            slotVuoto.setOpaque(false);
            slotVuoto.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "Posto Libero", 0, 0, null, Color.DARK_GRAY));
            JLabel lblVuoto = new JLabel("Nessuno", SwingConstants.CENTER);
            lblVuoto.setForeground(Color.DARK_GRAY);
            slotVuoto.add(lblVuoto, BorderLayout.CENTER);
            panelAvversari.add(slotVuoto);
            sedieOccupate++;
        }

        gestisciBottoni(state.getFaseAttuale());

        panelBanco.revalidate();
        panelBanco.repaint();
        panelAvversari.revalidate();
        panelAvversari.repaint();
        panelGiocatore.revalidate();
        panelGiocatore.repaint();
    }

    private void gestisciBottoni(GameState.FaseGioco fase) {
        boolean isScommessa = (fase == GameState.FaseGioco.SCOMMESSA);
        boolean isTurno = (fase == GameState.FaseGioco.TURNO_GIOCATORE);
        boolean isScelta = (fase == GameState.FaseGioco.FINE_MANO || fase == GameState.FaseGioco.ASSICURAZIONE);

        if (isScommessa) {
            lblTestoComando.setText("Inserisci Puntata: ");
        } else if (isTurno) {
            lblTestoComando.setText("Qual è la tua mossa? ");
        } else if (isScelta) {
            lblTestoComando.setText("Rispondi: ");
        } else {
            lblTestoComando.setText(""); // Scompare quando sei in semplice attesa!
        }

        txtScommessa.setVisible(isScommessa);
        btnScommetti.setVisible(isScommessa);

        btnCarta.setVisible(isTurno);
        btnSto.setVisible(isTurno);
        btnRaddoppio.setVisible(isTurno);
        btnSplit.setVisible(isTurno);

        btnSi.setVisible(isScelta);
        btnNo.setVisible(isScelta);
    }

    private JLabel creaLabelCarta(String nomeCarta) {
        JLabel carta = new JLabel("", SwingConstants.CENTER);
        String path = nomeCarta.equals("[CARTA COPERTA]") ? "images/retro.png" : "images/" + nomeCarta.replace(" ", "_") + ".png";

        ImageIcon iconaOriginale = new ImageIcon(path);
        Dimension dimensioniFisse = new Dimension(LARGHEZZA_CARTA, ALTEZZA_CARTA);

        carta.setPreferredSize(dimensioniFisse);
        carta.setMinimumSize(dimensioniFisse);
        carta.setMaximumSize(dimensioniFisse);

        if (iconaOriginale.getIconWidth() == -1) {
            carta.setText("Manca: " + path);
            carta.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            carta.setOpaque(true);
            carta.setBackground(Color.WHITE);
        } else {
            Image img = iconaOriginale.getImage();
            Image nuovaImg = img.getScaledInstance(LARGHEZZA_CARTA, ALTEZZA_CARTA, Image.SCALE_SMOOTH);
            carta.setIcon(new ImageIcon(nuovaImg));
        }
        return carta;
    }

    private JLabel creaLabelCartaPiccola(String nomeCarta) {
        JLabel carta = new JLabel("", SwingConstants.CENTER);
        String path = nomeCarta.equals("[CARTA COPERTA]") ? "images/retro.png" : "images/" + nomeCarta.replace(" ", "_") + ".png";

        ImageIcon iconaOriginale = new ImageIcon(path);
        Dimension dimPiccola = new Dimension(60, 87);

        carta.setPreferredSize(dimPiccola);
        carta.setMinimumSize(dimPiccola);
        carta.setMaximumSize(dimPiccola);

        if (iconaOriginale.getIconWidth() != -1) {
            Image img = iconaOriginale.getImage().getScaledInstance(60, 87, Image.SCALE_SMOOTH);
            carta.setIcon(new ImageIcon(img));
        } else {
            carta.setText("?");
            carta.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
            carta.setOpaque(true);
            carta.setBackground(Color.WHITE);
        }
        return carta;
    }

    private JLabel creaLabelFiche(int importo) {
        JLabel lblFiche = new JLabel(importo + "$", SwingConstants.CENTER);
        lblFiche.setForeground(new Color(255, 215, 0));
        lblFiche.setFont(new Font("Arial", Font.BOLD, 18));

        String nomeFile = "chipWhite.png";
        if (importo >= 500) nomeFile = "chipPurple.png";
        else if (importo >= 100) nomeFile = "chipBlack.png";
        else if (importo >= 50) nomeFile = "chipBlue.png";
        else if (importo >= 25) nomeFile = "chipGreen.png";
        else if (importo >= 10) nomeFile = "chipYellow.png";
        else if (importo >= 5) nomeFile = "chipRed.png";

        ImageIcon iconaOriginale = new ImageIcon("images/" + nomeFile);

        if (iconaOriginale.getIconWidth() != -1) {
            Image img = iconaOriginale.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            lblFiche.setIcon(new ImageIcon(img));
            lblFiche.setVerticalTextPosition(SwingConstants.BOTTOM);
            lblFiche.setHorizontalTextPosition(SwingConstants.CENTER);
        } else {
            lblFiche.setText("🪙 " + importo + "$");
        }

        lblFiche.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        return lblFiche;
    }

    // ==========================================
    // IL NUOVO PUNTO DI INGRESSO (MAIN)
    // ==========================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> avviaSchermataDiLogin());
    }

    private static void avviaSchermataDiLogin() {
        // Creiamo una nuova piccola finestra dedicata solo al login
        JFrame loginFrame = new JFrame("Login Casinò");
        loginFrame.setSize(350, 150);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 20));
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setResizable(false);

        JLabel lblInfo = new JLabel("Inserisci il tuo Nickname per giocare:");
        lblInfo.setFont(new Font("Arial", Font.BOLD, 14));

        JTextField txtNick = new JTextField(15);
        txtNick.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton btnEntra = new JButton("Entra al Tavolo");
        btnEntra.setFont(new Font("Arial", Font.BOLD, 14));
        btnEntra.setBackground(new Color(0, 153, 51)); // Verde
        btnEntra.setForeground(Color.WHITE);

        // L'azione quando premi il bottone
        btnEntra.addActionListener(e -> {
            String nick = txtNick.getText().trim();
            if (nick.isEmpty()) {
                nick = "Giocatore_" + (int)(Math.random() * 1000);
            }
            loginFrame.dispose(); // Chiude la finestrella del login
            new BlackjackGUI(nick); // Apre il tavolo da gioco passandogli il nome!
        });

        // Permette di entrare premendo direttamente il tasto "Invio" sulla tastiera
        loginFrame.getRootPane().setDefaultButton(btnEntra);

        loginFrame.add(lblInfo);
        loginFrame.add(txtNick);
        loginFrame.add(btnEntra);
        loginFrame.setVisible(true);
    }
}