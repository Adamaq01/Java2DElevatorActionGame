package br.ol.ge.spatial_partition;

import java.util.HashSet;
import java.util.Set;

/**
 * Cell class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Cell<T extends Area> {
    
    private int col;
    private int row;
    private Set<T> areas = new HashSet<T>();

    public Cell(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Set<T> getAreas() {
        return areas;
    }

    @Override
    public String toString() {
        return "Cell{" + "col=" + col + ", row=" + row + ", areas=" + areas + '}';
    }
    
}
