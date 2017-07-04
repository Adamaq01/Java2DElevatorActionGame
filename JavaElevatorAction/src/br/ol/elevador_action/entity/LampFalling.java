package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import java.awt.Graphics2D;

/**
 * LampFalling class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class LampFalling extends ElevadorActionEntity {
    
    private int x;
    private int y;
    private Animation animation;
    
    public LampFalling(ElevadorActionScene scene) {
        super(scene);
    }

    @Override
    public void init() {
        animation = getG2D().getAnimations().get("lamp_falling");
        animation.currentFrameIndex = 0;
        animation.stop();
        setVisible(false);
        setZorder(7);
    }

    @Override
    public void update() {
        if (isVisible()) {
            animation.update(1000 / 45);
            if (!animation.playing) {
                getModel().setLightsOff();
                setVisible(false);
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.translate(x, y);
        animation.draw(g);
        g.translate(-x, -y);
    }
    
    public void show(int x, int y) {
        this.x = x;
        this.y = y;
        setVisible(true);
        animation.stop();
        animation.play();
    }
    
}
