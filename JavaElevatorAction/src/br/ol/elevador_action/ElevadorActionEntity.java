package br.ol.elevador_action;

import br.ol.g2d.G2DContext;
import br.ol.ge.core.Entity;
import br.ol.ge.input.Keyboard;
import br.ol.ge.map.TMXParser;
import br.ol.ge.physics.World;

/**
 * ElevadorActionEntity class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class ElevadorActionEntity extends Entity<ElevadorActionScene> {

    public ElevadorActionEntity(ElevadorActionScene scene) {
        super(scene);
    }

    public ElevadorActionGame getGame() {
        return getScene().getGame();
    }
    
    public ElevadorActionModel getModel() {
        return getScene().getGame().getModel();
    }

    public TMXParser getTmxParser() {
        return getScene().getGame().getModel().getTMXParser();
    }

    public G2DContext getG2D() {
        return getScene().getGame().getModel().getG2D();
    }
    
    public World getWorld() {
        return getScene().getGame().getModel().getWorld();
    }
    
    public void updateAfterPhysics() {
    }
    
    public boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode);
    }

    public boolean isKeyPressed(int keyCode) {
        return Keyboard.isKeyPressed(keyCode);
    }
    
}
