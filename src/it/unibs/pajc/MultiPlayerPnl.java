package it.unibs.pajc;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Graphics;

public class MultiPlayerPnl extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Create the panel.
	 */
	public MultiPlayerPnl() 
	{
		
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		Image background = new ImageIcon(getClass().getResource("/Images/BackGroundSingle.png")).getImage();
		g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
	}

}
