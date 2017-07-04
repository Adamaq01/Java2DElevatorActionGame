package br.ol.elevador_action;

import br.ol.g2d.G2DContext;
import br.ol.ge.map.TMXParser;
import br.ol.ge.map.TMXParser.Layer;
import br.ol.ge.physics.Body;
import br.ol.ge.physics.World;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ElevadorActionModel class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class ElevadorActionModel {
    
    public static enum GameState { INITIALIZING, OL_PRESENTS, TITLE, PLAYING, GAME_OVER }
    private GameState gameState = GameState.INITIALIZING;
   
    private final ElevadorActionGame game;
    
    // G2D
    private G2DContext g2d;
    
    // TMX parser
    private TMXParser tmxParser;
    
    // 2D physics world
    private World world;
    private final List<Body> redDoors = new ArrayList<Body>();
    private final Set<Body> occupiedBlueDoors = new HashSet<Body>();
    private final TreeMap<Integer, Body> floorsY = new TreeMap<Integer, Body>();
    private int lastFloorY;
    
    // lamp
    private boolean lampHit;
    private long lightsOffStartTime;
    
    // HUD
    private int level;
    private int lives;
    private int score = 0;
    private int hiscore = 10000;

    public ElevadorActionModel(ElevadorActionGame game) {
        this.game = game;
        initG2D();
        initTMXParser();
    }
    
    private void initG2D() {
        try {
            g2d = new G2DContext();
            g2d.loadFromResource("/res/ea.g2d");
        } catch (Exception ex) {
            Logger.getLogger(ElevadorActionModel.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    private void initTMXParser() {
        tmxParser = new TMXParser();
    }
    
    public GameState getGameState() {
        return gameState;
    }

    public void changeGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public G2DContext getG2D() {
        return g2d;
    }

    public TMXParser getTMXParser() {
        return tmxParser;
    }

    public World getWorld() {
        return world;
    }

    public List<Body> getRedDoors() {
        return redDoors;
    }
    
    public boolean isBlueDoorOccupied(Body body) {
        return occupiedBlueDoors.contains(body) || redDoors.contains(body);
    }

    public void markDoorOccupied(Body body, boolean occupied) {
        if (occupied) {
            occupiedBlueDoors.add(body);
        }
        else {
            occupiedBlueDoors.remove(body);
        }
    }

    public Body getClosestFloorY(int fy) {
        Body b = null;
        try {
            b = floorsY.ceilingEntry(fy).getValue();
        }
        catch (Exception e) {
            System.out.println("e = " + e);
        }
        return b;
    }

    public int getLastFloorY() {
        return lastFloorY;
    }
    
    public boolean isLampHit() {
        return lampHit;
    }

    public void setLampHit(boolean lampHit) {
        this.lampHit = lampHit;
    }

    public boolean isLightsOn() {
        return System.currentTimeMillis() - lightsOffStartTime > 5000;
    }

    public void setLightsOff() {
        lightsOffStartTime = System.currentTimeMillis();
        lampHit = false;
    }

    // --- HUD ---
    
    public int getLives() {
        return lives;
    }

    public void decLives() {
        lives--;
        lives = lives < 0 ? 0 : lives;
    }

    public String getScore() {
        String scoreStr = "000000" + score;
        scoreStr = scoreStr.substring(scoreStr.length() - 6, scoreStr.length());
        return scoreStr;
    }
    
    public void addScore(int points) {
        score += points;
    }

    public String getHiscore() {
        String hiscoreStr = "000000" + hiscore;
        hiscoreStr = hiscoreStr.substring(hiscoreStr.length() - 6, hiscoreStr.length());
        return hiscoreStr;
    }
    
    public void updateHiscore() {
        if (score > hiscore) {
            hiscore = score;
        }
        score = 0;
    }
    
    // --- level loader ---
    
    private void loadLevel(int levelIndex) {
        try {
            
            // TODO
            levelIndex = 1;
            
            tmxParser.parseFromResource("/res/level_" + levelIndex + ".tmx");
            loadLevelImages();
            createPhysicsWorld();
            selectRedDoorsRandomically();
        } catch (Exception ex) {
            Logger.getLogger(ElevadorActionModel.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    private void loadLevelImages() throws Exception {
        for (TMXParser.TileSet tileSet : tmxParser.tileSets.tileSetsMap.values()) {
            String imageFile = tileSet.image.source.replace("../", "");
            imageFile = imageFile.replace(".png", "");
            tileSet.image.data = new BufferedImage[2];
            tileSet.image.data[0] = g2d.getSpriteSheet().getSprite(imageFile + "_lights_on").createImage();
            tileSet.image.data[1] = g2d.getSpriteSheet().getSprite(imageFile + "_lights_off").createImage();
        }
    }
    
    private void createPhysicsWorld() {
        world = new World(tmxParser.width * tmxParser.tilewidth, tmxParser.height * tmxParser.tileheight, tmxParser.tilewidth, tmxParser.tileheight);
        Layer layer = tmxParser.layers.get(0);
        for (int row=0; row<layer.height; row++) {
            for (int col=0; col<layer.width; col++) {
                long gid = layer.get(col, row);
                if (gid == 0) {
                    continue;
                }
                // roof
                if (col == 13 && (row > 10 && row < 16)) {
                    Body floor = new Body(this, true, false, 0, col * 8, row * 8 + 2, 8, 8 - 2, 1, 6);
                    world.addBody(floor);
                }
                // floor position
                else if (gid == 37)  {
                    int fy = row * 8;
                    lastFloorY = fy > lastFloorY ? fy : lastFloorY;
                    Body<String> floor_position = new Body(this, false, false, 0, col * 8, row * 8 + 2, 8, 8 - 2, 1, 6);
                    world.addBody(floor_position);
                    floorsY.put(fy, floor_position);
                }
                // just left walls
                else if (gid == 20 || gid == 41 || gid == 86) {
                    Body block = new Body(this, true, false, 0, col * 8, row * 8 + 2, 4, 8 - 2, 1, 6);
                    world.addBody(block);
                }
                // walls, floors, etc
                else if (gid == 3 || gid == 4 || gid == 7 || gid == 11 || gid == 14 || gid == 18 || gid == 19 
                        || gid == 20 || gid == 26 || gid == 32 || gid == 33 || gid == 36 || gid == 39 
                        || gid == 41 || gid == 55 || gid == 56 ||  gid == 57 || gid == 58 || gid == 69 
                        || gid == 70 || gid == 71 || gid == 72 || gid == 73 || gid == 75 || gid == 76 
                        || gid == 77 || gid == 78 || gid == 81 || gid == 82 || gid == 86 || gid == 87 
                        || gid == 88 || gid == 89 || gid == 90 ) {
                    Body block = new Body(this, true, false, 0, col * 8, row * 8 + 2, 8, 8 - 2, 1, 6);
                    world.addBody(block);
                }
                // lamp
                else if (gid == 24)  {
                    Body<String> lamp = new Body<String>("lamp_" + col + "," + row, false, false, 0, col * 8, row * 8, 8, 7, 0, 0);
                    world.addBody(lamp);
                }
                // door left
                else if (gid == 30)  {
                    Body<String> doorPosition = new Body<String>("door_left", false, false, 0, col * 8, (row - 1) * 8 - 4, 8, 8, 0, 0);
                    world.addBody(doorPosition);
                    if (layer.get(col + 1, row) == 83) {
                        layer.set(col, row, 83);
                    }
                }
                // door right
                else if (gid == 31)  {
                    Body<String> doorPosition = new Body<String>("door_right", false, false, 0, (col - 0) * 8, (row - 1) * 8 - 4, 8, 8, 0, 0);
                    world.addBody(doorPosition);
                    if (layer.get(col + 1, row) == 83) {
                        layer.set(col, row, 83);
                    }
                }
                // stair top left
                else if (gid == 49)  {
                    Body<String> stairTopLeft = new Body<String>("stair_top_left", false, false, 0, col * 8 + 4, row * 8, 1, 8, 0, 0);
                    world.addBody(stairTopLeft);
                }
                // stair bottom left
                else if (gid == 66)  {
                    Body<String> stairBottomLeft = new Body<String>("stair_bottom_left", false, false, 0, col * 8 + 4, row * 8, 1, 8, 0, 0);
                    world.addBody(stairBottomLeft);
                }
                // stair top right
                else if (gid == 54)  {
                    Body<String> stairTopRight = new Body<String>("stair_top_right", false, false, 0, col * 8 + 4, row * 8, 1, 8, 0, 0);
                    world.addBody(stairTopRight);
                }
                // stair bottom right
                else if (gid == 67)  {
                    Body<String> stairBottomRight = new Body<String>("stair_bottom_right", false, false, 0, col * 8 + 4, row * 8, 1, 8, 0, 0);
                    world.addBody(stairBottomRight);
                }
            }                
        }
    }
    
    public void filterJustDoorBodies(Set<Body> bodies) {
        Iterator<Body> i = bodies.iterator();
        while (i.hasNext()) {
            Body body = i.next();
            if (!body.getOwner().toString().startsWith("door_")) {
                i.remove();
            }
        }
    }
    
    private void selectRedDoorsRandomically() {
        redDoors.clear();
        int redDoorsNumber = 5;
        int areaHeight = world.getHeight() / redDoorsNumber;
        Rectangle recTmp = new Rectangle();
        List<Body> allDoors = new ArrayList<Body>();
        for (int n = 0; n < redDoorsNumber; n++) {
            allDoors.clear();
            recTmp.setRect(0, n * areaHeight, world.getWidth(), areaHeight);
            Set<Body> bodies = world.retrieve(recTmp, false, true, Object.class, String.class);
            filterJustDoorBodies(bodies);
            if (!bodies.isEmpty()) {
                allDoors.addAll(bodies);
                Body randomRedDoor = allDoors.remove((int) (allDoors.size() * Math.random()));
                redDoors.add(randomRedDoor);
            }
        }
        allDoors.clear();
        System.gc();
    }
    
    // --- game flow ---
    
    public void startGame() {
        lives = 3;
        level = 1;
        loadLevel(level);
        game.changeScene("level");
    }
    
    public void backToTitle() {
        updateHiscore();
        game.changeScene("title");
    }

    public void nextLevel() {
        loadLevel(++level);
        game.changeScene("level");
    }
    
}
