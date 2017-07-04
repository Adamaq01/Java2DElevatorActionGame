package br.ol.ge.core;

import java.awt.Graphics2D;

/**
 * Game class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Game {
    
    private Scene scene;

    public Game() {
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        scene.setGame(this);
        scene.onActivatedInternal();
    }
    
    public void update() {
        if (scene != null) {
            scene.updateInternal();
        }
    }
    
    public void draw(Graphics2D g) {
        if (scene != null) {
            scene.draw(g);
        }
    }
    
}
