package it.unibs.pajc;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class SinglePlayerPnl extends JPanel {

	private static final long serialVersionUID = 1L;

	public SinglePlayerPnl() 
	{
		
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		Image background = new ImageIcon(getClass().getResource("/Images/BackGroundSingle.png")).getImage();
		g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
	}

}
