package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.g2d.Animation;
import br.ol.g2d.Sprite;
import br.ol.ge.physics.Body;
import java.awt.Graphics2D;

/**
 * RedDoor class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class RedDoor extends ElevadorActionEntity {
    
    private int x;
    private int y;
    private int direction;
    private Body<RedDoor> body;
    private Sprite sprite;
    private Sprite positionSprite;
    private Animation pointAnimation;
    private boolean opened = false;
    private boolean showPoint;
    private Body modelBody;
    
    public RedDoor(ElevadorActionScene scene, Body redDoorBody) {
        super(scene);
        this.direction = redDoorBody.getOwner().toString().endsWith("_right") ? 1 : -1; 
        int tx = direction > 0 ? -8 : 0;
        this.x = (int) redDoorBody.x + tx;
        this.y = (int) redDoorBody.y;
        this.modelBody = redDoorBody;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public void init() {
        int tx = direction > 0 ? 15 : -1;
        body = new Body<RedDoor>(this, false, false, 0, x + tx, y + 21, 1, 8, 0, 0);        
        getWorld().addBody(body);
        String directionStr = direction > 0 ? "right" : "left";
        sprite = getG2D().getSpriteSheet().getSprite("door_red_" + directionStr + "_0");
        positionSprite = getG2D().getSpriteSheet().getSprite("red_door_position");
        pointAnimation = getG2D().getAnimations().getCopy("red_door_point");
        pointAnimation.stop();
        setVisible(true);
        setZorder(4);
    }

    @Override
    public void update() {
        if (showPoint) {
            pointAnimation.update(1000 / 45);
            if (!pointAnimation.playing) {
                setVisible(false);
                showPoint = false;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (!opened) {
            sprite.draw(g, x, y);
        }
        if (!showPoint) {
            int tx = direction > 0 ? x + 12 : x - 5;
            positionSprite.draw(g, tx, y + 28);
        }
        else if (showPoint) {
            int tx = x + 1;
            int ty = y + 10;
            g.translate(tx, ty);
            pointAnimation.draw(g);
            g.translate(-tx, -ty);
        }
    }
    
    public void open() {
        opened = true;
    }
    
    public boolean isOpened() {
        return opened;
    }
    
    public void addScorePoint() {
        showPoint = true;
        pointAnimation.play();
        getModel().addScore(500);
        getModel().getRedDoors().remove(modelBody);
    }
    
}
