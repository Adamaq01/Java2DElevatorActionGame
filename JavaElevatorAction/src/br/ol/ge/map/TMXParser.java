package br.ol.ge.map;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TMXParser class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class TMXParser {
    
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private DocumentBuilder db;

    public List<Layer> layers = new ArrayList<Layer>();
    public TileSets tileSets = new TileSets();
    
    // <map version="1.0" orientation="orthogonal" width="20" height="15" tilewidth="16" tileheight="16">
    public String orientation;
    public int width; // number of tiles
    public int height; // number of tiles
    public int tilewidth; // number of tiles
    public int tileheight; // number of tiles
    
    public void parseFromResource(String resource) throws Exception {
        clear();
        db = dbf.newDocumentBuilder();
        Document document = db.parse(getClass().getResourceAsStream(resource));
        Element map = document.getDocumentElement();
        orientation = map.getAttributes().getNamedItem("orientation").getNodeValue();
        width = Integer.parseInt(map.getAttributes().getNamedItem("width").getNodeValue());
        height = Integer.parseInt(map.getAttributes().getNamedItem("height").getNodeValue());
        tilewidth = Integer.parseInt(map.getAttributes().getNamedItem("tilewidth").getNodeValue());
        tileheight = Integer.parseInt(map.getAttributes().getNamedItem("tileheight").getNodeValue());
        // TILESETS
        NodeList tilesetNodes = map.getElementsByTagName("tileset");
        for (int i=0; i<tilesetNodes.getLength(); i++) {
            Node tileset = tilesetNodes.item(i);
            // <tileset firstgid="1021" name="Floor" tilewidth="16" tileheight="16">
            TileSet tileSetObj = new TileSet();
            tileSetObj.firstgid = Long.parseLong(tileset.getAttributes().getNamedItem("firstgid").getNodeValue());
            tileSetObj.name = tileset.getAttributes().getNamedItem("name").getNodeValue();
            tileSetObj.tilewidth = Integer.parseInt(tileset.getAttributes().getNamedItem("tilewidth").getNodeValue());
            tileSetObj.tileheight = Integer.parseInt(tileset.getAttributes().getNamedItem("tileheight").getNodeValue());
            if (tileset.getAttributes().getNamedItem("spacing") != null) {
                tileSetObj.spacing = Integer.parseInt(tileset.getAttributes().getNamedItem("spacing").getNodeValue());
            }
            // <image source="../Objects/Floor.png" width="336" height="624"/>
            NodeList images = ((Element) tileset).getElementsByTagName("image");
            Node image = images.item(0);
            Image imageObj = new Image();
            imageObj.source = image.getAttributes().getNamedItem("source").getNodeValue();
            imageObj.width = Integer.parseInt(image.getAttributes().getNamedItem("width").getNodeValue());
            imageObj.height = Integer.parseInt(image.getAttributes().getNamedItem("height").getNodeValue());
            imageObj.cols = imageObj.width / tileSetObj.tilewidth;
            imageObj.rows = imageObj.height / tileSetObj.tileheight;
            tileSetObj.image = imageObj;
            tileSets.add(tileSetObj);
        }
        // LAYERS
        NodeList layerNodes = map.getElementsByTagName("layer");
        for (int i=0; i<layerNodes.getLength(); i++) {
            Node layer = layerNodes.item(i);
            // <layer name="Tiles" width="20" height="15">
            Layer layerObj = new Layer();
            layerObj.name = layer.getAttributes().getNamedItem("name").getNodeValue();
            layerObj.width = Integer.parseInt(layer.getAttributes().getNamedItem("width").getNodeValue());
            layerObj.height = Integer.parseInt(layer.getAttributes().getNamedItem("height").getNodeValue());
            // <data encoding="csv">
            NodeList datas = ((Element) layer).getElementsByTagName("data");
            Node data = datas.item(0);
            Data dataObj = new Data();
            dataObj.encoding = data.getAttributes().getNamedItem("encoding").getNodeValue();
            dataObj.values = getDataValues(data.getTextContent());
            layerObj.data = dataObj;
            layerObj.setFirstNonZeroGid();
            layers.add(layerObj);
        }
    }
    
    public void clear() {
        layers.clear();
        tileSets.clear();
    }
    
    private static long[] getDataValues(String values) {
        String[] valuesArray = values.split(",");
        long[] ret = new long[valuesArray.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = Long.parseLong(valuesArray[i].trim());
        }
        return ret;
    }
    
    int[] tileRet = new int[4];
    public class TileSet {
        public long firstgid;
        public String name;
        public int tilewidth;
        public int tileheight;
        public int spacing;
        public Image image;

        public int[] get(long gid) {
            long i = gid - firstgid;
            int x = (int) (i % image.cols);
            int y = (int) (i / image.cols);
            tileRet[0] = x * (tilewidth + spacing); // x1
            tileRet[1] = y * (tileheight + spacing); // y1
            tileRet[2] = tileRet[0] + tilewidth; // x2
            tileRet[3] = tileRet[1] + tileheight; // y2
            return tileRet;
        }
        
        @Override
        public String toString() {
            return "TileSet{" + "firstgid=" + firstgid + ", name=" + name + ", tilewidth=" + tilewidth + ", tileheight=" + tileheight + ", image=" + image + '}';
        }
    }
    
    public class Image {
        public String source;
        public int width;
        public int height;
        public int cols;
        public int rows;
        public BufferedImage[] data;
        @Override
        public String toString() {
            return "Image{" + "source=" + source + ", width=" + width + ", height=" + height + '}';
        }
    }

    // <layer name="Tiles" width="20" height="15">
    public class Layer {
        public String name;
        public int width; // qtde de tiles
        public int height; // qtde de tiles
        public Data data;
        public long firstNonZeroGid;
        
        public long get(int x, int y) {
            return data.values[x + y * width];
        }

        public void set(int x, int y, long gid) {
            data.values[x + y * width] = gid;
        }
        
        public void setFirstNonZeroGid() {
            int di = 0;
            do {
                firstNonZeroGid = data.values[di++];
            }
            while (firstNonZeroGid == 0);
        }
        
        @Override
        public String toString() {
            return "Layer{" + "name=" + name + ", width=" + width + ", height=" + height + ", data=" + data + '}';
        }
    }
    
    //  <data encoding="csv">
    public class Data {
        public String encoding;
        public long[] values;
        
        @Override
        public String toString() {
            return "Data{" + "encoding=" + encoding + ", values=" + values + '}';
        }
    }
 
    public class TileSets {
        public NavigableMap<Long, TileSet> tileSetsMap = new TreeMap<Long, TileSet>();
        public void add(TileSet tileSet) {
            tileSetsMap.put(tileSet.firstgid, tileSet);
        }
        
        public TileSet get(long gid) {
            return tileSetsMap.floorEntry(gid).getValue(); 
        }
        
        public void clear() {
            tileSetsMap.clear();
        }
    }
    
}
