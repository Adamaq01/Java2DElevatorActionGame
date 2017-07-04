package br.ol.ge.spatial_partition;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * GridSpatialPartition class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class GridSpatialPartition<T extends Area> {

    private static final boolean DRAW_DEBUG_GRID = false;
    private final int spaceWidth;
    private final int spaceHeight;
    private final int cellWidth;
    private final int cellHeight;
    private final int cols;
    private final int rows;
    private final Cell[][] cells; // [row][col]
    private final Set<T> retrievedAreas = new HashSet<T>();
    
    public GridSpatialPartition(int spaceWidth, int spaceHeight, int cellWidth, int cellHeight) {
        this.spaceWidth = spaceWidth;
        this.spaceHeight = spaceHeight;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        cols = (int) Math.ceil(spaceWidth / cellWidth);
        rows = (int) Math.ceil(spaceHeight / cellHeight);
        cells = new Cell[rows][cols];
        createAllCells();
    }
    
    private void createAllCells() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cells[row][col] = new Cell(col, row);
            }
        }
    }
    
    public int getSpaceWidth() {
        return spaceWidth;
    }

    public int getSpaceHeight() {
        return spaceHeight;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }
    
    public Cell getCell(int col, int row) {
        if (col < 0 || row < 0 || col > cols - 1 || row > rows - 1) {
            return null;
        }
        return cells[row][col];
    }
    
    public void remove(Area area) {
        for (Object obj : area.getCells()) {
            Cell cell = (Cell) obj;
            cell.getAreas().remove(area);
        }
        area.getCells().clear();
    }
    
    public void update(Area area) {
        remove(area);
        add(area);
    }    

    private void add(Area area) {
        int col1 = (int) (area.getX() / cellWidth);
        int row1 = (int) (area.getY() / cellHeight);
        int col2 = (int) ((area.x + area.width - 1) / cellWidth);
        int row2 = (int) ((area.y +  area.height - 1) / cellHeight);
        for (int row = row1; row <= row2; row++) {
            for (int col = col1; col <= col2; col++) {
                Cell cell = getCell(col, row);
                if (cell != null) {
                    cell.getAreas().add(area);
                    area.getCells().add(cell);
                }
            }
        }
    }
       
    public Set<T> retrieve(Rectangle area) {
        return retrieve(area.x, area.y, area.width, area.height);
    }

    public Set<T> retrieve(Rectangle2D area) {
        return retrieve((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
    }
    
    public Set<T> retrieve(int x, int y, int w, int h) {
        retrievedAreas.clear();
        int col1 = x / cellWidth;
        int row1 = y / cellHeight;
        int col2 = (x + w - 1) / cellWidth;
        int row2 = (y + h - 1) / cellHeight;
        for (int row = row1; row <= row2; row++) {
            for (int col = col1; col <= col2; col++) {
                Cell cell = getCell(col, row);
                if (cell != null) {
                    retrievedAreas.addAll(cell.getAreas());
                }
            }
        }
        return retrievedAreas;
    }

    public void drawDebug(Graphics2D g) {
        Set<T> all = retrieve(0, 0, spaceWidth, spaceHeight);
        for (T t : all) {
            g.draw(t);
        }
        if (DRAW_DEBUG_GRID) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    g.drawRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }
    
}
