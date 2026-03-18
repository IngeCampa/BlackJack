package it.unibs.pajc;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlackjackGUI extends JFrame implements GameUpdateListener {

	private static final String FONT_GIOCO = "Georgia";
    private Client client; 
    private GameState statoAttuale;
    private Map<String, Image> cacheFichesPiccole = new HashMap<>();
    private Map<String, Image> cacheFichesGrandi = new HashMap<>();

    // Componenti dell'Interfaccia
    private JLabel lblMessaggioServer;
    private JLabel lblFiches;
    private JPanel panelBanco;
    private JPanel panelGiocatore;
    private JPanel panelAvversari;
    private JPanel panelTimer;
    private JPanel panelFichesScommessa;
    private JPanel panelComandi;
    
    private JLabel lblTimer;
    private JLabel lblPuntataAttuale;
    private JLabel lblTestoComando; 
    
    private JButton btnCarta, btnSto, btnRaddoppio, btnSplit;
    private JButton btnSi, btnNo;
    private JButton btnScommetti;
    private JButton btnEsci;
    private JButton btnAllIn;
    private JButton btnSvuotaPuntata;

    private final int LARGHEZZA_CARTA = 100; 
    private final int ALTEZZA_CARTA = 145;
    
    private int puntataAttuale = 0;
    private int timerSecondi = 0;

    // Costruttore: riceve il nickname direttamente dalla schermata di Login
    public BlackjackGUI(String nickname, String ipAddress) {
        
    	this.setUndecorated(true); // rimuove barra windows
    	
    	try {
    	    // Carica l'immagine dal tuo folder images
    	    ImageIcon imgIcon = new ImageIcon("images/A_di_Picche.png"); 
    	    // Imposta l'icona della finestra (quella che appare nella barra delle applicazioni)
    	    this.setIconImage(imgIcon.getImage());
    	} catch (Exception e) {
    	    System.out.println("Impossibile caricare l'icona del gioco.");
    	}
    	
        setSize(1000, 800); 
        setResizable(false); // Blocca la dimensione della finestra
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Centra la finestra

        client = new Client(this); 
        client.setNickname(nickname);
        
        inizializzaInterfaccia();
        
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
        // ==========================================
        // LA NUOVA BARRA SUPERIORE (Fiches - Messaggio - Timer)
        // ==========================================
        JPanel panelInfo = new JPanel(new BorderLayout());
        panelInfo.setBackground(new Color(34, 40, 49));
        // Questo colora lo sfondo principale della finestra, così niente "grigio" sotto il tavolo!
        this.getContentPane().setBackground(new Color(0, 60, 0));
        panelInfo.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Spazio ai lati
        
        // spostare window
        MouseAdapter trascinamentoWindow = new MouseAdapter() {
            private Point clickIniziale;

            @Override
            public void mousePressed(MouseEvent e) {
                clickIniziale = e.getPoint(); // Salva dove hai cliccato
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Calcola lo spostamento e muove l'intera finestra (BlackjackGUI.this)
                int dragX = e.getXOnScreen();
                int dragY = e.getYOnScreen();
                BlackjackGUI.this.setLocation(dragX - clickIniziale.x, dragY - clickIniziale.y);
            }
        };
        panelInfo.addMouseListener(trascinamentoWindow);
        panelInfo.addMouseMotionListener(trascinamentoWindow);
        
     // 1. A SINISTRA: Nome Giocatore + Fiches
        JPanel panelSinistra = new JPanel(new GridLayout(2, 1)); // Crea una colonnina con 2 posti
        panelSinistra.setOpaque(false);

        String nomeGiocatore = client.getNickname(); 
        JLabel lblNomeGiocatore = new JLabel("Giocatore: " + nomeGiocatore);
        lblNomeGiocatore.setForeground(Color.LIGHT_GRAY); 
        lblNomeGiocatore.setFont(new Font(FONT_GIOCO, Font.ITALIC, 14));

        lblFiches = new JLabel("Fiches: ---");
        lblFiches.setForeground(new Color(255, 215, 0)); 
        lblFiches.setFont(new Font(FONT_GIOCO, Font.BOLD, 20));

        panelSinistra.add(lblNomeGiocatore);
        panelSinistra.add(lblFiches);

        // 2. AL CENTRO: Il tabellone messaggi pulito
        lblMessaggioServer = new JLabel("Connessione in corso...", SwingConstants.CENTER);
        lblMessaggioServer.setForeground(Color.WHITE);
        lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 26));
        
        // 3. A DESTRA: Il fantastico Timer Animato!
        panelTimer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (timerSecondi > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Sfondo del cerchio (Grigio scuro)
                    g2.setStroke(new BasicStroke(4));
                    g2.setColor(new Color(60, 60, 60));
                    g2.drawOval(5, 5, 50, 50);
                    
                    // Anello animato (Oro di base, Rosso se < 5 secondi)
                    g2.setColor(timerSecondi <= 5 ? new Color(255, 50, 50) : new Color(255, 191, 0));
                    
                    // Calcola l'angolo (Assumiamo un turno di 15 secondi per il calcolo visivo)
                    int maxSecondi = 15; 
                    int angolo = (int) (((double) timerSecondi / maxSecondi) * 360);
                    if (angolo > 360) angolo = 360; // Evita sbavature grafiche
                    
                    // Disegna l'arco partendo da ore 12 (90 gradi)
                    g2.drawArc(5, 5, 50, 50, 90, angolo);
                }
            }
        };
        panelTimer.setPreferredSize(new Dimension(60, 60));
        panelTimer.setOpaque(false);
        panelTimer.setLayout(new BorderLayout());
        
        // Il numero dentro il cerchio
        lblTimer = new JLabel("", SwingConstants.CENTER);
        lblTimer.setFont(new Font(FONT_GIOCO, Font.BOLD, 22));
        lblTimer.setForeground(Color.WHITE);
        panelTimer.add(lblTimer, BorderLayout.CENTER);
        panelTimer.setVisible(false);

        // Assembliamo la barra alta
        panelInfo.add(panelSinistra, BorderLayout.WEST);
        panelInfo.add(lblMessaggioServer, BorderLayout.CENTER);
        panelInfo.add(panelTimer, BorderLayout.EAST);
        
        add(panelInfo, BorderLayout.NORTH);

        // ==========================================
        // IL TAVOLO VERDE
        // ==========================================
        JPanel tavoloVerde = new JPanel(new BorderLayout()) {
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
     // Creiamo il bordo per il Banco
        TitledBorder bordoBanco = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "BANCO");
        bordoBanco.setTitleFont(new Font(FONT_GIOCO, Font.BOLD, 14)); // APPLICA IL TUO FONT!
        bordoBanco.setTitleColor(Color.WHITE);
        
        panelBanco.setBorder(bordoBanco);
        panelAvversari = new JPanel(new GridLayout(1, 3, 10, 0)); 
        panelAvversari.setOpaque(false);
        panelAvversari.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "AVVERSARI AL TAVOLO", 0, 0, null, Color.LIGHT_GRAY));

        panelGiocatore = new JPanel(new FlowLayout());
        panelGiocatore.setOpaque(false);
        panelGiocatore.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "LE TUE MANI", 0, 0, null, Color.WHITE));

     // ==========================================
        // ASSEMBLAGGIO DINAMICO DEL TAVOLO
        // ==========================================
        JPanel topTavolo = new JPanel();
        topTavolo.setLayout(new BoxLayout(topTavolo, BoxLayout.Y_AXIS)); // Niente più spazi vuoti forzati!
        topTavolo.setOpaque(false);
        topTavolo.add(panelBanco);
        topTavolo.add(panelAvversari);

        tavoloVerde.add(topTavolo, BorderLayout.NORTH);
        tavoloVerde.add(panelGiocatore, BorderLayout.CENTER); 
        
        add(tavoloVerde, BorderLayout.CENTER);
        
        // ==========================================
        // PANNELLO COMANDI (SUD)
        // ==========================================
        JPanel panelSouthContainer = new JPanel(new BorderLayout());
        panelSouthContainer.setBackground(Color.LIGHT_GRAY);

        panelComandi = new JPanel(new FlowLayout());
        panelComandi.setOpaque(false);

        // --- BOTTONI AZIONE DI GIOCO ---
        btnCarta = new JButton("Carta");
        btnSto = new JButton("Sto");
        btnRaddoppio = new JButton("Raddoppio");
        btnSplit = new JButton("Split");
        btnSi = new JButton("Sì");
        btnNo = new JButton("No");

        btnCarta.addActionListener(e -> client.inviaComando("carta"));
        btnSto.addActionListener(e -> client.inviaComando("sto"));
        btnRaddoppio.addActionListener(e -> client.inviaComando("raddoppio"));
        btnSplit.addActionListener(e -> client.inviaComando("split"));
        btnSi.addActionListener(e -> client.inviaComando("si"));
        btnNo.addActionListener(e -> client.inviaComando("no"));
        
        lblTestoComando = new JLabel("Scommessa:");
        lblTestoComando.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        
        // --- BOTTONI SCOMMESSA ---
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
        lblPuntataAttuale.setFont(new Font(FONT_GIOCO, Font.BOLD, 16));
        lblPuntataAttuale.setForeground(new Color(0, 100, 0)); 

        btnSvuotaPuntata = new JButton("Svuota");
        btnSvuotaPuntata.addActionListener(e -> {
            puntataAttuale = 0;
            lblPuntataAttuale.setText("Totale: 0$");
        });

        btnAllIn = new JButton("All-In");
        btnAllIn.setBackground(new Color(150, 0, 0)); 
        btnAllIn.setForeground(Color.WHITE);
        btnAllIn.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnAllIn.addActionListener(e -> {
            if (statoAttuale != null && statoAttuale.getFiches() > 0) {
                puntataAttuale = statoAttuale.getFiches(); 
                lblPuntataAttuale.setText("Totale: " + puntataAttuale + "$");
            }
        });

        btnScommetti = new JButton("Conferma Puntata");
        btnScommetti.setBackground(new Color(204, 153, 0)); 
        btnScommetti.setForeground(Color.WHITE);
        btnScommetti.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnScommetti.addActionListener(e -> {
            if (statoAttuale != null && puntataAttuale > statoAttuale.getFiches()) {
                JOptionPane.showMessageDialog(BlackjackGUI.this, 
                    "Fondi insufficienti!\nStai cercando di puntare " + puntataAttuale + "$, ma hai solo " + statoAttuale.getFiches() + "$.", 
                    "Errore Puntata", 
                    JOptionPane.WARNING_MESSAGE);
                puntataAttuale = 0;
                lblPuntataAttuale.setText("Totale: 0$");
                return; 
            }
            if (puntataAttuale > 0) {
                client.inviaComando(String.valueOf(puntataAttuale));
                puntataAttuale = 0; 
                lblPuntataAttuale.setText("Totale: 0$");
            } else {
                JOptionPane.showMessageDialog(BlackjackGUI.this, 
                    "Devi inserire almeno una fiche sul tavolo per poter giocare!", 
                    "Puntata Vuota", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // --- IMPOSTA TUTTO COME INVISIBILE DI DEFAULT ---
        btnCarta.setVisible(false);
        btnSto.setVisible(false);
        btnRaddoppio.setVisible(false);
        btnSplit.setVisible(false);
        btnSi.setVisible(false);
        btnNo.setVisible(false);
        panelFichesScommessa.setVisible(false);
        lblPuntataAttuale.setVisible(false);
        btnSvuotaPuntata.setVisible(false);
        btnAllIn.setVisible(false);
        btnScommetti.setVisible(false);

        // ==========================================
        // ASSEMBLAGGIO FINALE DEL PANNELLO COMANDI
        // (L'ordine in cui appaiono da sinistra a destra!)
        // ==========================================
        panelComandi.add(lblTestoComando);
        panelComandi.add(panelFichesScommessa); 
        panelComandi.add(lblPuntataAttuale);
        panelComandi.add(btnSvuotaPuntata);
        panelComandi.add(btnAllIn);  
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
        btnEsci.setBackground(new Color(180, 0, 0)); 
        btnEsci.setForeground(Color.WHITE);
        btnEsci.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnEsci.setEnabled(false); 
        
        btnEsci.addActionListener(e -> {
            client.inviaComando("esci");
            btnEsci.setEnabled(false); 
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
        
        // Aggiorna il testo centrale filtrando i messaggi "tecnici" del Server
     // ==========================================
        // GESTIONE MESSAGGI ED ESITO FINALE GIGANTE!
        // ==========================================
        if (state.isFinePartita() && !state.getManiGiocatore().isEmpty()) {
            // LA MANO È FINITA! Calcoliamo l'esito della tua prima mano
            int puntiBanco = state.getPunteggioDealer();
            int puntiMano = state.getPunteggiMani().get(0);
            boolean bancoSballato = (puntiBanco > 21);
            boolean giocatoreSballato = (puntiMano > 21);
            boolean giocatoreBlackjack = (puntiMano == 21 && state.getManiGiocatore().get(0).size() == 2);
            boolean bancoBlackjack = (puntiBanco == 21 && state.getCarteDealer().size() == 2);

            String testoEsito = "";
            Color coloreEsito = Color.WHITE;

            // Decidiamo cosa scrivere e di che colore
            if (giocatoreSballato) {
                testoEsito = "SBALLATO! HAI PERSO!";
                coloreEsito = new Color(255, 80, 80); // Rosso acceso
            } else if (giocatoreBlackjack && !bancoBlackjack) {
                testoEsito = "BLACKJACK! HAI VINTO!";
                coloreEsito = new Color(255, 215, 0); // Oro scintillante
            } else if (bancoSballato || puntiMano > puntiBanco) {
                testoEsito = "HAI VINTO!";
                coloreEsito = new Color(50, 255, 50); // Verde fluo
            } else if (puntiMano == puntiBanco) {
                testoEsito = "PAREGGIO!";
                coloreEsito = Color.LIGHT_GRAY; // Grigio neutro
            } else {
                testoEsito = "IL BANCO VINCE!";
                coloreEsito = new Color(255, 80, 80); // Rosso acceso
            }

            // Applichiamo il testo con un FONT GIGANTE!
            lblMessaggioServer.setText(testoEsito);
            lblMessaggioServer.setForeground(coloreEsito);
            lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 38)); // Molto più grande del normale!

        } else if (state.getMessaggioAvviso() != null) {
            // LA PARTITA È IN CORSO: Stile normale
            String messaggioPulito = state.getMessaggioAvviso();
            
            if (messaggioPulito.contains("Mossa (Mano")) {
                if (messaggioPulito.contains("Mano 1")) messaggioPulito = "È il tuo turno (Mano 1):";
                else if (messaggioPulito.contains("Mano 2")) messaggioPulito = "È il tuo turno (Mano 2):";
                else messaggioPulito = "È il tuo turno: scegli la tua mossa";
            }
            
            lblMessaggioServer.setText(messaggioPulito);
            lblMessaggioServer.setForeground(Color.WHITE); // Torna bianco
            lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 22)); // Torna alla grandezza normale
        } else {
            lblMessaggioServer.setText("");
        }

        // ==========================================
        // AGGIORNA IL TIMER GRAFICO
        // ==========================================
        timerSecondi = state.getSecondiAttesa();
        if (timerSecondi > 0) {
            lblTimer.setText(String.valueOf(timerSecondi)); // Scrive il numero
            lblTimer.setForeground(timerSecondi <= 5 ? new Color(255, 50, 50) : Color.WHITE); // Il numero diventa rosso alla fine
            panelTimer.setVisible(true);
            panelTimer.repaint(); // Fa girare l'animazione dell'anello
        } else {
            panelTimer.setVisible(false); // Spegne l'orologio se non c'è attesa
        }

        lblFiches.setText("Fiches: " + state.getFiches());

        panelBanco.removeAll();
        panelGiocatore.removeAll();
        panelAvversari.removeAll();
        
      //ridimensionamento delle mani (SPLIT)
        
        List<List<String>> mani = state.getManiGiocatore();
        double scaleFactor = 1.0; 
        
        if (mani.size() == 2) {
            scaleFactor = 0.80; // Rimpicciolisce all'80%
        } else if (mani.size() == 3) {
            scaleFactor = 0.65; // Rimpicciolisce al 65%
        } else if (mani.size() >= 4) {
            scaleFactor = 0.50; // Rimpicciolisce al 50%
        }
        
        // IL BANCO (Usa la scala normale 1.0 per non rimpicciolire il banco se TU fai split)
        if (!state.getCarteDealer().isEmpty()) {
            int puntiBanco = state.getPunteggioDealer();
            boolean bancoSballato = (puntiBanco > 21);
            boolean bancoBlackjack = (puntiBanco == 21 && state.getCarteDealer().size() == 2 && state.isFinePartita());
            
            String testoBanco = " (Punti: " + puntiBanco + ") ";
            if (bancoSballato) testoBanco = " (SBALLATO) ";
            else if (bancoBlackjack) testoBanco = " (BLACKJACK!) ";
            
            JLabel lblPuntiBanco = new JLabel(testoBanco);
            lblPuntiBanco.setForeground(bancoSballato ? new Color(255, 80, 80) : (bancoBlackjack ? new Color(50, 255, 50) : Color.WHITE));
            lblPuntiBanco.setFont(new Font(FONT_GIOCO, Font.BOLD, 16));
            panelBanco.add(lblPuntiBanco);
            
            for (String nomeCarta : state.getCarteDealer()) {
                // Passiamo 1.0 come fattore di scala al banco (non si rimpicciolisce mai)
                panelBanco.add(creaLabelCarta(nomeCarta, 1.0)); 
            }
        }

        // IL GIOCATORE (Usa lo scaleFactor calcolato)
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
            // Anche il testo si rimpicciolisce un po' se fai tanti split!
            lblPunti.setFont(new Font(FONT_GIOCO, Font.BOLD, (int)(15 * scaleFactor))); 
            singolaMano.add(lblPunti);

            for (String nomeCarta : mani.get(i)) {
                // Passiamo lo scaleFactor al metodo che disegna la carta
                singolaMano.add(creaLabelCarta(nomeCarta, scaleFactor)); 
            }
            
            int scommessa = state.getScommesseMani().get(i);
            if (scommessa > 0) {
                // Se hai tante mani, rimpiccioliamo anche le fiches usando il parametro 'piccole' = true
                singolaMano.add(creaPannelloFiches(scommessa, scaleFactor < 1.0));
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
                        for (int c = mano.size() - 1; c >= 0; c--) {
                            // Gli avversari usano già il metodo 'Piccola', quindi non serve la scala dinamica
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
        btnAllIn.setVisible(isScommessa);
        
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

    private JLabel creaLabelCarta(String nomeCarta, double scaleFactor) {
        JLabel carta = new JLabel("", SwingConstants.CENTER);
        String path = nomeCarta.equals("[CARTA COPERTA]") ? "images/retro.png" : "images/" + nomeCarta.replace(" ", "_") + ".png";
        
        // ==========================================
        // CALCOLO DELLO ZOOM (Scala le tue costanti originali)
        // ==========================================
        int larghezzaScalata = (int) (LARGHEZZA_CARTA * scaleFactor);
        int altezzaScalata = (int) (ALTEZZA_CARTA * scaleFactor);
        
        ImageIcon iconaOriginale = new ImageIcon(path);
        Dimension dimensioniFisse = new Dimension(larghezzaScalata, altezzaScalata);

        // Imposta i limiti della JLabel usando le nuove dimensioni rimpicciolite
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
            // Rimpicciolisce fisicamente l'immagine
            Image nuovaImg = img.getScaledInstance(larghezzaScalata, altezzaScalata, Image.SCALE_SMOOTH);
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
        // Contenitore principale (Fiches a sinistra, Testo a destra)
        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        container.setOpaque(false);

        int size = piccole ? 30 : 50;
     // 8 pixel di spazio per le piccole, 12 o 14 pixel per le grandi!
        int yOffset = piccole ? 8 : 12;

        // 1. Calcoliamo quante fiches servono
        int resto = totale;
        int[] valori = {500, 250, 100, 50};
        String[] files = {"chipPurple.png", "chipGreen.png", "chipBlue.png", "chipBlack.png"};

        // Salviamo le immagini pronte da disegnare
        java.util.List<Image> fichesDaDisegnare = new java.util.ArrayList<>();

     // ==========================================
        // CARICAMENTO INTELLIGENTE DALLA CACHE
        // ==========================================
        for (int i = 0; i < valori.length; i++) {
            while (resto >= valori[i]) {
                resto -= valori[i];
                String nomeFile = files[i];
                
                // Scegliamo in quale scatola cercare in base alla grandezza
                java.util.Map<String, Image> cacheGiusta = piccole ? cacheFichesPiccole : cacheFichesGrandi;
                
                // Se l'immagine NON c'è nella memoria, la peschiamo dal disco e la rimpiccioliamo (lo fa 1 volta sola!)
                if (!cacheGiusta.containsKey(nomeFile)) {
                    ImageIcon icona = new ImageIcon("images/" + nomeFile);
                    if (icona.getIconWidth() != -1) {
                        Image img = icona.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        cacheGiusta.put(nomeFile, img); // La salviamo in memoria per il futuro!
                    }
                }
                
                // Ora peschiamo l'immagine direttamente dalla RAM (è istantaneo, zero sfarfallio!)
                if (cacheGiusta.containsKey(nomeFile)) {
                    fichesDaDisegnare.add(cacheGiusta.get(nomeFile));
                }
            }
        }
        
     // ... fine del ciclo for ...

    // ==========================================
    // GESTIONE "SPICCIOLI" (Resto < 50)
    // ==========================================
    // Se avanza un resto che è più piccolo della nostra fiche minima (es. 25$, 30$),
    // stampiamo un'ultima fiche (la più piccola che abbiamo, in questo caso files[3]) per rappresentarlo visivamente.
    if (resto > 0) {
        String nomeFile = files[files.length - 1]; // Prende l'ultima fiche dell'array (chipBlack.png)
        
        java.util.Map<String, Image> cacheGiusta = piccole ? cacheFichesPiccole : cacheFichesGrandi;
        
        if (!cacheGiusta.containsKey(nomeFile)) {
            ImageIcon icona = new ImageIcon("images/" + nomeFile);
            if (icona.getIconWidth() != -1) {
                Image img = icona.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                cacheGiusta.put(nomeFile, img);
            }
        }
        
        if (cacheGiusta.containsKey(nomeFile)) {
            fichesDaDisegnare.add(cacheGiusta.get(nomeFile));
        }
    }

        // 2. Calcoliamo l'altezza TOTALE esatta della pila (non taglierà mai niente!)
        int altezzaPila = fichesDaDisegnare.isEmpty() ? size : size + ((fichesDaDisegnare.size() - 1) * yOffset);

        // 3. LA MAGIA: Il pannello che si dipinge da solo
        JPanel panelStack = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // Attiva l'antialiasing per rendere i bordi delle fiches morbidissimi
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Disegniamo dal basso verso l'alto
                int currentY = altezzaPila - size; // Punto di partenza (la base della pila)
                
                // Disegna le fiches partendo dalla più bassa e salendo verso l'alto
                for (Image img : fichesDaDisegnare) {
                    g2d.drawImage(img, 0, currentY, this);
                    currentY -= yOffset; // Sale un po' per la fiche successiva
                }
            }

            @Override
            public Dimension getPreferredSize() {
                // Comunica a Java la grandezza ESATTA del blocco, impedendo i tagli!
                return new Dimension(size, altezzaPila);
            }
        };
        panelStack.setOpaque(false);

        // 4. Il testo con il totale
        JLabel lblTesto = new JLabel(totale + "$");
        lblTesto.setForeground(new Color(255, 215, 0));
        lblTesto.setFont(new Font(FONT_GIOCO, Font.BOLD, piccole ? 14 : 18));

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
        JFrame loginFrame = new JFrame();
        loginFrame.setSize(800, 700); 
        loginFrame.setUndecorated(true); 
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null); 

        JPanel backgroundPanel = new JPanel(null) { 
            Image bgImage;
            {
                try {
                    java.io.File fileImmagine = new java.io.File("images/sfondo.jpg");
                    if (fileImmagine.exists()) bgImage = javax.imageio.ImageIO.read(fileImmagine);
                } catch (Exception ex) { }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(0, 50, 0));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // Sistema di trascinamento finestra
        java.awt.event.MouseAdapter dragger = new java.awt.event.MouseAdapter() {
            int mouseX, mouseY;
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
            }
            public void mouseDragged(java.awt.event.MouseEvent e) {
                loginFrame.setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY);
            }
        };
        backgroundPanel.addMouseListener(dragger);
        backgroundPanel.addMouseMotionListener(dragger);

        // tasto chiudi
        JButton btnClose = new JButton("X");
        btnClose.setBounds(750, 10, 40, 40); 
        btnClose.setFont(new Font(FONT_GIOCO, Font.BOLD, 20));
        btnClose.setForeground(Color.WHITE);
        btnClose.setBackground(new Color(200, 0, 0));
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder());
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> System.exit(0)); 
        backgroundPanel.add(btnClose);

        // ==========================================
        // UI DA VIDEOGIOCO: LARGHE E SENZA LABEL
        // ==========================================
        Color colorOro = new Color(255, 191, 0); 
        Color colorBgInput = new Color(10, 10, 10, 210); // Nero opaco elegante

        // --- NOME GIOCATORE (Con Placeholder) ---
        JTextField txtNick = new JTextField("INSERISCI NICKNAME");
        txtNick.setBounds(260, 530, 280, 40);
        txtNick.setFont(new Font("Georgia", Font.BOLD, 16));
        txtNick.setForeground(Color.GRAY); // Testo grigio finché non scrivi
        txtNick.setCaretColor(Color.WHITE);
        txtNick.setBackground(colorBgInput);
        txtNick.setOpaque(true);
        txtNick.setBorder(BorderFactory.createLineBorder(colorOro, 2)); // Bordo d'oro!
        txtNick.setHorizontalAlignment(JTextField.CENTER);

        // Effetto a scomparsa quando ci clicchi (Placeholder)
        txtNick.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtNick.getText().equals("INSERISCI NICKNAME")) {
                    txtNick.setText("");
                    txtNick.setForeground(Color.WHITE); // Il tuo testo diventa bianco luminoso
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtNick.getText().trim().isEmpty()) {
                    txtNick.setForeground(Color.GRAY);
                    txtNick.setText("INSERISCI NICKNAME");
                }
            }
        });
        backgroundPanel.add(txtNick);

        // --- INDIRIZZO SERVER (Con Placeholder) ---
        JTextField txtIp = new JTextField("IP SERVER (localhost per Hostare)");
        txtIp.setBounds(260, 580, 280, 40);
        txtIp.setFont(new Font("Georgia", Font.BOLD, 14));
        txtIp.setForeground(Color.GRAY);
        txtIp.setCaretColor(Color.WHITE);
        txtIp.setBackground(colorBgInput);
        txtIp.setOpaque(true);
        txtIp.setBorder(BorderFactory.createLineBorder(colorOro, 2));
        txtIp.setHorizontalAlignment(JTextField.CENTER);

        txtIp.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtIp.getText().equals("IP SERVER (localhost per Hostare)")) {
                    txtIp.setText("localhost");
                    txtIp.setForeground(Color.WHITE);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtIp.getText().trim().isEmpty()) {
                    txtIp.setForeground(Color.GRAY);
                    txtIp.setText("IP SERVER (localhost per Hostare)");
                }
            }
        });
        backgroundPanel.add(txtIp);

        // bottone start
        JButton btnStart = new JButton("START GAME") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 230, 100)); 
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
                g2.dispose();
            }
        };
        
        btnStart.setContentAreaFilled(false);
        btnStart.setBounds(300, 630, 200, 40);
        btnStart.setFont(new Font("Georgia", Font.BOLD, 22));
        btnStart.setBackground(colorOro); 
        btnStart.setForeground(new Color(92, 35, 0)); 
        btnStart.setFocusPainted(false);
        btnStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStart.setBorder(new javax.swing.border.LineBorder(new Color(255, 230, 100), 2, true));
        
        btnStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnStart.setBackground(new Color(255, 230, 100)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnStart.setBackground(colorOro); 
            }
        });

        btnStart.addActionListener(e -> {
            String nick = txtNick.getText().trim();
            if (nick.isEmpty() || nick.equals("INSERISCI NICKNAME")) nick = "Player_" + (int)(Math.random() * 1000); // se il giocatore non inserisce il nickname ne genera uno casuale
            
            String ip = txtIp.getText().trim();
            if (ip.isEmpty() || ip.equals("IP SERVER (localhost per Hostare)")) ip = "localhost"; 
            
            loginFrame.dispose(); 
            gestisciStartAutomatico(nick, ip);
        });
        backgroundPanel.add(btnStart);
        
        // pannello informativo
        JPanel pannelloInfo = new JPanel();
        pannelloInfo.setLayout(new BorderLayout());
        pannelloInfo.setBounds(10, 55, 420, 230);
        pannelloInfo.setBackground(new Color(30, 30, 30, 240));
        pannelloInfo.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        pannelloInfo.setVisible(false);

        JTextArea txtSpiegazione = new JTextArea();
        txtSpiegazione.setText("COME FUNZIONA LA CONNESSIONE?\n\n"
            + "👑 CREA UNA TUA PARTITA (HOST):\n"
            + "Lascia la casella IP su 'localhost'. Il gioco avvierà\n"
            + "il server in automatico e aprirà il tavolo.\n\n"
            + "🤝 UNISCITI A UN AMICO (JOIN):\n"
            + "Vuoi giocare al tavolo creato da un tuo amico?\n"
            + "Cancella 'localhost', scrivi il suo indirizzo IP\n"
            + "(es. 192.168.1.55) e premi START GAME.");
        txtSpiegazione.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtSpiegazione.setForeground(Color.BLACK);
        txtSpiegazione.setOpaque(false);
        txtSpiegazione.setEditable(false);
        txtSpiegazione.setHighlighter(null);
        txtSpiegazione.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        pannelloInfo.add(txtSpiegazione, BorderLayout.CENTER);
        backgroundPanel.add(pannelloInfo);

        // tatso info
        JButton btnInfo = new JButton("?");
        btnInfo.setBounds(10, 10, 40, 40); 
        btnInfo.setFont(new Font("Georgia", Font.BOLD, 22));
        btnInfo.setForeground(Color.WHITE); 
        btnInfo.setBackground(new Color(70, 70, 70)); 
        btnInfo.setFocusPainted(false);
        btnInfo.setBorder(BorderFactory.createEmptyBorder());
        btnInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnInfo.setBackground(new Color(100, 100, 100)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnInfo.setBackground(new Color(70, 70, 70)); 
            }
        });
        
        btnInfo.addActionListener(e -> {
            boolean isNascosto = !pannelloInfo.isVisible();
            pannelloInfo.setVisible(isNascosto);
            
            if (isNascosto) {
                backgroundPanel.setComponentZOrder(pannelloInfo, 0);
                backgroundPanel.repaint();
            }
        });
        
        backgroundPanel.add(btnInfo);
        backgroundPanel.requestFocusInWindow();

        loginFrame.add(backgroundPanel);
        loginFrame.getRootPane().setDefaultButton(btnStart);
        loginFrame.setVisible(true);
    }

    // se l'utente lascia localhost, allora capisce che sta facendo anche da server e avvia automaticamente la classe Server.java
    private static void gestisciStartAutomatico(String nick, String ip) {
        if (ip.equalsIgnoreCase("localhost")) {
            boolean serverGiaAttivo = false;
            try (java.net.Socket controllo = new java.net.Socket("localhost", 12345)) {
                serverGiaAttivo = true;
            } catch (java.io.IOException ex) {
                serverGiaAttivo = false;
            }
            
            if (!serverGiaAttivo) {
                System.out.println("Nessun server locale trovato. Avvio il Server come Host...");
                new Thread(() -> {
                    try {
                        Server.main(new String[]{}); 
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

                try { Thread.sleep(500); } catch (InterruptedException ex) {}
            } else {
                System.out.println("Server locale già attivo! Entro come giocatore...");
            }
            new BlackjackGUI(nick, "localhost"); 
        } else {
            new BlackjackGUI(nick, ip);
        }
    }
}