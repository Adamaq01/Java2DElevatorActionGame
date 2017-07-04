package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import java.awt.Graphics2D;

/**
 * Curtain class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Curtain extends ElevadorActionEntity {
    
    private Animation animation;
    
    public Curtain(ElevadorActionScene scene) {
        super(scene);
    }

    @Override
    public void init() {
    }

    @Override
    public void onActivated() {
        animation = null;
    }
    
    @Override
    public void update() {
        if (animation != null && isVisible()) {
            animation.update(1000 / 45);
            if (!animation.playing) {
                animation = null;
                setVisible(false);
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (animation != null) {
            animation.draw(g);
        }
    }
    
    public void open() {
        animation = getG2D().getAnimations().get("curtain_open");
        animation.stop();
        animation.play();
        setVisible(true);
    }

    public void close() {
        animation = getG2D().getAnimations().get("curtain_close");
        animation.stop();
        animation.play();
        setVisible(true);
    }
    
    public boolean isOpeningOrClosing() {
        return animation != null && animation.playing;
    }
    
}
