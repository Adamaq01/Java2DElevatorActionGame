package br.ol.ge.core;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scene class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Scene<T extends Game> {
    
    private T game;
    private final List<Entity> entities = new ArrayList<Entity>();
    private boolean needsReorder;
    
    public Scene() {
    }

    public T getGame() {
        return game;
    }

    protected void setGame(T game) {
        this.game = game;
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public void addEntity(Entity entity) {
        entities.add(entity);
    }
    
    public void changeEntityZOrder(Entity e, int newZOrder) {
        if (e.getZorder() != newZOrder) {
            e.setZorder(newZOrder);
            needsReorder = true;
        }
    }
    
    public void start() {
        init();
        createAllEntities();
        initAllEntities();
    }
    
    // initialized just once
    public void init() {
    }
    
    public void createAllEntities() {
        // override your code here
    }

    protected void initAllEntities() {
        for (Entity entity : entities) {
            entity.init();
        }
    }
    
    public void onActivatedInternal() {
        onActivated();
        for (Entity entity : entities) {
            entity.onActivated();
        }
        needsReorder = true;
    }
    
    // every time this scene is invoked
    public void onActivated() {
    }
    
    public void update() {
    }
    
    public void updateInternal() {
        update();
        for (Entity entity : entities) {
            entity.updateInternal();
        }
    }
    
    public void draw(Graphics2D g) {
        if (needsReorder) {
            Collections.sort(entities);
            needsReorder = false;
        }
        for (Entity entity : entities) {
            if (entity.isVisible()) {
                entity.draw(g);
            }
        }
    }
    
}
