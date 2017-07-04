package br.ol.elevador_action.scene;

import br.ol.elevador_action.ElevadorActionScene;
import br.ol.elevador_action.entity.HUD;
import br.ol.g2d.Animation;
import br.ol.g2d.TextBitmapScreen;
import br.ol.ge.input.Keyboard;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * TitleScene class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class TitleScene extends ElevadorActionScene {
    
    private TextBitmapScreen screen;
    private Animation animation;
    
    private String[] pressSpaceToStartText = {"                    ", "PRESS SPACE TO START"};
    
    public TitleScene() {
    }

    @Override
    public void init() {
        screen = getG2D().getTextScreens().get("screen");
        animation = getG2D().getAnimations().get("title");
    }
    
    @Override
    public void onActivated() {
        animation.stop();
        animation.play();
        screen.print(6, 22, pressSpaceToStartText[0]);
    }

    @Override
    public void createAllEntities() {
        addEntity(new HUD(this));
    }
    
    @Override
    public void update() {
        animation.update(1000 / 60);
        if (animation.playing) {
            return;
        }
        int index = ((System.nanoTime() / 200000000) % 3) < 2 ? 1 : 0;
        screen.print(6, 22, pressSpaceToStartText[index]);
        if (Keyboard.isKeyPressed(KeyEvent.VK_SPACE)) {
            getModel().startGame();
        }
    }

    @Override
    public void draw(Graphics2D g) {
        animation.draw(g);
        screen.draw(g);
        super.draw(g); // drawHUD
    }
    
}
