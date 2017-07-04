package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.ge.map.TMXParser;
import br.ol.ge.map.TMXParser.Layer;
import br.ol.ge.spatial_partition.Area;
import br.ol.ge.spatial_partition.GridSpatialPartition;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tilemap class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Tilemap extends ElevadorActionEntity {
    
    private Camera camera;
    private GridSpatialPartition tilesSpatialPartition;
 
    private Map<Long, BufferedImage> tileImagesLightsOn = new HashMap<Long, BufferedImage>();
    private Map<Long, BufferedImage> tileImagesLightsOff = new HashMap<Long, BufferedImage>();

    public Tilemap(ElevadorActionScene scene, Camera camera) {
        super(scene);
        this.camera = camera;
    }

    public GridSpatialPartition getTilesSpatialPartition() {
        return tilesSpatialPartition;
    }

    public Map<Long, BufferedImage> getTileImages() {
        return getModel().isLightsOn() ? tileImagesLightsOn : tileImagesLightsOff;
    }

    @Override
    public void onActivated() {
        try {
            setZorder(3);
            setVisible(true);
            Layer layer = getTmxParser().layers.get(0);
            tilesSpatialPartition = new GridSpatialPartition(layer.width * 8, layer.height * 8, 8, 8);
            cacheAllTileImages();
            createAllTiles();
            System.gc();
        } catch (Exception ex) {
            Logger.getLogger(Tilemap.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }

    private void cacheAllTileImages() throws Exception {
        cacheTileImages(0, tileImagesLightsOn);
        cacheTileImages(1, tileImagesLightsOff);
    }    
    
    private void cacheTileImages(int index, Map<Long, BufferedImage> tileImages) {
        tileImages.clear();
        for (TMXParser.TileSet tileSet : getTmxParser().tileSets.tileSetsMap.values()) {
            BufferedImage img = tileSet.image.data[index];
            int cols = img.getWidth() / (tileSet.tilewidth + tileSet.spacing);
            int rows = img.getHeight() / (tileSet.tileheight + tileSet.spacing);
            long gid = tileSet.firstgid;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    BufferedImage tileImg = new BufferedImage(tileSet.tilewidth, tileSet.tileheight, BufferedImage.TYPE_INT_ARGB);
                    int sx1 = col * (tileSet.tilewidth + tileSet.spacing);
                    int sy1 = row * (tileSet.tileheight + tileSet.spacing);
                    int sx2 = sx1 + tileSet.tilewidth;
                    int sy2 = sy1 + tileSet.tileheight;
                    tileImg.getGraphics().drawImage(img, 0, 0, tileSet.tilewidth, tileSet.tileheight, sx1, sy1, sx2, sy2, null);
                    tileImages.put(gid++, tileImg);
                }
            }
        }
    }
    
    private void createAllTiles() {
        for (Layer layer : getTmxParser().layers) {
            for (int y=0; y<layer.height; y++) {
                for (int x=0; x<layer.width; x++) {
                    long gid = layer.get(x, y);
                    if (gid == 0) {
                        continue;
                    }
                    Area<Long> area = new Area<Long>(x * 8, y * 8, 8, 8);
                    area.setOwner(gid);
                    tilesSpatialPartition.update(area);                    
                }                
            }
        }
    }
    
    @Override
    public void draw(Graphics2D g) {
        Set<Area> retrievedAreas = tilesSpatialPartition.retrieve(camera.getArea());
        for (Area<Long> ra : retrievedAreas) {
            BufferedImage image = getTileImages().get(ra.getOwner());
            g.drawImage(image, (int) ra.x, (int) ra.y, null);
        }        
    }
    
}
