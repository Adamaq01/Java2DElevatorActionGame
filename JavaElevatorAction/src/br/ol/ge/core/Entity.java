package br.ol.ge.core;

import java.awt.Graphics2D;

/**
 * Entity class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Entity<T> implements Comparable<Entity> {
    
    private T scene;
    private boolean visible;
    private int zorder;
    
    public Entity(T scene) {
        this.scene = scene;
    }

    public T getScene() {
        return scene;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getZorder() {
        return zorder;
    }

    protected void setZorder(int zorder) {
        this.zorder = zorder;
    }

    public void init() {
    }
    
    // every time this scene is invoked
    public void onActivated() {
    }

    public void updateInternal() {
        update();
    }
    
    public void update() {
    }
    
    public void draw(Graphics2D g) {
    }

    @Override
    public int compareTo(Entity o) {
        return zorder - o.zorder;
    }
    
}
