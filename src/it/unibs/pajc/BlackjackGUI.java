package it.unibs.pajc;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlackjackGUI extends JFrame implements GameUpdateListener {

    private static final String FONT_GIOCO = "Georgia";
    private Client client; 
    private boolean isHost;
    
    private GameState statoAttuale;
    private Map<String, Image> cacheFichesPiccole = new HashMap<>();
    private Map<String, Image> cacheFichesGrandi = new HashMap<>();
    
    private boolean bancarottaMostrata = false;

    // Componenti dell'Interfaccia
    private JLabel lblMessaggioServer;
    private JLabel lblFiches;
    private JPanel panelBanco;
    private JPanel panelGiocatore;
    private JPanel panelAvversari;
    private JPanel panelTimer;
    private JPanel panelFichesScommessa;
    private JPanel panelComandi;
    private JTextArea areaChat;
    private JTextField txtInputChat;
    private JPanel panelChat;
    
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

    //riceve il nickname direttamente dalla schermata di Login
    public BlackjackGUI(String nickname, String ipAddress, boolean isHost) {
        super("Blackjack - Giocatore: " + nickname);
        this.isHost = isHost;
        
        this.setUndecorated(true); // rimuove barra in alto windows
        
        try {
            // Carica l'immagine da images
            ImageIcon imgIcon = new ImageIcon("images/A_di_Picche.png"); 
            // Imposta l'icona della finestra
            this.setIconImage(imgIcon.getImage());
        } catch (Exception e) {
            System.out.println("Impossibile caricare l'icona del gioco.");
        }
        
        setSize(1200, 800);
        setResizable(false); // Blocca la dimensione della finestra

        if (this.isHost) {
            // Se sei l'Host, chiudere la finestra distrugge solo la finestra di gioco
            // Il programma Java rimarrà segretamente acceso per tenere in vita il Server!
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        } else {
            // Se sei un giocatore normale, chiudere la finestra spegne tutto.
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        
        setLayout(new BorderLayout()); //da questo momento in poi dividiamo la finestra in 3 aree
        setLocationRelativeTo(null); // Centra la finestra

        client = new Client(this); 
        client.setNickname(nickname);
        
        //creiamo l'interfaccia senza però ancora farla vedere
        inizializzaInterfaccia();
        
        //client prova a connettersi al server
        boolean connesso = client.connetti(ipAddress, 12345);
        
        //controlla che effettivamente si sia connesso, altrimenti mostra un messaggio d'errore e chiude il programma
        if (!connesso) {
            JOptionPane.showMessageDialog(null, 
                "Impossibile collegarsi alla stanza all'indirizzo: " + ipAddress + "\nAssicurati che il Server sia avviato e l'IP sia corretto!", 
                "Server Offline o Non Trovato", 
                JOptionPane.ERROR_MESSAGE);
                
            System.exit(0); 
        }
        
        //pausa di 0.2 secondi per dare il tempo al client di stabilire la connessione
        try { 
        	Thread.sleep(200); 
        	} catch (InterruptedException ex) {}
       
        //invio il nickname al server
        client.inviaComando(nickname); 
        
        //ora che è tutto pronto mostriamo la finestra al giocatore
        setVisible(true);
    }

    private void inizializzaInterfaccia() {
        
        JPanel panelInfo = new JPanel(new BorderLayout());
        panelInfo.setBackground(new Color(34, 40, 49));
        
        this.getContentPane().setBackground(new Color(0, 60, 0));
     
        //creo dei bordi per distanziare gli elementi dai bordi della finestra
        panelInfo.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
        
        
        /*dato che abbiamo tolto la barra di windows dobbiamo creare un modo per poter trascinare la finestra, 
        e lo facciamo con un MouseAdapter*/
        MouseAdapter trascinamentoWindow = new MouseAdapter() {
            private Point clickIniziale;

            
            @Override
            public void mousePressed(MouseEvent e) {
                clickIniziale = e.getPoint(); // Salva dove ho cliccato
            }

            // Calcola lo spostamento e muove l'intera finestra
            @Override
            public void mouseDragged(MouseEvent e) {
                
                int dragX = e.getXOnScreen();
                int dragY = e.getYOnScreen();
                
                //sposto la finestra tenendo conto però di dove ho premuto all'inizio
                BlackjackGUI.this.setLocation(dragX - clickIniziale.x, dragY - clickIniziale.y);
            }
        };
        //serve per ascoltare il click iniziale
        panelInfo.addMouseListener(trascinamentoWindow);
        //serve per ascoltare il trascinamento del mouse
        panelInfo.addMouseMotionListener(trascinamentoWindow);
        
        JPanel panelSinistra = new JPanel(new GridLayout(2, 1)); // Crea una colonnina con 2 posti
        panelSinistra.setOpaque(false); // Rende il pannello trasparente per far vedere lo sfondo del panel

        String nomeGiocatore = client.getNickname(); 
        JLabel lblNomeGiocatore = new JLabel("Giocatore: " + nomeGiocatore);
        lblNomeGiocatore.setForeground(Color.LIGHT_GRAY); 
        lblNomeGiocatore.setFont(new Font(FONT_GIOCO, Font.ITALIC, 14));

        lblFiches = new JLabel("Fiches: ---");
        lblFiches.setForeground(new Color(255, 215, 0)); 
        lblFiches.setFont(new Font(FONT_GIOCO, Font.BOLD, 20));

        //aggiungo il nome del giocatore e il numero di fiches disponibili al pannello di sinistra
        panelSinistra.add(lblNomeGiocatore);
        panelSinistra.add(lblFiches);

        //label per il messaggio del server al centro
        lblMessaggioServer = new JLabel("Connessione in corso...", SwingConstants.CENTER);
        lblMessaggioServer.setForeground(Color.WHITE);
        lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 26));
        
        //creazione del timer visivo circolare
        panelTimer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);// Pulisce il pannello prima di ridisegnare
                if (timerSecondi > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    
                    //questa riga serve per rendere i bordi del cerchio più liscie
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Sfondo del cerchio
                    g2.setStroke(new BasicStroke(4)); //Spessore del bordo
                    g2.setColor(new Color(60, 60, 60));
                    g2.drawOval(5, 5, 50, 50);
                    
                    // Anello Oro di base, Rosso se < 5 secondi
                    g2.setColor(timerSecondi <= 5 ? new Color(255, 50, 50) : new Color(255, 191, 0));
                    
                    // Calcolo l'angolo dell'arco in base al tempo rimanente
                    int maxSecondi = 15; 
                    int angolo = (int) (((double) timerSecondi / maxSecondi) * 360);
                    if (angolo > 360) angolo = 360; // Evita sbavature grafiche
                    
                    // Disegno l'arco partendo da ore 12 (90 gradi)
                    g2.drawArc(5, 5, 50, 50, 90, angolo);
                }
            }
        };
        panelTimer.setPreferredSize(new Dimension(60, 60)); // Dimensione fissa per il timer
        panelTimer.setOpaque(false);
        panelTimer.setLayout(new BorderLayout());
        
        // Il numero dentro il cerchio che inizialmente è vuoto perchè mi manderà il server i numeri
        lblTimer = new JLabel("", SwingConstants.CENTER);
        lblTimer.setFont(new Font(FONT_GIOCO, Font.BOLD, 22));
        lblTimer.setForeground(Color.WHITE);
        panelTimer.add(lblTimer, BorderLayout.CENTER);
        panelTimer.setVisible(false);

        //aggiungo tutto quello che ho creato alla barra alta
        panelInfo.add(panelSinistra, BorderLayout.WEST);
        panelInfo.add(lblMessaggioServer, BorderLayout.CENTER);
        panelInfo.add(panelTimer, BorderLayout.EAST);
        
        //aggiungo tutto alla finestra
        add(panelInfo, BorderLayout.NORTH);

        //disegnamo il tavolo verde da 0
        JPanel tavoloVerde = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int w = getWidth();
                int h = getHeight();
                
                //usiamo questa classe per creare un colore sfumato come se ci fosse una lampada ad illuminare il tavolo
                RadialGradientPaint paint = new RadialGradientPaint(
                    w / 2f, h / 2f, Math.max(w, h),
                  //parti con il verde splendente e man mano ti allontani dal centro sfuma sempre di più
                    new float[]{0.0f, 0.8f}, 
                    new Color[]{new Color(0, 140, 0), new Color(0, 40, 0)}
                );
                
                g2d.setPaint(paint); //dipingiamo con il colore che abbiamo creato
                g2d.fillRect(0, 0, w, h);
            }
        };
        
        //creiamo il panel per il banco con relativo bordo
        panelBanco = new JPanel(new FlowLayout());
        panelBanco.setOpaque(false);
        
        // Creiamo il bordo per il Banco
        TitledBorder bordoBanco = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "BANCO");
        bordoBanco.setTitleFont(new Font(FONT_GIOCO, Font.BOLD, 14)); 
        bordoBanco.setTitleColor(Color.WHITE);
        panelBanco.setBorder(bordoBanco);
        
        //creiamo il bordo degli avversari
        panelAvversari = new JPanel(new GridLayout(1, 3, 10, 0)); 
        panelAvversari.setOpaque(false);
        panelAvversari.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "AVVERSARI AL TAVOLO", 0, 0, null, Color.LIGHT_GRAY));

        //creiamo il panel del giocatore
        panelGiocatore = new JPanel(new FlowLayout());
        panelGiocatore.setOpaque(false);
        panelGiocatore.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "LE TUE MANI", 0, 0, null, Color.WHITE));

        //creo un panel per inserire banco e avversari nella parte alta del tavolo
        JPanel topTavolo = new JPanel();
        topTavolo.setLayout(new BoxLayout(topTavolo, BoxLayout.Y_AXIS)); //usiamo boxlayout per impilare in verticale i componenti 
        topTavolo.setOpaque(false);
        topTavolo.add(panelBanco);
        topTavolo.add(panelAvversari);

        //aggiungo al tavolo la visuale del banco, degli avversari, e del giocatore principale 
        tavoloVerde.add(topTavolo, BorderLayout.NORTH);
        tavoloVerde.add(panelGiocatore, BorderLayout.CENTER); 
        
        add(tavoloVerde, BorderLayout.CENTER);
        
        //creiamo il pannello che conterrà i comandi che il giocatore potrà eseguire
        JPanel panelSouthContainer = new JPanel(new BorderLayout());
        panelSouthContainer.setBackground(Color.LIGHT_GRAY);
        panelSouthContainer.setPreferredSize(new Dimension(1000, 110));
        
        //pannello dei comandi
        panelComandi = new JPanel(new FlowLayout());
        panelComandi.setOpaque(false);

        //bottoni di gioco
        btnCarta = new JButton("Carta");
        btnSto = new JButton("Sto");
        btnRaddoppio = new JButton("Raddoppio");
        btnSplit = new JButton("Split");
        btnSi = new JButton("Sì");
        btnNo = new JButton("No");

        //inviamo al client ogni comando in base al bottone schiacciato
        btnCarta.addActionListener(e -> client.inviaComando("carta"));
        btnSto.addActionListener(e -> client.inviaComando("sto"));
        btnRaddoppio.addActionListener(e -> client.inviaComando("raddoppio"));
        btnSplit.addActionListener(e -> client.inviaComando("split"));
        btnSi.addActionListener(e -> client.inviaComando("si"));
        btnNo.addActionListener(e -> client.inviaComando("no"));
        
        lblTestoComando = new JLabel("Scommessa:");
        lblTestoComando.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        
        //creiamo il panel per le fiches
        panelFichesScommessa = new JPanel(new FlowLayout());
        panelFichesScommessa.setOpaque(false);

        int[] valoriFiches = {50, 100, 250, 500};
        String[] nomiFiches = {"chipBlack.png", "chipBlue.png", "chipGreen.png", "chipPurple.png"};

        for (int i = 0; i < valoriFiches.length; i++) {
        	/*creo questa variabile perchè le lambda expression richiedono variabili effective final 
        	 quindi non posso mettere direttamente nel creaBottoneFiche() valoriFiches[i]*/
        	int val = valoriFiches[i]; 
            //creo il bottone e ci incollo sopra l'immagine della fiche
        	JButton btnChip = creaBottoneFiche(nomiFiches[i], val);
            btnChip.addActionListener(e -> {
                puntataAttuale += val;
                lblPuntataAttuale.setText("Totale: " + puntataAttuale + "$");
            });
            
            panelFichesScommessa.add(btnChip);
        }
        
        //creo questa label di fianco ai bottoni così da mostrare il totale scommesso
        lblPuntataAttuale = new JLabel("Totale: 0$");
        lblPuntataAttuale.setFont(new Font(FONT_GIOCO, Font.BOLD, 16));
        lblPuntataAttuale.setForeground(new Color(0, 100, 0)); 

        btnSvuotaPuntata = new JButton("Svuota");
        btnSvuotaPuntata.addActionListener(e -> {
            puntataAttuale = 0;
            lblPuntataAttuale.setText("Totale: 0$");
        });

        //creazione bottone per scommettere tutto
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
        
        //creazione del bottone per confermare la puntata
        btnScommetti = new JButton("Conferma Puntata");
        btnScommetti.setBackground(new Color(204, 153, 0)); 
        btnScommetti.setForeground(Color.WHITE);
        btnScommetti.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnScommetti.addActionListener(e -> {
            //qui controllo di avere abbastanza fiches per puntare
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

        //metto tutto invisibile di default
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

        //aggiungo tutto al pannello
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
        
        //creo un panel per il bottone che mi farà uscire
        JPanel panelExit = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        panelExit.setOpaque(false);
        
        JButton btnToggleChat = new JButton("Chat");
        btnToggleChat.setBackground(new Color(70, 130, 180));
        btnToggleChat.setForeground(Color.WHITE);
        btnToggleChat.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        
        btnToggleChat.addActionListener(e -> {
            //se è visibile la nasconde, se è nascosta la mostra!
            panelChat.setVisible(!panelChat.isVisible());
            
            // Ordiniamo alla finestra di "ridisegnarsi" per fare spazio alla chat
            BlackjackGUI.this.revalidate();
            BlackjackGUI.this.repaint();
        });
        
        //bottone per l'uscita dal programma
        btnEsci = new JButton("Esci dal Tavolo");
        btnEsci.setBackground(new Color(180, 0, 0)); 
        btnEsci.setForeground(Color.WHITE);
        btnEsci.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnEsci.setEnabled(false); 
        
        btnEsci.addActionListener(e -> {
            btnEsci.setEnabled(false); 
            lblMessaggioServer.setText("Arrivederci! Uscita in corso...");
            // avvisiamo il server, il server chiuderà la connessione
            client.inviaComando("esci"); 
        });
        
        //bottone per la classifica
        JButton btnClassifica = new JButton("Classifica");
        btnClassifica.setBackground(new Color(255, 215, 0)); // Colore oro!
        btnClassifica.setForeground(Color.BLACK);
        btnClassifica.setFont(new Font(FONT_GIOCO, Font.BOLD, 14));
        btnClassifica.addActionListener(e -> {
            client.inviaComando("CLASSIFICA");
        });
        
        //aggiungiamo al panel che abbiamo creato in basso a destra i tre bottoni
        panelExit.add(btnClassifica);
        panelExit.add(btnToggleChat);
        panelExit.add(btnEsci);

        //aggiungiamo il tutto al container principale in basso
        panelSouthContainer.add(panelComandi, BorderLayout.CENTER);
        panelSouthContainer.add(panelExit, BorderLayout.EAST);
        
        //creazione del pannello a scomparsa della chat
        panelChat = new JPanel(new BorderLayout());
        panelChat.setPreferredSize(new Dimension(280, 0)); //larghezza fissa, altezza adattata alla finestra
        panelChat.setOpaque(false);
        panelChat.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "CHAT TAVOLO", 0, 0, new Font(FONT_GIOCO, Font.BOLD, 14), Color.ORANGE));

        //creazione di una textArea dove ci saranno i messaggi
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true); //crea un muro che impedisce alla frase di finire fuori dallo schermo 
        areaChat.setWrapStyleWord(true); //ci permette di mandare a capo in modo intelligente senza spezzare una parola a metà
        areaChat.setBackground(new Color(20, 20, 20, 200));
        areaChat.setForeground(Color.WHITE);
        areaChat.setFont(new Font("SansSerif", Font.PLAIN, 15));
        areaChat.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //creiamo la barra laterale per scorrere la chat
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setOpaque(false);
        scrollChat.getViewport().setOpaque(false);
        scrollChat.setBorder(BorderFactory.createEmptyBorder());

        //creazione dello spazio per permettere all'utente di scrivere
        txtInputChat = new JTextField();
        txtInputChat.setBackground(new Color(50, 50, 50));
        txtInputChat.setForeground(Color.WHITE);
        txtInputChat.setCaretColor(Color.WHITE); //lineetta lampeggiante classica che ti fa capire che stai scrivendo
        txtInputChat.setFont(new Font("SansSerif", Font.PLAIN, 15));
        txtInputChat.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        //quando premi INVIO sulla tastiera, invia la chat!
        txtInputChat.addActionListener(e -> {
            String txt = txtInputChat.getText().trim();
            if (!txt.isEmpty()) {
                client.inviaComando("CHAT:" + txt); // Il prefisso magico!
                txtInputChat.setText("");
            }
        });

        //aggiungiamo tutto al pannello della chat e alla finestra
        panelChat.add(scrollChat, BorderLayout.CENTER);
        panelChat.add(txtInputChat, BorderLayout.SOUTH);
        panelChat.setVisible(false);
        add(panelChat, BorderLayout.EAST); // Aggiunge la chat a destra del tavolo verde!
        
        add(panelSouthContainer, BorderLayout.SOUTH);
    }

    
    /**
     * creazione di questo metodo per far capire al programma di che messaggio si tratta
     */
    @Override
    public void sulMessaggioDiTesto(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            
            if (messaggio.startsWith("CHATMSG:")) {
                String chatTesto = messaggio.substring(8); //togliamo il prefisso
                areaChat.append(chatTesto + "\n");
                //scrolla in basso in automatico
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
            } 
            else if (messaggio.equals("DISCONNESSIONE") || messaggio.toLowerCase().contains("disconness")) {
                if (this.isHost) {
                    BlackjackGUI.this.dispose(); 
                } else {
                    System.exit(0); 
                }
            } 
            else if (messaggio.startsWith("CLASSIFICA_DATI:")) {
                //fa apparire la classifica
                String dati = messaggio.substring("CLASSIFICA_DATI:".length());
                JOptionPane.showMessageDialog(this, dati, "🏆 Classifica", JOptionPane.INFORMATION_MESSAGE);
            }
            //altrimenti è un normale messaggio del server
            else {
                lblMessaggioServer.setText(messaggio);
            }
        });
    }

    /**
     * metodo per far in modo di aggiornare la grafica non appena EDT riesce
     */
    @Override
    public void onStateUpdate(GameState state) {
        SwingUtilities.invokeLater(() -> aggiornaSchermo(state));
    }

    private void aggiornaSchermo(GameState state) {
        this.statoAttuale = state;
        
        //prendiamo tutti i dati che ci servono per aggiornare lo schermo nel momento in cui finisce la mano
        if (state.isFinePartita() && !state.getManiGiocatore().isEmpty()) {
            int puntiBanco = state.getPunteggioDealer();
            int puntiMano = state.getPunteggiMani().get(0);
            boolean bancoSballato = (puntiBanco > 21);
            boolean giocatoreSballato = (puntiMano > 21);
            boolean giocatoreBlackjack = (puntiMano == 21 && state.getManiGiocatore().get(0).size() == 2);
            boolean bancoBlackjack = (puntiBanco == 21 && state.getCarteDealer().size() == 2);

            String testoEsito = "";
            Color coloreEsito = Color.WHITE;

            //decidiamo cosa scrivere
            if (giocatoreSballato) {
                testoEsito = "SBALLATO! HAI PERSO!";
                coloreEsito = new Color(255, 80, 80); //rosso
            } else if (giocatoreBlackjack && !bancoBlackjack) {
                testoEsito = "BLACKJACK! HAI VINTO!";
                coloreEsito = new Color(255, 215, 0); //giallo
            } else if (bancoSballato || puntiMano > puntiBanco) {
                testoEsito = "HAI VINTO!";
                coloreEsito = new Color(50, 255, 50); //verde
            } else if (puntiMano == puntiBanco) {
                testoEsito = "PAREGGIO!";
                coloreEsito = Color.LIGHT_GRAY; //grigio
            } else {
                testoEsito = "IL BANCO VINCE!";
                coloreEsito = new Color(255, 80, 80); //rosso
            }

            //applichiamo il testo con un font grosso
            lblMessaggioServer.setText(testoEsito);
            lblMessaggioServer.setForeground(coloreEsito);
            lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 38));

        } else if (state.getMessaggioAvviso() != null) {
            //la partita è ancora in corso
            String messaggioPulito = state.getMessaggioAvviso();
            
            if (messaggioPulito.contains("Mossa (Mano")) {
                if (messaggioPulito.contains("Mano 1")) messaggioPulito = "È il tuo turno (Mano 1):";
                else if (messaggioPulito.contains("Mano 2")) messaggioPulito = "È il tuo turno (Mano 2):";
                else messaggioPulito = "È il tuo turno: scegli la tua mossa";
            }
            
            lblMessaggioServer.setText(messaggioPulito);
            lblMessaggioServer.setForeground(Color.WHITE); //torna bianco
            lblMessaggioServer.setFont(new Font(FONT_GIOCO, Font.BOLD, 22)); //grandezza normale
        } else {
            lblMessaggioServer.setText("");
        }

        //aggiorniamo il timer grafico
        timerSecondi = state.getSecondiAttesa();
        if (timerSecondi > 0) {
            lblTimer.setText(String.valueOf(timerSecondi)); //scrive il numero
            lblTimer.setForeground(timerSecondi <= 5 ? new Color(255, 50, 50) : Color.WHITE);
            panelTimer.setVisible(true);
            panelTimer.repaint(); //fa girare l'animazione dell'anello
        } else {
            panelTimer.setVisible(false); //spegne l'orologio se non c'è attesa
        }

        lblFiches.setText("Fiches: " + state.getFiches());

        panelBanco.removeAll();
        panelGiocatore.removeAll();
        panelAvversari.removeAll();
        
        //ridimensionamento delle mani quando facciamo lo split
        List<List<String>> mani = state.getManiGiocatore();
        double scaleFactor = 1.0; 
        
        if (mani.size() == 2) {
            scaleFactor = 0.80; //rimpicciolisce all'80%
        } else if (mani.size() == 3) {
            scaleFactor = 0.65; //rimpicciolisce al 65%
        } else if (mani.size() >= 4) {
            scaleFactor = 0.50; //rimpicciolisce al 50%
        }
        
        //il banco usa la scala normale 1.0 per non rimpicciolire il banco anche se il giocatore fa split
        if (!state.getCarteDealer().isEmpty()) {
            int puntiBanco = state.getPunteggioDealer();
            boolean bancoSballato = (puntiBanco > 21);
            boolean bancoBlackjack = (puntiBanco == 21 && state.getCarteDealer().size() == 2 && state.isFinePartita());
            
            String testoBanco = " (Punti: " + puntiBanco + ") ";
            if (bancoSballato) 
            	testoBanco = " (SBALLATO) ";
            else if (bancoBlackjack) 
            	testoBanco = " (BLACKJACK!) ";
            
            JLabel lblPuntiBanco = new JLabel(testoBanco);
            //operatore ternario per il colore da usare in base alla situazione
            lblPuntiBanco.setForeground(bancoSballato ? new Color(255, 80, 80) : (bancoBlackjack ? new Color(50, 255, 50) : Color.WHITE)); 
            lblPuntiBanco.setFont(new Font(FONT_GIOCO, Font.BOLD, 16));
            panelBanco.add(lblPuntiBanco);
            
            for (String nomeCarta : state.getCarteDealer()) {
                //passiamo 1.0 come fattore di scala al banco
                panelBanco.add(creaLabelCarta(nomeCarta, 1.0)); 
            }
        }

        //per ogni mano del giocatore, creiamo un pannello che le contiene tutte e lo aggiungiamo al pannello del giocatore
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
            //anche il testo si rimpicciolisce un po' se fai tanti split
            lblPunti.setFont(new Font(FONT_GIOCO, Font.BOLD, (int)(15 * scaleFactor))); 
            singolaMano.add(lblPunti);

            for (String nomeCarta : mani.get(i)) {
                //passiamo lo scaleFactor al metodo che disegna la carta
                singolaMano.add(creaLabelCarta(nomeCarta, scaleFactor)); 
            }
            
            int scommessa = state.getScommesseMani().get(i);
            if (scommessa > 0) {
                //se hai tante mani, rimpiccioliamo anche le fiches usando il parametro 'piccole' = true
                singolaMano.add(creaPannelloFiches(scommessa, scaleFactor < 1.0));
            }

            panelGiocatore.add(singolaMano);
        }

        int sedieOccupate = 0;
        
        //per ogni avversario, creiamo un pannello che contiene tutte le sue mani e lo aggiungiamo al pannello degli avversari
        if (state.getAltriGiocatori() != null) {
            //utilizziamo questo for in modo che tutti quelli che si aggiungono in pù non vedano le carte così da evitare caos
        	for (Map.Entry<String, List<List<String>>> entry : state.getAltriGiocatori().entrySet()) {
                if (sedieOccupate >= 3) 
                	break; 

                String nick = entry.getKey();
                List<List<String>> maniAvversario = entry.getValue();

                List<Integer> scommesseAvv = null;
                if (state.getScommesseAvversari() != null) {
                    scommesseAvv = state.getScommesseAvversari().get(nick);
                }

                
                JPanel slotGiocatore = new JPanel();
                //usiamo un BoxLayout verticale per impilare le mani una sotto l'altra
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

                        //qui creiamo un gap negativo per fare l'effetto ventaglio delle carte
                        int cardGap = -20; 
                        JPanel panelMano = new JPanel(new FlowLayout(FlowLayout.CENTER, cardGap, 5)) {
                            //facciamo l'override di questo metodo a causa del gap negativo
                        	@Override
                            public Dimension getPreferredSize() {
                                Dimension d = super.getPreferredSize();
                                d.width += Math.abs(cardGap); 
                                return d;
                            }
                        };
                        panelMano.setOpaque(false);
                        panelMano.setAlignmentX(Component.CENTER_ALIGNMENT);

                        //invertiamo la destra con la sinistra così da vedere le carte in ordine corretto
                        panelMano.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

                        //aggiungiamo le fiches che andranno a destra
                        if (scommesseAvv != null && scommesseAvv.size() > m) {
                            int puntata = scommesseAvv.get(m);
                            if (puntata > 0) {
                                JPanel pnlFiches = creaPannelloFiches(puntata, true);
                                //diamo 40 pixel di margine per compensare il gap negativo e staccarle dalle carte
                                pnlFiches.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0)); 
                                panelMano.add(pnlFiches);
                            }
                        }
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

        //ciclo che se non c'è nessuno crea delle sedie vuote fittizie
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
        
        //ora creiamo un pop-up per la bancarotta
        if (state.getFiches() <= 0 && state.getMessaggioAvviso() != null && state.getMessaggioAvviso().contains("BANCAROTTA")) {
            
            if (!bancarottaMostrata) {
                bancarottaMostrata = true; 

                //apriamo il pop-up in un canale separato così i "tic" del timer non creano sfarfallii
                new Thread(() -> {
                    JOptionPane.showMessageDialog(BlackjackGUI.this, 
                        "Hai esaurito tutte le fiches!\nLa tua partita finisce qui. Grazie per aver giocato.", 
                        "💸 Bancarotta!", 
                        JOptionPane.ERROR_MESSAGE); 
                    
                    client.inviaComando("capito_bancarotta");

                    if (isHost) {
                        BlackjackGUI.this.dispose(); 
                    } else {
                        System.exit(0); 
                    }
                }).start();
            }
        }
    }

    //in questo metodo gestiamo le varie fasi del gioco con i bottoni
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
        
        btnCarta.setVisible(isTurno);
        btnSto.setVisible(isTurno);
        btnRaddoppio.setVisible(isTurno);
        btnSplit.setVisible(isTurno);
        
        //rendiamo i tasti attivi solo quando possono essere schiacciati
        if (isTurno) {
            //carta e sto si possono fare sempre
            btnCarta.setEnabled(true);
            btnSto.setEnabled(true);
            
            //leggiamo la frase che ci manda il Server
            String opzioniServer = state.getMessaggioAvviso() != null ? state.getMessaggioAvviso().toLowerCase() : "";
            
            //se il Server ci ha concesso le mosse speciali, attiviamo i tasti, altrimenti restano grigi!
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
        //prende il percorso della carta in base al suo nome
        String path = nomeCarta.equals("[CARTA COPERTA]") ? "images/retro.png" : "images/" + nomeCarta.replace(" ", "_") + ".png";
       
        int larghezzaScalata = (int) (LARGHEZZA_CARTA * scaleFactor);
        int altezzaScalata = (int) (ALTEZZA_CARTA * scaleFactor);
        
        ImageIcon iconaOriginale = new ImageIcon(path);
        Dimension dimensioniFisse = new Dimension(larghezzaScalata, altezzaScalata);

        //imposta i limiti della JLabel usando dimensioni rimpicciolite
        carta.setPreferredSize(dimensioniFisse);
        carta.setMinimumSize(dimensioniFisse);
        carta.setMaximumSize(dimensioniFisse);

        //se non trova l'immagine invece di esplodere scrive a schermo il nome della carta che manca
        if (iconaOriginale.getIconWidth() == -1) {
            carta.setText("Manca: " + path);
            carta.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            carta.setOpaque(true);
            carta.setBackground(Color.WHITE);
        } else {
            Image img = iconaOriginale.getImage();
            //rimpicciolisce fisicamente l'immagine
            Image nuovaImg = img.getScaledInstance(larghezzaScalata, altezzaScalata, Image.SCALE_SMOOTH);
            carta.setIcon(new ImageIcon(nuovaImg));
        }
        return carta;
    }
    
    /**
     * crea la carta rimpicciolita in caso di split
     * @param nomeCarta
     * @return
     */
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
    
    //pannello per visualizzazione fiches sovrapposte
    private JPanel creaPannelloFiches(int totale, boolean piccole) {
        //contenitore principale
        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        container.setOpaque(false);

        int size = piccole ? 30 : 50;
        //8 pixel di spazio per le piccole, 12 o 14 pixel per le grandi!
        int yOffset = piccole ? 8 : 12;

        //calcoliamo quante fiches servono
        int resto = totale;
        int[] valori = {500, 250, 100, 50};
        String[] files = {"chipPurple.png", "chipGreen.png", "chipBlue.png", "chipBlack.png"};

        //salviamo le immagini pronte da disegnare
        List<Image> fichesDaDisegnare = new ArrayList<>();

        //usiamo la cache per velocizzare la creazione delle immagini
        for (int i = 0; i < valori.length; i++) {
            while (resto >= valori[i]) {
                resto -= valori[i];
                String nomeFile = files[i];
                
                //scegliamo in quale scatola cercare in base alla grandezza
                Map<String, Image> cacheGiusta = piccole ? cacheFichesPiccole : cacheFichesGrandi;
                
                //se l'immagine NON c'è nella memoria, la peschiamo dal disco e la rimpiccioliamo
                if (!cacheGiusta.containsKey(nomeFile)) {
                    ImageIcon icona = new ImageIcon("images/" + nomeFile);
                    if (icona.getIconWidth() != -1) {
                        Image img = icona.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        cacheGiusta.put(nomeFile, img); // La salviamo in memoria per il futuro!
                    }
                }
                
                //ora peschiamo l'immagine direttamente dalla RAM
                if (cacheGiusta.containsKey(nomeFile)) {
                    fichesDaDisegnare.add(cacheGiusta.get(nomeFile));
                }
            }
        }
        
        //se il resto è minore di 50 dsegnamo comunque la fiche nera 
        if (resto > 0) {
            String nomeFile = files[files.length - 1]; //prende l'ultima fiche dell'array (chipBlack.png)
            
            Map<String, Image> cacheGiusta = piccole ? cacheFichesPiccole : cacheFichesGrandi;
            
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

        //calcoliamo l'altezza TOTALE esatta della pila
        int altezzaPila = fichesDaDisegnare.isEmpty() ? size : size + ((fichesDaDisegnare.size() - 1) * yOffset);

        //pannello per la creazione dello stack di fiches
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
                
                return new Dimension(size, altezzaPila);
            }
        };
        panelStack.setOpaque(false);

        //il testo con il totale
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
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            //se per qualche motivo fallisce, userà il tema di base
        }

        SwingUtilities.invokeLater(() -> avviaSchermataDiLogin());
    }

    //frame di login
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
                    File fileImmagine = new File("images/sfondo.jpg");
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

        
        MouseAdapter dragger = new MouseAdapter() {
            int mouseX, mouseY;
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
            }
            public void mouseDragged(MouseEvent e) {
                loginFrame.setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY);
            }
        };
        backgroundPanel.addMouseListener(dragger);
        backgroundPanel.addMouseMotionListener(dragger);

        //anche qui abbiamo tolto la striscia di windows e abbiamo inserito una X rossa
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
        Color colorOro = new Color(255, 191, 0); 
        Color colorBgInput = new Color(10, 10, 10, 210); 

        //field del nome giocatore
        JTextField txtNick = new JTextField("INSERISCI NICKNAME");
        txtNick.setBounds(260, 530, 280, 40);
        txtNick.setFont(new Font("Georgia", Font.BOLD, 16));
        txtNick.setForeground(Color.GRAY);
        txtNick.setCaretColor(Color.WHITE);
        txtNick.setBackground(colorBgInput);
        txtNick.setOpaque(true);
        txtNick.setBorder(BorderFactory.createLineBorder(colorOro, 2)); // Bordo d'oro!
        txtNick.setHorizontalAlignment(JTextField.CENTER);

        
        txtNick.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtNick.getText().equals("INSERISCI NICKNAME")) {
                    txtNick.setText("");
                    txtNick.setForeground(Color.WHITE);
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

        //bottone start serve per avviare la partita
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
        
        //pannello informativo
        JPanel pannelloInfo = new JPanel();
        pannelloInfo.setLayout(new BorderLayout());
        pannelloInfo.setBounds(10, 55, 420, 230);
        pannelloInfo.setBackground(new Color(30, 30, 30, 240));
        pannelloInfo.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        pannelloInfo.setVisible(false);

        JTextArea txtSpiegazione = new JTextArea();
        txtSpiegazione.setText("COME FUNZIONA LA CONNESSIONE?\n\n"
            + "CREA UNA TUA PARTITA (HOST):\n"
            + "Lascia la casella IP su 'localhost'. Il gioco avvierà\n"
            + "il server in automatico e aprirà il tavolo.\n\n"
            + "UNISCITI A UN AMICO (JOIN):\n"
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

        //tasto info
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

    /**
	 * questo metodo gestisce la logica di avvio del gioco in base all'IP inserito:
	 * - Se l'IP è "localhost", prova a connettersi al server locale. Se non trova un server attivo, lo avvia lui stesso e si connette come Host.
	 * - Se l'IP è diverso, si connette come Client al server remoto specificato.
	 */
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
                    try { Server.main(new String[]{}); } catch (Exception ex) { ex.printStackTrace(); }
                }).start();
                try { Thread.sleep(500); } catch (InterruptedException ex) {}
                
                new BlackjackGUI(nick, "localhost", true); // LUI È IL VERO HOST

            } else {
                System.out.println("Server locale già attivo! Entro come giocatore...");
                new BlackjackGUI(nick, "localhost", false); // LUI È SOLO UN CLIENT!
            }
            
        } else {
            System.out.println("Mi connetto al server remoto: " + ip);
            new BlackjackGUI(nick, ip, false); // È un Client normale che si connette da fuori
        }
    }
}