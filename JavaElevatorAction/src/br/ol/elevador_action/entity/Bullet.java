package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.elevador_action.scene.LevelScene;
import br.ol.g2d.Animation;
import br.ol.ge.physics.Body;
import br.ol.ge.spatial_partition.Area;
import java.awt.Graphics2D;
import java.util.Set;

/**
 * Bullet class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Bullet extends ElevadorActionEntity {

    private final Camera camera;
    private Animation animation;
    private Body<Bullet> body;
    private int direction;
    private boolean collided;
    private final int target; // 2 = player, 4 = enemy
    
    public Bullet(ElevadorActionScene scene, Camera camera, int target) {
        super(scene);
        this.camera = camera;
        this.target = target;
    }

    public Body<Bullet> getBody() {
        return body;
    }

    public int getDirection() {
        return direction;
    }

    public int getTarget() {
        return target;
    }
    
    @Override
    public void init() {
        body = new Body<Bullet>(this, false, true, 1, 0, 0, 6, 3, 0, 0);
        body.setAffectedByGravity(false);
        getWorld().addBody(body);
        setVisible(false);
        setZorder(7);
    }

    @Override
    public void update() {
        if (!isVisible()) {
            return;
        }
        
        if (collided) {
            animation.update(1000 / 45);
            if (animation.playing) {
                return;
            }
            setVisible(false);
            return;
        }
        
        body.setVelocityX(direction * 3);
        
        if (!camera.getArea().contains(body)) {
            setVisible(false);
            return;
        }

        // check collisions
        Set<Body> bodies = getWorld().retrieve(body, true, true);
        for (Body b : bodies) {
            long cc = b.getCollisionCategory();
            // wall, floor
            if (cc == 1 || cc == 32 || cc == 128) {
                hit();
                    return;
            }
            // hit player
            if (target == 2 && cc == 2) { 
                Player player = (Player) b.getOwner();
                if (player.isHitable()) {
                    player.hit();
                    hit();
                    return;
                }
            }
            // hit enemy
            if (target == 4 && cc == 4) { 
                Enemy enemy = (Enemy) b.getOwner();
                if (enemy.isHitable()) {
                    getModel().addScore(100);
                    enemy.hit();
                    hit();
                    return;
                }
            }
            // lamp
            if (target == 4 && b.getOwner().toString().startsWith("lamp_") 
                    && getModel().isLightsOn() && !getModel().isLampHit()) { // hit lamp
                
                LevelScene scene = (LevelScene) getScene();
                String[] lampInfo = b.getOwner().toString().replace("lamp_", "").split(",");
                int c = Integer.parseInt(lampInfo[0]);
                int r = Integer.parseInt(lampInfo[1]);
                long gid = getTmxParser().layers.get(0).get(c, r);
                if (gid == 24) {
                    scene.getLampFalling().show((int) b.x, (int) b.y);
                    hit();
                    getModel().setLampHit(true);
                    // replace lamp tile with empty tile
                    getTmxParser().layers.get(0).set(c, r, 21);
                    Set<Area> areas = scene.getTilemap().getTilesSpatialPartition()[0].retrieve(b);
                    for (Area area : areas) {
                        if (area.getOwner() instanceof Long) {
                            area.setOwner(21L);
                        }
                    }
                }
            }
        }
    }

    private void hit() {
        collided = true;
        animation = getG2D().getAnimations().get("bullet_collision_" + (direction > 0 ? "right" : "left"));
        animation.stop();
        animation.play();
        body.setVelocityX(0);
    }

    @Override
    public void draw(Graphics2D g) {
        g.translate(body.x, body.y);
        animation.draw(g);
        g.translate(-body.x, -body.y);
    }
    
    public void spawn(int x, int y, int direction) {
        this.direction = direction;
        body.x = x;
        body.y = y;
        collided = false;
        setVisible(true);
        animation = getG2D().getAnimations().get("bullet");
    }
        
}
