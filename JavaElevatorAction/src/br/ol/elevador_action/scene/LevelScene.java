package br.ol.elevador_action.scene;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.elevador_action.entity.Bullet;
import br.ol.elevador_action.entity.Camera;
import br.ol.elevador_action.entity.Curtain;
import br.ol.elevador_action.entity.Elevator;
import br.ol.elevador_action.entity.Enemy;
import br.ol.elevador_action.entity.GameOver;
import br.ol.elevador_action.entity.HUD;
import br.ol.elevador_action.entity.LampFalling;
import br.ol.elevador_action.entity.LevelCleared;
import br.ol.elevador_action.entity.Player;
import br.ol.elevador_action.entity.RedDoor;
import br.ol.elevador_action.entity.Tilemap;
import br.ol.ge.core.Entity;
import br.ol.ge.map.TMXParser;
import br.ol.ge.physics.Body;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * LevelScene class.
 * 
 * Notes:
 * ======
 * 
 * layers (zorder):
 * ----------------
 * 0 = elevator background
 * 1 = elevators
 * 2 = player & enemy down stairs animation
 * 3 = background tilemap tmx
 * 4 = red door, door in, out animation (enemy/player)
 * 5 = enemies
 * 6 = player / level cleared
 * 7 = bullet / lamp
 * 
 * body collision:
 * ---------------
 * category                 mass       isRigid isDynamic   collidesWithCategories     sum
 *  1   = walls, block    = infinite = true  = false     = (2, 3                    = 5   )
 *  2   = player          = 1        = true  = true      = (1, 32, 128, 256         = 417 )
 *  4   = enemy           = 1        = true  = true      = (1, 32, 128, 256         = 417 )
 *  8   = player bullet   = 0        = false = true      = (---                     = 0   )
 *  16  = enemy bullet    = 0        = false = true      = (---                     = 0   )
 *  32  = elevator top    = 2        = true  = true      = (2, 4, 64, 128, 256, 512 = 966 )
 *  64  = elevator inside = 2        = true  = true      = (32, 128, 256, 512       = 928 )
 *  128 = elevator bottom = 2        = true  = true      = (2, 4, 32, 64, 256, 512  = 870 )
 *  256 = elevator cable  = 2        = true  = true      = (2, 4, 32, 64, 128, 512  = 742 )
 *  512 = elevator motor  = 3        = true  = true      = (32, 64, 128, 256        = 480 )
 * 1024 = lamp            = infinite = false = false     = (---                     = 0   )
 * 
 * animations name convention:
 * ---------------------------
 * player _ idle  _ left
 *  enemy _ walk  _ right
 *        _ smash _
 *        _ die   _
 *        _ down  _
 *        _ shoot _
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class LevelScene extends ElevadorActionScene {
    
    private Player player;
    private Camera camera;
    private LevelCleared levelCleared;
    private Curtain curtain;
    private GameOver gameOver;
    private HUD hud;
    private Tilemap tilemap;
    private LampFalling lampFalling;
    
    public LevelScene() {
    }

    public Player getPlayer() {
        return player;
    }

    public Camera getCamera() {
        return camera;
    }

    public HUD getHud() {
        return hud;
    }

    public LevelCleared getLevelCleared() {
        return levelCleared;
    }

    public Curtain getCurtain() {
        return curtain;
    }

    public GameOver getGameOver() {
        return gameOver;
    }

    public Tilemap getTilemap() {
        return tilemap;
    }

    public LampFalling getLampFalling() {
        return lampFalling;
    }

    @Override
    public void init() {
        curtain = new Curtain(this);
        curtain.init();
        gameOver = new GameOver(this);
        gameOver.init();
        hud = new HUD(this);
        hud.init();
    }
    
    @Override
    public void onActivated() {
        curtain.onActivated();
        gameOver.onActivated();
        hud.onActivated();
        
        getEntities().clear();
        player = new Player(this, hud);
        camera = new Camera(this, player);
        player.setCamera(camera);
        
        createAllElevators();
        addEntity(tilemap = new Tilemap(this, camera));
        createRedDoors();
        addEntity(lampFalling = new LampFalling(this));
        
        Enemy[] enemies = new Enemy[3];
        // add enemies
        for (int e = 0; e < enemies.length; e++) {
            Enemy enemy = new Enemy(this, 10000 + e * 2000);
            enemy.setCamera(camera);
            enemy.setPlayer(player);
            addEntity(enemy);
            enemies[e] = enemy;
        }

        addEntity(player);

        // add bullets
        for (Enemy enemy : enemies) {
            addEntity(enemy.getBullets()[0]);
        }
        for (Bullet bullet : player.getBullets()) {
            addEntity(bullet);
        }
        addEntity(levelCleared = new LevelCleared(this));
        levelCleared.setCamera(camera);
        
        addEntity(camera);
        System.gc();
        initAllEntities();
    }
    
    private void createAllElevators() {
        TMXParser.Layer layer = getTmxParser().layers.get(0);
        for (int row=0; row<layer.height; row++) {
            for (int col=0; col<layer.width; col++) {
                long gid = layer.get(col, row);
                if (gid == 6) {
                    createElevator(layer, col, row);
                }
            }                
        }
    }
    
    private void createElevator(TMXParser.Layer layer, int col, int row) {
        int startRow = row - 1;
        
        int cabinetsCount = 0;
        while (layer.get(col, row) == 6) {
            layer.set(col, row++, 5);
            cabinetsCount++;
        }
        
        do {
            row++;
        }
        while (layer.get(col, row) == 5);
        
        int height = (row - startRow + 1) * 8;
        Elevator elevator = new Elevator(this, camera, (col - 1) * 8, startRow * 8, startRow * 8 + height, cabinetsCount);
        Body<String> elevatorArea = new Body<String>("elevator_area", false, false, 0, (col - 1) * 8, startRow * 8, 24, height, 0, 0);
        getWorld().addBody(elevatorArea);
        addEntity(elevator);
    }
    
    private void createRedDoors() {
        for (Body body : getModel().getRedDoors()) {
            RedDoor redDoor = new RedDoor(this, body);
            addEntity(redDoor);
        }
    }
    
    @Override
    public void updateInternal() {
        super.updateInternal();  
        getWorld().update();
        updateAfterPhysics();
    }  
    
    @Override
    public void update() {
        curtain.update();
        gameOver.update();
        hud.update();
    }

    public void updateAfterPhysics() {
        for (Entity entity : getEntities()) {
            ElevadorActionEntity e = (ElevadorActionEntity) entity;
            e.updateAfterPhysics();
        }
    }

    @Override 
    public void draw(Graphics2D g) {
        g.translate(-camera.getArea().getX(), -camera.getArea().getY());
        super.draw(g);
        // getModel().getWorld().drawDebug(g);
        g.translate(camera.getArea().getX(), camera.getArea().getY());
        curtain.draw(g);
        gameOver.draw(g);
        hud.draw(g);
    }
    
    public void startElevators() {
        for (Entity entity : getEntities()) {
            if (entity instanceof Elevator) {
                Elevator elevator = (Elevator) entity;
                elevator.down();
            }
        }
    }

    private final Rectangle2D rectangleTmp = new Rectangle2D.Double();
    private final List<Body> doorsTmp = new ArrayList<Body>();
    
    public Body<String> getRandomBlueDoorPosition() {
        double ry = player.getBody().getY();
        ry = (ry > getWorld().getHeight() - 100) ? getWorld().getHeight() - 100 : ry;
        rectangleTmp.setRect(0, ry, getWorld().getWidth(), 100);
        Set<Body> bodies = getWorld().retrieve(rectangleTmp, false, true, Object.class, String.class);
        getModel().filterJustDoorBodies(bodies);
        doorsTmp.clear();
        doorsTmp.addAll(bodies);
        Body<String> randomDoor = null;
        if (!doorsTmp.isEmpty()) {
            Collections.shuffle(doorsTmp);
            for (Body doorTmp : doorsTmp) {
                if (!getModel().isBlueDoorOccupied(doorTmp)) {
                    randomDoor = doorTmp;
                    break;
                }
            }
        }
        return randomDoor;
    }
    
    public void showLevelCleared() {
        levelCleared.show();
    }

    public void showGameOver() {
        gameOver.show();
    }

    public void standByAllEnemies() {
        int t = 3000;
        for (Entity entity : getEntities()) {
            if (entity instanceof Enemy) {
                Enemy enemy = (Enemy) entity;
                enemy.standBy(t);
                t += 3000;
            }
        }
    }
    
}
