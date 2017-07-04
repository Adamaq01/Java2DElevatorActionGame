package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import br.ol.g2d.Sprite;
import br.ol.g2d.TextBitmapScreen;
import java.awt.Graphics2D;

/**
 * HUD class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class HUD extends ElevadorActionEntity {
    
    private TextBitmapScreen screen;
    private Sprite scoreBorder;
    private Animation[] lives = new Animation[3];
    private Sprite lifeBorder;
    
    public HUD(ElevadorActionScene scene) {
        super(scene);
    }

    @Override
    public void init() {
        screen = getG2D().getTextScreens().get("hud");
        scoreBorder = getG2D().getSpriteSheet().getSprite("score_border");
        lifeBorder = getG2D().getSpriteSheet().getSprite("life_border");
        for (int i = 0; i < lives.length; i++) {
            lives[i] = getG2D().getAnimations().getCopy("life");
        }
        setVisible(true);
    }

    @Override
    public void onActivated() {
        for (int i = 0; i < lives.length; i++) {
            lives[i].currentFrameIndex = i < getModel().getLives() ? 0 : lives[i].lastFrameIndex;
            lives[i].stop();
        }
    }
    
    @Override
    public void update() {
        screen.print(3, 1, getModel().getScore());
        screen.print(13, 1, getModel().getHiscore());
        for (int i = 0; i < lives.length; i++) {
            lives[i].update(1000 / 45);
        }
    }
    
    @Override
    public void draw(Graphics2D g) {
        scoreBorder.draw(g, 0, 0, 0.7);
        screen.draw(g);
        g.translate(200, 4);
        for (int i = 0; i < lives.length; i++) {
            if (lives[i].currentFrameIndex < lives[i].lastFrameIndex) {
                lifeBorder.draw(g, (i + 1) * 9, 0, 0.7);
            }
        }
        for (int i = 0; i < lives.length; i++) {
            g.translate(9, 0);
            lives[i].draw(g);
        }
        g.translate(-227, -4);
    }
    
    public void died() {
        for (int i = 2; i >= 0; i--) {
            if (!lives[i].playing && lives[i].currentFrameIndex != lives[i].lastFrameIndex) {
                lives[i].play();
                break;
            }
        }
    }
    
}
