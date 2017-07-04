package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import java.awt.Graphics2D;

/**
 * GameOver class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class GameOver extends ElevadorActionEntity {
    
    private Animation animation;
    
    public GameOver(ElevadorActionScene scene) {
        super(scene);
    }

    @Override
    public void init() {
        animation = getG2D().getAnimations().get("game_over");
    }

    @Override
    public void onActivated() {
        animation.currentFrameIndex = 0;
        animation.stop();
        setVisible(false);
    }
    
    @Override
    public void update() {
        if (isVisible()) {
            animation.update(1000 / 45);
            if (!animation.playing) {
                setVisible(false);
                getModel().backToTitle();
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        animation.draw(g);
    }
    
    public void show() {
        setVisible(true);
        animation.stop();
        animation.play();
    }
    
}
