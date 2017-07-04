package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.ge.physics.Body;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;

/**
 * Camera class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Camera extends ElevadorActionEntity {
    
    private int x, y;
    private final Rectangle2D area = new Rectangle2D.Double();
    private final Color color = new Color(0, 0, 255, 64);
    private Player player;
    private boolean locked;
    
    public Camera(ElevadorActionScene scene, Player player) {
        super(scene);
        this.player = player;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Rectangle2D getArea() {
        return area;
    }
    
    @Override
    public void init() {
        setVisible(true);
        area.setRect(0, 0, 256, 240);
    }

    @Override
    public void update() {
        //updateInput();
        //if (1 == 1) {
        //    return;
        //}
        
        if (locked) {
            return;
        }
        
        double ax = area.getX();
        double ay = area.getY();
        double cx = 0; // ax + (player.getBody().getX() - ax - 128 + player.getBody().getWidth()/ 2) * 0.1;
        double cy = ay + (player.getBody().getY() - ay - 120 + player.getBody().getHeight() / 2) * 0.1;
        
        double maxTopY = getWorld().getHeight() - area.getHeight();
        cy = cy > maxTopY ? maxTopY : cy;
        
        //area.setRect(Mouse.x, Mouse.y, 256, 240);
        //cx = cx < -8 ? -8 : cx;
        //cx = cx > 8 ? 8 : cx;
        
        // todo
        //limit cy mapHeight - 240
        area.setRect(cx, cy, 256, 240);
    }

    private void updateInput() {
        if (isKeyDown(KeyEvent.VK_LEFT)) {
            x -= 3;
        }
        else if (isKeyDown(KeyEvent.VK_RIGHT)) {
            x += 3;
        }

        if (isKeyDown(KeyEvent.VK_UP)) {
            y -= 3;
        }
        else if (isKeyDown(KeyEvent.VK_DOWN)) {
            y += 3;
        }
        area.setRect(x, y, 256, 240);
    }
    
    public void updatePosition(Body body) {
        double cx = 0; 
        double cy = body.getY() + (body.getHeight() - area.getHeight()) / 2;
        area.setRect(cx, cy, 256, 240);
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.draw(area);
    }
    
}
