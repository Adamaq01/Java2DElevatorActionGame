package br.ol.ge.physics;

import br.ol.ge.spatial_partition.GridSpatialPartition;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * World class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class World {

    private int width;
    private int height;
    private double gravity = 0.15;
    private List<Body> bodies = new ArrayList<Body>();
    private GridSpatialPartition<Body> spatialPartition;
    
    public World(int spaceWidth, int spaceHeight, int cellWidth, int cellHeight) {
        width = spaceWidth;
        height = spaceHeight;
        spatialPartition = new GridSpatialPartition(spaceWidth, spaceHeight, cellWidth, cellHeight);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        this.gravity = gravity;
    }

    public GridSpatialPartition<Body> getSpatialPartition() {
        return spatialPartition;
    }

    public void addBody(Body body) {
        body.setWorld(this);
        if (body.isDynamic()) {
            bodies.add(body);
        }
        spatialPartition.update(body);
    }
    
    public void update() {
        for (Body body : bodies) {
            if (body.isAffectedByGravity()) {
                body.addVelocityY(gravity);
            }
            body.updatePhysics();
            spatialPartition.update(body);
        }
    }
    
    private final Set<Body> retrievedBodiesTmp = new HashSet<Body>();
    public Set<Body> retrieve(Rectangle2D b1, boolean rigid, boolean notRigid) {
        retrievedBodiesTmp.clear();
        Set<Body> colliders = spatialPartition.retrieve(b1);
        for (Body b2 : colliders) {
            if ((b1 != b2) && ((rigid && b2.isRigid()) || (notRigid && !b2.isRigid())) && b1.intersects(b2)) {
                retrievedBodiesTmp.add(b2);
            }
        }
        return retrievedBodiesTmp;
    }
    
    public Set<Body> retrieve(Rectangle2D b1, boolean rigid, boolean notRigid, Class bodyType, Class ownerType) {
        retrievedBodiesTmp.clear();
        Set<Body> colliders = spatialPartition.retrieve(b1);
        for (Body b2 : colliders) {
            if ((b1 != b2) && bodyType.isInstance(b2) && ownerType.isInstance(b2.getOwner()) 
                    && ((rigid && b2.isRigid()) || (notRigid && !b2.isRigid())) && b1.intersects(b2)) {
                retrievedBodiesTmp.add(b2);
            }
        }
        return retrievedBodiesTmp;
    }
    
    public void drawDebug(Graphics2D g) {
        g.setColor(Color.GREEN);
        for (Body body : bodies) {
            g.draw(body);
        }
        spatialPartition.drawDebug(g);
    }
    
}
