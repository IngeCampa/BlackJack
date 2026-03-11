package it.unibs.pajc;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class BlackjackGUI extends JFrame implements GameUpdateListener {

    private Client client; 
    private GameState statoAttuale;

    // Componenti dell'Interfaccia
    private JLabel lblMessaggioServer;
    private JLabel lblFiches;
    private JPanel panelBanco;
    private JPanel panelGiocatore;
    private JPanel panelAvversari;
    
    private JPanel panelComandi;
    private JLabel lblTestoComando; 
    private JButton btnCarta, btnSto, btnRaddoppio, btnSplit;
    private JButton btnSi, btnNo;
    private JButton btnScommetti;
    private JButton btnEsci;

    private final int LARGHEZZA_CARTA = 100; 
    private final int ALTEZZA_CARTA = 145;
    
    private int puntataAttuale = 0;
    private JLabel lblPuntataAttuale;
    private JPanel panelFichesScommessa; 
    private JButton btnSvuotaPuntata;

    // Costruttore: riceve il nickname direttamente dalla schermata di Login
    public BlackjackGUI(String nickname, String ipAddress) {
        super("Blackjack (" + nickname + ")");
        setSize(1000, 800); 
        setResizable(false); // Blocca la dimensione della finestra
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Centra la finestra

        inizializzaInterfaccia();

        client = new Client(this); 
        
        boolean connesso = client.connetti(ipAddress, 12345);
        
        if (!connesso) {
            JOptionPane.showMessageDialog(null, 
                "Impossibile collegarsi alla stanza all'indirizzo: " + ipAddress + "\nAssicurati che il Server sia avviato e l'IP sia corretto!", 
                "Server Offline o Non Trovato", 
                JOptionPane.ERROR_MESSAGE);
                
            System.exit(0); 
        }
        
        try { Thread.sleep(200); } catch (InterruptedException ex) {}
        client.inviaComando(nickname); 
        
        setVisible(true);
    }

    private void inizializzaInterfaccia() {
        JPanel panelInfo = new JPanel(new GridLayout(2, 1));
        panelInfo.setBackground(new Color(34, 40, 49));
        
        // tabellone in alto messaggi del server
        lblMessaggioServer = new JLabel("Connessione in corso...", SwingConstants.CENTER);
        lblMessaggioServer.setForeground(Color.WHITE);
        lblMessaggioServer.setFont(new Font("Arial", Font.BOLD, 26));
        lblMessaggioServer.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        lblFiches = new JLabel("Fiches: ---", SwingConstants.CENTER);
        lblFiches.setForeground(new Color(255, 215, 0)); 
        lblFiches.setFont(new Font("Arial", Font.BOLD, 16));

        panelInfo.add(lblMessaggioServer);
        panelInfo.add(lblFiches);
        add(panelInfo, BorderLayout.NORTH);

        // pannello centrale verde con faretto 
        JPanel tavoloVerde = new JPanel(new GridLayout(3, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int w = getWidth();
                int h = getHeight();
                
                RadialGradientPaint paint = new RadialGradientPaint(
                    w / 2f, h / 2f, Math.max(w, h),
                    new float[]{0.0f, 0.8f}, 
                    new Color[]{new Color(0, 140, 0), new Color(0, 40, 0)}
                );
                
                g2d.setPaint(paint);
                g2d.fillRect(0, 0, w, h);
            }
        };
        
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
        
        JPanel panelSouthContainer = new JPanel(new BorderLayout());
        panelSouthContainer.setBackground(Color.LIGHT_GRAY);

        panelComandi = new JPanel(new FlowLayout());
        panelComandi.setOpaque(false);

        btnCarta = new JButton("Carta");
        btnSto = new JButton("Sto");
        btnRaddoppio = new JButton("Raddoppio");
        btnSplit = new JButton("Split");
        btnSi = new JButton("Sì");
        btnNo = new JButton("No");
        btnScommetti = new JButton("Punta");

        btnCarta.setVisible(false);
        btnSto.setVisible(false);
        btnRaddoppio.setVisible(false);
        btnSplit.setVisible(false);
        btnSi.setVisible(false);
        btnNo.setVisible(false);

        btnCarta.addActionListener(e -> client.inviaComando("carta"));
        btnSto.addActionListener(e -> client.inviaComando("sto"));
        btnRaddoppio.addActionListener(e -> client.inviaComando("raddoppio"));
        btnSplit.addActionListener(e -> client.inviaComando("split"));
        btnSi.addActionListener(e -> client.inviaComando("si"));
        btnNo.addActionListener(e -> client.inviaComando("no"));
        
        lblTestoComando = new JLabel("Scommessa:");
        lblTestoComando.setFont(new Font("Arial", Font.BOLD, 14));
        panelComandi.add(lblTestoComando);
        
        // ==========================================
        // RASTRELLIERA DELLE FICHES PER SCOMMETTERE
        // ==========================================
        panelFichesScommessa = new JPanel(new FlowLayout());
        panelFichesScommessa.setOpaque(false);

        int[] valoriFiches = {50, 100, 250, 500};
        String[] nomiFiches = {"chipBlack.png", "chipBlue.png", "chipGreen.png", "chipPurple.png"};

        for (int i = 0; i < valoriFiches.length; i++) {
            int val = valoriFiches[i];
            JButton btnChip = creaBottoneFiche(nomiFiches[i], val);
            btnChip.addActionListener(e -> {
                puntataAttuale += val;
                lblPuntataAttuale.setText("Totale: " + puntataAttuale + "$");
            });
            panelFichesScommessa.add(btnChip);
        }

        lblPuntataAttuale = new JLabel("Totale: 0$");
        lblPuntataAttuale.setFont(new Font("Arial", Font.BOLD, 16));
        lblPuntataAttuale.setForeground(new Color(0, 100, 0)); 

        btnSvuotaPuntata = new JButton("Svuota");
        btnSvuotaPuntata.addActionListener(e -> {
            puntataAttuale = 0;
            lblPuntataAttuale.setText("Totale: 0$");
        });

        btnScommetti = new JButton("Conferma Puntata");
        btnScommetti.setBackground(new Color(204, 153, 0)); 
        btnScommetti.setForeground(Color.WHITE);
        btnScommetti.setFont(new Font("Arial", Font.BOLD, 14));

        btnScommetti.addActionListener(e -> {
            // 1. Controllo di sicurezza: l'utente sta puntando più di quello che possiede?
            if (statoAttuale != null && puntataAttuale > statoAttuale.getFiches()) {
                // Mostra il Popup di Allerta!
                JOptionPane.showMessageDialog(BlackjackGUI.this, 
                    "Fondi insufficienti!\nStai cercando di puntare " + puntataAttuale + "$, ma hai solo " + statoAttuale.getFiches() + "$.", 
                    "Errore Puntata", 
                    JOptionPane.WARNING_MESSAGE);
                
                // Azzera la scommessa sbagliata per fargli riprovare
                puntataAttuale = 0;
                lblPuntataAttuale.setText("Totale: 0$");
                return; // Ferma l'esecuzione, non invia nulla al Server!
            }

            // 2. Controllo: la puntata è valida (maggiore di zero)?
            if (puntataAttuale > 0) {
                client.inviaComando(String.valueOf(puntataAttuale));
                puntataAttuale = 0; // Azzera per la mano successiva
                lblPuntataAttuale.setText("Totale: 0$");
            } else {
                // Sostituiamo anche il vecchio testo in alto con un Alert per la puntata nulla!
                JOptionPane.showMessageDialog(BlackjackGUI.this, 
                    "Devi inserire almeno una fiche sul tavolo per poter giocare!", 
                    "Puntata Vuota", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panelFichesScommessa.setVisible(false);
        lblPuntataAttuale.setVisible(false);
        btnSvuotaPuntata.setVisible(false);
        btnScommetti.setVisible(false);

        panelComandi.add(panelFichesScommessa); 
        panelComandi.add(lblPuntataAttuale);
        panelComandi.add(btnSvuotaPuntata);
        panelComandi.add(btnScommetti);
        
        panelComandi.add(btnCarta);
        panelComandi.add(btnSto);
        panelComandi.add(btnRaddoppio);
        panelComandi.add(btnSplit);
        panelComandi.add(btnSi);
        panelComandi.add(btnNo);
        
        // ==========================================
        // BOTTONE ESCI (ROSSO FUOCO A DESTRA)
        // ==========================================
        JPanel panelExit = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        panelExit.setOpaque(false);
        
        btnEsci = new JButton("Esci dal Tavolo");
        btnEsci.setBackground(new Color(180, 0, 0)); // Rosso Fuoco!
        btnEsci.setForeground(Color.WHITE);
        btnEsci.setFont(new Font("Arial", Font.BOLD, 14));
        btnEsci.setEnabled(false); // Inizialmente opaco e disabilitato
        
        btnEsci.addActionListener(e -> {
            client.inviaComando("esci");
            btnEsci.setEnabled(false); // Evita doppi click
            lblMessaggioServer.setText("Uscita dal tavolo in corso...");
        });
        
        panelExit.add(btnEsci);

        // Aggiungiamo il tutto al container principale in basso
        panelSouthContainer.add(panelComandi, BorderLayout.CENTER);
        panelSouthContainer.add(panelExit, BorderLayout.EAST);

        add(panelSouthContainer, BorderLayout.SOUTH);
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
        
        if (state.getSecondiAttesa() > 0) {
            lblMessaggioServer.setText(state.getMessaggioAvviso() +  state.getSecondiAttesa() + "s");
        } else if (state.getMessaggioAvviso() != null) {
            lblMessaggioServer.setText(state.getMessaggioAvviso());
        }

        lblFiches.setText("Fiches: " + state.getFiches());

        panelBanco.removeAll();
        panelGiocatore.removeAll();
        panelAvversari.removeAll();
        
        if (!state.getCarteDealer().isEmpty()) {
            int puntiBanco = state.getPunteggioDealer();
            boolean bancoSballato = (puntiBanco > 21);
            boolean bancoBlackjack = (puntiBanco == 21 && state.getCarteDealer().size() == 2 && state.isFinePartita());
            
            String testoBanco = " (Punti: " + puntiBanco + ") ";
            if (bancoSballato) testoBanco = " (SBALLATO) ";
            else if (bancoBlackjack) testoBanco = " (BLACKJACK!) ";
            
            JLabel lblPuntiBanco = new JLabel(testoBanco);
            lblPuntiBanco.setForeground(bancoSballato ? new Color(255, 80, 80) : (bancoBlackjack ? new Color(50, 255, 50) : Color.WHITE));
            lblPuntiBanco.setFont(new Font("Arial", Font.BOLD, 16));
            panelBanco.add(lblPuntiBanco);
            
            for (String nomeCarta : state.getCarteDealer()) panelBanco.add(creaLabelCarta(nomeCarta));
        }

        List<List<String>> mani = state.getManiGiocatore();
        for (int i = 0; i < mani.size(); i++) {
            JPanel singolaMano = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            singolaMano.setOpaque(false);
            
            int punti = state.getPunteggiMani().get(i);
            boolean isSballato = (punti > 21);
            boolean isBlackjack = (punti == 21 && mani.get(i).size() == 2);

            String testoPunti = "Mano " + (i+1) + " (Punti: " + punti + ") ";
            if (isSballato) testoPunti = "Mano " + (i+1) + " (SBALLATO) ";
            else if (isBlackjack) testoPunti = "Mano " + (i+1) + " (BLACKJACK!) ";

            Color coloreTesto;
            if (isSballato) coloreTesto = new Color(255, 80, 80);
            else if (isBlackjack) coloreTesto = new Color(50, 255, 50);
            else coloreTesto = Color.WHITE;

            JLabel lblPunti = new JLabel(testoPunti);
            lblPunti.setForeground(coloreTesto);
            lblPunti.setFont(new Font("Arial", Font.BOLD, 15));
            singolaMano.add(lblPunti);

            for (String nomeCarta : mani.get(i)) singolaMano.add(creaLabelCarta(nomeCarta));
            
            int scommessa = state.getScommesseMani().get(i);
            if (scommessa > 0) {
                singolaMano.add(creaPannelloFiches(scommessa, false));
            }

            panelGiocatore.add(singolaMano);
        }

        int sedieOccupate = 0;
        if (state.getAltriGiocatori() != null) {
            for (Map.Entry<String, List<List<String>>> entry : state.getAltriGiocatori().entrySet()) {
                if (sedieOccupate >= 3) break; 

                String nick = entry.getKey();
                List<List<String>> maniAvversario = entry.getValue();

                List<Integer> scommesseAvv = null;
                if (state.getScommesseAvversari() != null) {
                    scommesseAvv = state.getScommesseAvversari().get(nick);
                }

             // ==========================================
                // IL NUOVO SLOT DELL'AVVERSARIO (Ordinato!)
                // ==========================================
                JPanel slotGiocatore = new JPanel();
                // Usa un BoxLayout verticale per impilare le mani (es. Split) una sotto l'altra
                slotGiocatore.setLayout(new BoxLayout(slotGiocatore, BoxLayout.Y_AXIS)); 
                slotGiocatore.setOpaque(false);
                slotGiocatore.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), nick, 0, 0, null, Color.ORANGE));

                if (maniAvversario.isEmpty()) {
                    JLabel lblAttesa = new JLabel("In attesa...");
                    lblAttesa.setForeground(Color.LIGHT_GRAY);
                    lblAttesa.setAlignmentX(Component.CENTER_ALIGNMENT);
                    slotGiocatore.add(lblAttesa);
                } else {
                    for (int m = 0; m < maniAvversario.size(); m++) {
                        List<String> mano = maniAvversario.get(m);

                     // IL TRUCCO DEL VENTAGLIO: Distanza negativa (-20)
                        int cardGap = -20; 
                        JPanel panelMano = new JPanel(new FlowLayout(FlowLayout.CENTER, cardGap, 5)) {
                            @Override
                            public Dimension getPreferredSize() {
                                Dimension d = super.getPreferredSize();
                                d.width += Math.abs(cardGap); 
                                return d;
                            }
                        };
                        panelMano.setOpaque(false);
                        panelMano.setAlignmentX(Component.CENTER_ALIGNMENT);

                        // ==========================================
                        // LA MAGIA DEGLI ANGOLI: Invertiamo la logica!
                        // ==========================================
                        panelMano.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

                        // 1. Aggiungiamo PRIMA le fiches (che grazie al trucco finiranno all'estrema DESTRA)
                        if (scommesseAvv != null && scommesseAvv.size() > m) {
                            int puntata = scommesseAvv.get(m);
                            if (puntata > 0) {
                                JPanel pnlFiches = creaPannelloFiches(puntata, true);
                                // Diamo 40 pixel di margine per compensare il gap negativo e staccarle dalle carte
                                pnlFiches.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0)); 
                                panelMano.add(pnlFiches);
                            }
                        }

                        // 2. Aggiungiamo le carte AL CONTRARIO (dall'ultima alla prima)
                        // Così si dispongono visivamente da destra verso sinistra, sormontandosi nel verso giusto!
                        for (int c = mano.size() - 1; c >= 0; c--) {
                            panelMano.add(creaLabelCartaPiccola(mano.get(c))); 
                        }
                        
                        slotGiocatore.add(panelMano);
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

        gestisciBottoni(state);

        panelBanco.revalidate();
        panelBanco.repaint();
        panelAvversari.revalidate();
        panelAvversari.repaint();
        panelGiocatore.revalidate();
        panelGiocatore.repaint();
        
     // ==========================================
        // POPUP DI BANCAROTTA!
        // ==========================================
        if (state.getFiches() <= 0 && state.getMessaggioAvviso() != null && state.getMessaggioAvviso().contains("BANCAROTTA")) {
            // Mostra un Alert bloccante a schermo
            JOptionPane.showMessageDialog(this, 
                "Hai esaurito tutte le fiches!\nLa tua partita finisce qui. Grazie per aver giocato.", 
                "💸 Bancarotta!", 
                JOptionPane.ERROR_MESSAGE); // Mostra l'icona rossa di errore
            
            // Appena l'utente clicca OK, manda il segnale segreto al Server per farsi disconnettere
            client.inviaComando("capito_bancarotta");
        }
        
    }

    private void gestisciBottoni(GameState state) {
        GameState.FaseGioco fase = state.getFaseAttuale();
        boolean isScommessa = (fase == GameState.FaseGioco.SCOMMESSA);
        boolean isTurno = (fase == GameState.FaseGioco.TURNO_GIOCATORE);
        boolean isAssicurazione = (fase == GameState.FaseGioco.ASSICURAZIONE);
        boolean isFineMano = (fase == GameState.FaseGioco.FINE_MANO);
        
        if (isScommessa) {
            lblTestoComando.setText("Inserisci Puntata: ");
        } else if (isTurno) {
            lblTestoComando.setText("Qual è la tua mossa? ");
        } else if (isAssicurazione) {
            lblTestoComando.setText("Assicurazione? ");
        } else {
            lblTestoComando.setText(""); 
        }

        panelFichesScommessa.setVisible(isScommessa);
        lblPuntataAttuale.setVisible(isScommessa);
        btnSvuotaPuntata.setVisible(isScommessa);
        btnScommetti.setVisible(isScommessa);
        
        // Li rendiamo sempre visibili durante il tuo turno...
        btnCarta.setVisible(isTurno);
        btnSto.setVisible(isTurno);
        btnRaddoppio.setVisible(isTurno);
        btnSplit.setVisible(isTurno);
        
        // ==========================================
        // LA MAGIA DEI TASTI INTELLIGENTI
        // ==========================================
        if (isTurno) {
            // Carta e Sto si possono fare sempre
            btnCarta.setEnabled(true);
            btnSto.setEnabled(true);
            
            // Leggiamo la frase che ci manda il Croupier (il Server)
            String opzioniServer = state.getMessaggioAvviso() != null ? state.getMessaggioAvviso().toLowerCase() : "";
            
            // Se il Server ci ha concesso le mosse speciali, attiviamo i tasti, altrimenti restano grigi!
            btnRaddoppio.setEnabled(opzioniServer.contains("raddoppio"));
            btnSplit.setEnabled(opzioniServer.contains("split"));
        }
        
        btnSi.setVisible(isAssicurazione);
        btnNo.setVisible(isAssicurazione);
        
        btnEsci.setEnabled(isFineMano); 
        
        panelComandi.getParent().revalidate();
        panelComandi.getParent().repaint();
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
    
    // pannello per visualizzazione fiches sovrapposte
    private JPanel creaPannelloFiches(int totale, boolean piccole) {
        
        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        container.setOpaque(false);

        int gap = piccole ? -15 : -25;
        JPanel panelStack = new JPanel(new FlowLayout(FlowLayout.CENTER, gap, 0)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width += Math.abs(gap); 
                return d;
            }
        };
        panelStack.setOpaque(false);

        int resto = totale;
        
        int[] valori =   {500, 250, 100, 50};
        String[] files = {"chipPurple.png", "chipGreen.png", "chipBlue.png", "chipBlack.png"};

        int size = piccole ? 30 : 50;

        for (int i = 0; i < valori.length; i++) {
            while (resto >= valori[i]) {
                resto -= valori[i];
                JLabel lblFiche = new JLabel();
                ImageIcon icona = new ImageIcon("images/" + files[i]);
                if (icona.getIconWidth() != -1) {
                    Image img = icona.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    lblFiche.setIcon(new ImageIcon(img));
                }
                panelStack.add(lblFiche);
            }
        }

        JLabel lblTesto = new JLabel(totale + "$");
        lblTesto.setForeground(new Color(255, 215, 0));
        lblTesto.setFont(new Font("Arial", Font.BOLD, piccole ? 12 : 18));
        lblTesto.setBorder(BorderFactory.createEmptyBorder(0, Math.abs(gap) + 5, 0, 0));

        container.add(panelStack);
        container.add(lblTesto);

        return container;
    }
    
    private JButton creaBottoneFiche(String nomeFile, int valore) {
        JButton btn = new JButton();
        ImageIcon icona = new ImageIcon("images/" + nomeFile);
        
        if (icona.getIconWidth() != -1) {
            Image img = icona.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
            btn.setPreferredSize(new Dimension(50, 50));
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            btn.setText(valore + "$");
        }
        btn.setToolTipText("Aggiungi " + valore + "$ alla puntata");
        return btn;
    }

    public static void main(String[] args) {
        // ==========================================
        // TRUCCO 1: ATTIVIAMO IL TEMA GRAFICO MODERNO!
        // ==========================================
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Se per qualche motivo fallisce, userà il tema di base
        }

        SwingUtilities.invokeLater(() -> avviaSchermataDiLogin());
    }

    private static void avviaSchermataDiLogin() {
        JFrame loginFrame = new JFrame("Login Casinò");
        // Finestra leggermente più alta per farci stare l'IP
        loginFrame.setSize(350, 220); 
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        loginFrame.setLocationRelativeTo(null); 
        loginFrame.setResizable(false);

        // 1. Campo Nickname
        JLabel lblInfo = new JLabel("Inserisci il tuo Nickname per giocare:");
        lblInfo.setFont(new Font("Arial", Font.BOLD, 14));
        JTextField txtNick = new JTextField(15);
        txtNick.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // 2. Campo Indirizzo IP
        JLabel lblIp = new JLabel("Indirizzo IP del Banco:");
        lblIp.setFont(new Font("Arial", Font.BOLD, 14));
        JTextField txtIp = new JTextField("localhost", 15); // Precompilato per i test in locale
        txtIp.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // 3. Bottone
        JButton btnEntra = new JButton("Entra al Tavolo");
        btnEntra.setFont(new Font("Arial", Font.BOLD, 14));
        btnEntra.setBackground(new Color(0, 153, 51)); 
        btnEntra.setForeground(Color.WHITE);

        btnEntra.addActionListener(e -> {
            String nick = txtNick.getText().trim();
            if (nick.isEmpty()) {
                nick = "Giocatore_" + (int)(Math.random() * 1000);
            }
            
            String ip = txtIp.getText().trim();
            if (ip.isEmpty()) {
                ip = "localhost"; // Sicurezza: se lascia vuoto, prova in locale
            }
            
            loginFrame.dispose(); 
            // Passa sia il nickname che l'IP al gioco vero e proprio!
            new BlackjackGUI(nick, ip); 
        });

        loginFrame.getRootPane().setDefaultButton(btnEntra);

        loginFrame.add(lblInfo);
        loginFrame.add(txtNick);
        loginFrame.add(lblIp);
        loginFrame.add(txtIp);
        loginFrame.add(btnEntra);
        loginFrame.setVisible(true);
    }
}