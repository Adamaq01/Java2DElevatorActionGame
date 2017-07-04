package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import java.awt.Graphics2D;

/**
 * LevelCleared class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class LevelCleared extends ElevadorActionEntity {
    
    private Camera camera;
    private Animation animation;
    
    public LevelCleared(ElevadorActionScene scene) {
        super(scene);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void init() {
        animation = getG2D().getAnimations().get("level_cleared");
        setZorder(6);
    }

    @Override
    public void onActivated() {
        animation.currentFrameIndex = 0;
        animation.stop();
        setVisible(true);
    }
    
    @Override
    public void update() {
        animation.update(1000 / 45);
        if (animation.currentFrameIndex < animation.lastFrameIndex) {
            return;
        }
        getModel().addScore(1000); // TODO
        getModel().nextLevel();
    }

    @Override
    public void draw(Graphics2D g) {
        g.translate(0, getWorld().getHeight() - camera.getArea().getHeight());
        animation.draw(g);
        g.translate(0, camera.getArea().getHeight() - getWorld().getHeight());
    }
    
    public void show() {
        animation.stop();
        animation.play();
        animation.currentFrameIndex = 1;
    }
    
}
