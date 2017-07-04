package br.ol.ge.spatial_partition;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * Area class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Area<T> extends Rectangle2D.Double {
    
    private final Set<Cell> cells = new HashSet<Cell>();
    private T owner;

    public Area() {
    }

    public Area(T owner) {
        this.owner = owner;
    }

    public Area(int x, int y, int width, int height) {
        setRect(x, y, width, height);
    }

    public Area(T owner, int x, int y, int width, int height) {
        setRect(x, y, width, height);
        this.owner = owner;
    }

    public Set<Cell> getCells() {
        return cells;
    }

    public T getOwner() {
        return owner;
    }
    
    public void setOwner(T owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return 3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Area)) {
            return false;
        }
        return this == obj;
    }
    
}
