package br.ol.elevador_action;

import br.ol.g2d.G2DContext;
import br.ol.ge.core.Scene;
import br.ol.ge.map.TMXParser;
import br.ol.ge.physics.World;

/**
 *
 * @author leonardo
 */
public class ElevadorActionScene extends Scene<ElevadorActionGame> {
    
    public ElevadorActionScene() {
    }

    @Override
    protected void setGame(ElevadorActionGame game) {
        super.setGame(game);
    }
    
    public ElevadorActionModel getModel() {
        return getGame().getModel();
    }

    public TMXParser getTmxParser() {
        return getGame().getModel().getTMXParser();
    }

    public G2DContext getG2D() {
        return getGame().getModel().getG2D();
    }

    public World getWorld() {
        return getGame().getModel().getWorld();
    }
    
}
