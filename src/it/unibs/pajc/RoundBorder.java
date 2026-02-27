package it.unibs.pajc;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.swing.*;
import javax.swing.border.Border;

class RoundBorder implements Border {

    private int radius;


    RoundBorder(int radius) {
        this.radius = radius;
    }


    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }


    public boolean isBorderOpaque() {
        return true;
    }


    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
    	g2d.setColor(new Color(255, 228, 170));
    	g2d.drawRoundRect(x, y, width-1, height-1, radius, radius);
    }
    	
    	
}