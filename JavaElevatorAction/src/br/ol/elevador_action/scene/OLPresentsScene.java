package br.ol.elevador_action.scene;

import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import java.awt.Graphics2D;

/**
 * OLPresentsScene class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class OLPresentsScene extends ElevadorActionScene {
    
    private Animation animation;
    private long startTime;
    
    public OLPresentsScene() {
    }

    @Override
    public void init() {
        animation = getG2D().getAnimations().get("ol_presents");
    }
    
    @Override
    public void onActivated() {
        animation.stop();
        animation.play();
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public void update() {
        // wait 3 seconds before start
        if (System.currentTimeMillis() - startTime < 3000) {
            return;
        }
        animation.update(1000 / 45);
        if (animation.playing) {
            return;
        }
        getGame().changeScene("title");
    }

    @Override
    public void draw(Graphics2D g) {
        g.translate(0, -8);
        animation.draw(g);
        g.translate(0, 8);
    }
    
}
