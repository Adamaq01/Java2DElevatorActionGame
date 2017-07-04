package br.ol.ge.core;

import br.ol.ge.input.Keyboard;
import br.ol.ge.input.Mouse;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;


/**
 * Display class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Display extends Canvas {
    
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;
    public static double SCREEN_SCALE_X;
    public static double SCREEN_SCALE_Y;
    private boolean running;
    private BufferStrategy bs;
    private Game game;
    private BufferedImage offscreen;
    private Color backgroundColor = Color.BLACK;
    
    public Display(Game game, int screenWidth, int screenHeight, double screenScaleX, double screenScaleY) {
        this.game = game;
        SCREEN_WIDTH = screenWidth;
        SCREEN_HEIGHT = screenHeight;
        SCREEN_SCALE_X = screenScaleX;
        SCREEN_SCALE_Y = screenScaleY;
        setPreferredSize(new Dimension((int) (screenWidth * screenScaleX), (int) (screenHeight * screenScaleY)));
        addKeyListener(new Keyboard());
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        offscreen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    private class MainLoop implements Runnable {

        @Override
        public void run() {
            boolean needsRedraw = false;
            while (running) {
                Time.update();
                while (Time.getUpdateCount() > 0) {
                    update();
                    Time.decUpdateCount();
                    needsRedraw = true;
                }
                if (needsRedraw) {
                    Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
                    g2d.scale(SCREEN_SCALE_X, SCREEN_SCALE_Y);
                    draw((Graphics2D) offscreen.getGraphics());
                    g2d.drawImage(offscreen, 0, 0, null);
                    bs.show();
                    needsRedraw = false;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
            }
        }
        
    }
    
    public void start() {
        createBufferStrategy(1);
        bs = getBufferStrategy();
        running = true;
        new Thread(new MainLoop()).start();
    }
    
    private void update() {
        game.update();
    }
    
    private void draw(Graphics2D g) {
        g.setBackground(backgroundColor);
        g.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        game.draw(g);
        // g.drawString("FPS: " + Time.getFPS(), 5, 10);
    }
    
}
