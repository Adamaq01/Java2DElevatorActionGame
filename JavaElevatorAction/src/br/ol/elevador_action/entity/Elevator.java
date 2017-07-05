package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import br.ol.elevador_action.scene.LevelScene;
import br.ol.g2d.Sprite;
import br.ol.ge.physics.Body;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Elevator class.
 * 
 * TODO
 * - ai (se nao tiver player) pode controlar elevador
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Elevator extends ElevadorActionEntity {

    public static enum ElevadorState { NONE, WAIT, UP, DOWN }
    private ElevadorState state = ElevadorState.NONE;
    private final Camera camera;
    private int directionY = 1; // -1 = up / +1 = down
    private int targetTopY;
    private long waitStartTime;
    private final int x, y, minTopY, maxTopY;
    private Sprite spriteCabinet;
    private Sprite spriteCable;
    private Body<Elevator> motor;
    private Cable primaryCable;
    private final Cabinet[] cabinets;
    private Cable[] secondaryCables;
    private final Rectangle[] rectsTmp = { new Rectangle(), new Rectangle() };
    private final List<Body> bodiesTmp = new ArrayList<Body>();
    
    public Elevator(ElevadorActionScene scene, Camera camera, int x, int y, int maxY, int cabinetsCount) {
        super(scene);
        this.camera = camera;
        this.x = x;
        this.y = y;
        this.minTopY = y + 2;
        this.maxTopY = maxY - 54 - (cabinetsCount - 1) * 96;
        this.targetTopY = minTopY;
        cabinets = new Cabinet[cabinetsCount];
        secondaryCables = null;
        int offsetY = 0; // (int) (48 * Math.random());
        // create secondary cables
        if (cabinetsCount > 1) {
            secondaryCables = new Cable[cabinetsCount - 1];
        }
        // create cabinets
        for (int i = 0; i < cabinets.length; i++) {
            if (i > 0) {
                secondaryCables[i - 1] = new Cable(x + 11, offsetY + y + i * 96 - 39);
            }
            cabinets[i] = new Cabinet(x, offsetY + y + i * 96);
        }
        createPrimaryCable();
        createMotor();
    }

    public ElevadorState getState() {
        return state;
    }

    public void changeState(ElevadorState state) {
        if (this.state != state) {
            ElevadorState oldState = this.state;
            this.state = state;
            elevatorStateChanged(oldState, state);
        }
    }
    
    public int getTopY() {
        return (int) cabinets[0].top.y;
    }

    public int getBottomY() {
        return (int) (cabinets[cabinets.length - 1].bottom.y 
                + cabinets[cabinets.length - 1].bottom.height);
    }

    private boolean canKeepDown() {
        return targetTopY < maxTopY;
    }

    private boolean canKeepUp() {
        return targetTopY > minTopY;
    }

    public Cabinet[] getCabinets() {
        return cabinets;
    }

    @Override
    public void init() {
        spriteCabinet = getG2D().getSpriteSheet().getSprite("elevator");
        spriteCable = getG2D().getSpriteSheet().getSprite("elevator_cable");
        setVisible(true);
        setZorder(1);
    }
    
    private void createMotor() {
        getScene().getWorld().addBody(motor = new Body<Elevator>(this, true, true, 3, x, cabinets[0].top.y, 5, 5, 512, 480));
    }
    
    private void createPrimaryCable() {
        primaryCable = new Cable(x + 11, y + 2);
    }
    
    private class Cable {
        Body<Elevator> body;

        public Cable(int x, int y) {
            getWorld().addBody(body = new Body<Elevator>(Elevator.this, true, true, 2, x, y - 2, 1, 44, 256, 742));
        }
        
        public void draw(Graphics2D g) {
            Rectangle rectangle = spriteCable.getRectangle();
            g.drawImage(spriteCable.getSheet().getImage(), (int) body.x, (int) (body.y + 2), (int) (body.x + 2), (int) (body.y + body.height)
                    , rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height, null);
        }
    }
    
    public class Cabinet {
        Body<Elevator> top;
        Body<Elevator> inside;
        Body<Elevator> bottom;

        public Cabinet(int x, int y) {
            getWorld().addBody(top = new Body<Elevator>(Elevator.this, true, true, 2, x, y + 2, 24, 6, 32, 966)); 
            getWorld().addBody(inside = new Body<Elevator>(Elevator.this, true, true, 2, x, y + 8, 24, 42, 64, 928));
            getWorld().addBody(bottom = new Body<Elevator>(Elevator.this, true, true, 2, x, y + 50, 24, 6, 128, 870));
        }
        
        public void draw(Graphics2D g) {
            spriteCabinet.draw(g, (int) top.x, (int) (top.y - 2));
        }
    }

    @Override
    public void update() {
        if (state == ElevadorState.NONE) {
            return;
        }

        // control elevator manually when player is inside cabinet
        if (canPlayerControl() && isKeyPressed(KeyEvent.VK_UP)) {
            up();
            return;
        }
        else if (canPlayerControl() && isKeyPressed(KeyEvent.VK_DOWN)) {
            // System.out.println("down " + System.currentTimeMillis() + " " + isKeyPressed(KeyEvent.VK_DOWN));
            down();
            return;
        }
        
        switch (state) {
            case WAIT: updateWait(); break;
            case UP: updateUp(); break;
            case DOWN: updateDown(); break;
        }

        updatePrimaryCable();
        checkSmashingPlayerOrEnemy();
    }

    private void checkSmashingPlayerOrEnemy() {
        for (Cabinet cabinet : cabinets) {
            if (motor.isPressing() && state == ElevadorState.UP) {
                rectsTmp[0].setRect(cabinet.top.x, cabinet.top.y - 1, cabinet.top.width, cabinet.top.height);
                rectsTmp[1].setRect(cabinet.bottom.x, cabinet.bottom.y - 1, cabinet.bottom.width, cabinet.bottom.height);
            }
            else if (motor.isPressing() && state == ElevadorState.DOWN) {
                rectsTmp[0].setRect(cabinet.top.x, cabinet.top.y + 1, cabinet.top.width, cabinet.top.height);
                rectsTmp[1].setRect(cabinet.bottom.x, cabinet.bottom.y + 1, cabinet.bottom.width, cabinet.bottom.height);
            }
            else {
                return;
            }
            
            for (Rectangle rectTmp : rectsTmp) {
                bodiesTmp.clear();
                bodiesTmp.addAll(getWorld().retrieve(rectTmp, true, false));
                for (Body body : bodiesTmp) {
                    if (body.getCollisionCategory() == 2) { // player
                        Player player = (Player) body.getOwner();
                        if (player.isBeenSmashed()) {
                            player.smash();
                        }
                    }
                    else if (body.getCollisionCategory() == 4) { // enemy
                        Enemy enemy = (Enemy) body.getOwner();
                        if (enemy.isBeenSmashed()) {
                            enemy.smash();
                        }
                    }
                }
            }
        }
    }
    
    private void updatePrimaryCable() {
        primaryCable.body.y = y + 2;
        primaryCable.body.height = cabinets[0].top.y - y;
    }
    
    private void updateWait() {
        if (System.currentTimeMillis() - waitStartTime < 3000) {
            return;
        }
        if (getBottomY() < camera.getArea().getY()) {
            down();
        }
        else if (getTopY() > camera.getArea().getY() + camera.getArea().getHeight()) {
            up();
        }
        else if (directionY > 0 && canKeepDown()) {
            down();
        }
        else if (directionY > 0) {
            up();
        }
        else if (directionY < 0 && canKeepUp()) {
            up();
        }
        else if (directionY < 0) {
            down();
        }
    }
    
    private void updateUp() {
        if (getTopY() <= targetTopY || getTopY() <= minTopY) {
            motor.setVelocityY(0);
            waitElevator();
            return;
        }
        motor.y = cabinets[cabinets.length - 1].bottom.y;
        motor.setVelocityY(-1);
    }
    
    private void updateDown() {
        if (getTopY() >= targetTopY || getTopY() > maxTopY) {
            motor.setVelocityY(0);
            waitElevator();
            return;
        }
        motor.y = cabinets[0].top.y;
        motor.setVelocityY(1);
    }
    
    @Override
    public void draw(Graphics2D g) {
        primaryCable.draw(g);
        if (secondaryCables != null) {
            for (Cable secondaryCable : secondaryCables) {
                secondaryCable.draw(g);
            }
        }
        for (Cabinet cabinet : cabinets) {
            cabinet.draw(g);
        }
    }
    
    public void waitElevator() {
        waitStartTime = System.currentTimeMillis();
        changeState(ElevadorState.WAIT);
    }

    public void down() {
        if (getTopY() >= maxTopY) {
            motor.setVelocityY(0);
            waitElevator();
            return;
        }
        if (getTopY() >= targetTopY) {
            targetTopY += 48;
        }
        if (targetTopY > maxTopY) {
            targetTopY = maxTopY;
        }
        changeState(ElevadorState.DOWN);
    }

    public void up() {
        // level cleared
        if (cabinets[cabinets.length - 1].bottom.y - 2 > getModel().getLastFloorY() && canPlayerControl()) {
            return;
        }
        else if (getTopY() <= minTopY) {
            motor.setVelocityY(0);
            waitElevator();
            return;
        }
        if (getTopY() <= targetTopY) {
            targetTopY -= 48;
        }
        if (targetTopY < minTopY) {
            targetTopY = maxTopY;
        }
        changeState(ElevadorState.UP);
    }

    private void elevatorStateChanged(ElevadorState oldState, ElevadorState newState) {
        switch (newState) {
            case UP: directionY = -1; break;
            case DOWN: directionY = 1; break;
        }
    }
    
    public boolean canPlayerControl() {
        Player player = ((LevelScene) getScene()).getPlayer();
        for (Cabinet cabinet : cabinets) {
            if (cabinet.inside.contains(player.getBody()) && player.isOnFloor() && !player.isDead()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnemyInside(Enemy enemy) {
        for (Cabinet cabinet : cabinets) {
            if (cabinet.inside.contains(enemy.getBody()) && !enemy.isDead()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isOutOfCameraArea() {
        return getBottomY() < camera.getArea().getY() 
                || getTopY() > camera.getArea().getY() + camera.getArea().getHeight();
    }
    
}
