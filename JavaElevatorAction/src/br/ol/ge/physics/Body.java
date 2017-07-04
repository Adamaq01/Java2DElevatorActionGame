package br.ol.ge.physics;

import br.ol.ge.spatial_partition.Area;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

/**
 * Body class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Body<T> extends Area {
    
    private World world;
    private final int mass;
    private boolean rigid;
    private boolean affectedByGravity;
    private final boolean dynamic;
    private double velocityX;
    private double velocityY;
    private final long collisionMask;
    private final long collisionCategory;
    private boolean pressing;
    
    public Body(T owner, boolean rigid, boolean dynamic, int mass, double x, double y, double width, double height, long collisioncategory, long collisionMask) {
        super(owner);
        this.rigid = rigid;
        this.dynamic = dynamic;
        this.mass = dynamic ? mass : Integer.MAX_VALUE;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.collisionCategory = collisioncategory;
        this.collisionMask = collisionMask;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public boolean isRigid() {
        return rigid;
    }

    public void setRigid(boolean rigid) {
        this.rigid = rigid;
    }

    public boolean isAffectedByGravity() {
        return affectedByGravity;
    }

    public void setAffectedByGravity(boolean affectedByGravity) {
        this.affectedByGravity = affectedByGravity;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public int getMass() {
        return mass;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public void addVelocityX(double velocityX) {
        this.velocityX += velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public void addVelocityY(double velocityY) {
        this.velocityY += velocityY;
    }

    public long getCollisionCategory() {
        return collisionCategory;
    }
    
    public long getCollisionMask() {
        return collisionMask;
    }

    public boolean isPressing() {
        return pressing;
    }

    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }
    
    public void updatePhysics() {
        pressing = false;
        
        int dy = (int) velocityY;
        int signY = dy < 0 ? -1 : 1;
        while (dy != 0) {
            if (canPushY(signY)) {
                dy -= signY;
                pushY(signY);
            }
            else {
                pressing = true;
                velocityY = 0;
                break;
            }
        }
        
        int dx = (int) velocityX;
        int signX = dx < 0 ? -1 : 1;
        while (dx != 0) {
            if (canPushX(signX)) {
                dx -= signX;
                pushX(signX);
            }
            else {
                pressing = true;
                velocityX = 0;
                break;
            }
        }
    }
    
    private final Set<Body> checkedBodiesTmp = new HashSet<Body>();
    private final Set<Body> iteratingBodiesTmp = new HashSet<Body>();
    
    public void pushY(double dy) {
        checkedBodiesTmp.clear();
        checkedBodiesTmp.add(this);
        pushY(dy, mass, checkedBodiesTmp);
    }
    
    private void pushY(double dy, int parentMass, Set<Body> checkedBodies) {
        this.y += dy;
        Set<Body> bodies = world.getSpatialPartition().retrieve(this);
        bodies.removeAll(checkedBodies);
        iteratingBodiesTmp.clear();
        iteratingBodiesTmp.addAll(bodies);
        for (Body body : iteratingBodiesTmp) {
            if (body != this && body.isRigid() 
                    && (this.collisionMask & body.getCollisionCategory()) != 0
                    && (this.collisionCategory & body.getCollisionMask()) != 0
                    && body.intersects(this) && body.getMass() < parentMass) {
                checkedBodies.add(body);
                body.pushY(dy, parentMass, checkedBodies);
            }
        }
    }

    public boolean canPushY(double dy) {
        checkedBodiesTmp.clear();
        checkedBodiesTmp.add(this);
        return canPushY(dy, mass, checkedBodiesTmp);
    }
    
    private boolean canPushY(double dy, int parentMass, Set<Body> checkedBodies) {
        double originalY = this.y;
        this.y += dy;
        Set<Body> bodies = world.getSpatialPartition().retrieve(this);
        bodies.removeAll(checkedBodies);
        iteratingBodiesTmp.clear();
        iteratingBodiesTmp.addAll(bodies);
        if (bodies.isEmpty()) {
            this.y = originalY;
            return true;
        }
        else {
            for (Body body : iteratingBodiesTmp) {
                checkedBodies.add(body);
                if (body != this && body.isRigid() 
                        && (this.collisionMask & body.getCollisionCategory()) != 0
                        && (this.collisionCategory & body.getCollisionMask()) != 0
                        && this.intersects(body) 
                        && (body.getMass() >= parentMass 
                        || !body.canPushY(dy, parentMass, checkedBodies))) {
                    this.y = originalY;
                    return false;
                }
            }
            this.y = originalY;
            return true;
        }
    }

    public void pushX(double dx) {
        checkedBodiesTmp.clear();
        checkedBodiesTmp.add(this);
        pushX(dx, mass, checkedBodiesTmp);
    }
    
    private void pushX(double dx, int parentMass, Set<Body> checkedBodies) {
        this.x += dx;
        Set<Body> bodies = world.getSpatialPartition().retrieve(this);
        bodies.removeAll(checkedBodies);
        iteratingBodiesTmp.clear();
        iteratingBodiesTmp.addAll(bodies);
        for (Body body : iteratingBodiesTmp) {
            if (body != this && body.isRigid() 
                    && (this.collisionMask & body.getCollisionCategory()) != 0
                    && (this.collisionCategory & body.getCollisionMask()) != 0
                    && body.intersects(this) && body.getMass() < parentMass) {
                checkedBodies.add(body);
                body.pushX(dx, parentMass, checkedBodies);
            }
        }
    }

    public boolean canPushX(double dx) {
        checkedBodiesTmp.clear();
        checkedBodiesTmp.add(this);
        return canPushX(dx, mass, checkedBodiesTmp);
    }

    private boolean canPushX(double dx, int parentMass, Set<Body> checkedBodies) {
        double originalX = this.x;
        this.x += dx;
        Set<Body> bodies = world.getSpatialPartition().retrieve(this);
        bodies.removeAll(checkedBodies);
        iteratingBodiesTmp.clear();
        iteratingBodiesTmp.addAll(bodies);
        if (bodies.isEmpty()) {
            this.x = originalX;
            return true;
        }
        else {
            for (Body body : iteratingBodiesTmp) {
                checkedBodies.add(body);
                if (body != this && body.isRigid() 
                        && (this.collisionMask & body.getCollisionCategory()) != 0
                        && (this.collisionCategory & body.getCollisionMask()) != 0
                        && body.intersects(this) 
                        && (body.getMass() >= parentMass 
                        || !body.canPushX(dx, parentMass, checkedBodies))) {
                    this.x = originalX;
                    return false;
                }
            }
            this.x = originalX;
            return true;
        }
    }    
    
    @Override
    public int hashCode() {
        return 3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Body)) {
            return false;
        }
        return this == obj;
    }
    
}
