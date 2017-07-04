package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import static br.ol.elevador_action.entity.Player.PlayerState.*;
import br.ol.elevador_action.scene.LevelScene;
import br.ol.g2d.Animation;
import br.ol.ge.input.Keyboard;
import br.ol.ge.physics.Body;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.*;
import java.util.Set;
/**
 * Player class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Player extends ElevadorActionEntity {
    
    private Camera camera;
    private HUD hud;
    
    public static enum PlayerState { START, IDLE, WALK, JUMP, DOWN, SMASH, DIE
        , DOWN_STAIR, UP_STAIR, RED_DOOR_IN, RED_DOOR_OUT, STAND_BY, LEVEL_CLEARED, NONE }
    
    private PlayerState state;
    private int direction;
    private int jumpDirection;
    private Body<Player> body;
    private Body<Player> lastFloorBodyPosition;
    private int diedYPosition;
    private Animation bodyAnimation;
    private final Point bodyAnimationPosition = new Point();
    private Animation shotAnimation;
    private final Point shotAnimationPosition = new Point();
    private final Rectangle[] rectsTmp = { new Rectangle(), new Rectangle() };
    private final Bullet[] bullets = new Bullet[3];
    private long holdingDocStartTime;
    private Body<String> downStairInfo;
    private Body<String> upStairInfo;
    private Body<RedDoor> redDoorInfo;
    private long standByStartTime;
    
    public Player(ElevadorActionScene scene, HUD hud) {
        super(scene);
        this.hud = hud;
    }

    public Bullet[] getBullets() {
        return bullets;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        // create bullets
        for (int i = 0; i < bullets.length; i++) {
            bullets[i] = new Bullet(getScene(), camera, 4); // 4 = enemy
        }
    }

    public PlayerState getState() {
        return state;
    }

    public Body<Player> getBody() {
        return body;
    }

    public void changeState(PlayerState state) {
        if (this.state != state) {
            PlayerState oldState = this.state;
            this.state = state;
            playerStateChanged(oldState, state);
        }
    }

    @Override
    public void init() {
        direction = -1;
        bodyAnimation = getG2D().getAnimations().get("player_walk_normal_left");
        shotAnimation = getG2D().getAnimations().get("player_shot_left");
        body = new Body<Player>(this, true, true, 1, 120, 112, 10, 24, 2, 417);
        lastFloorBodyPosition = new Body<Player>(this, true, true, 1, 120, body.getY() + body.getHeight(), 1, 2, 2, 417);
        body.setAffectedByGravity(false);
        getScene().getWorld().addBody(body);
        getScene().getWorld().addBody(lastFloorBodyPosition);
        setZorder(6);
    }

    @Override
    public void onActivated() {
        
        // test
//        state = START;
//        setVisible(true);
//        setStartSetup();
        
        start();
    }

    @Override
    public void update() {
        checkLevelCleared();
        ensureStayGluedElevatorFloor();
        switch (state) {
            case START: updateStart(); break;
            case IDLE: updateIdle(); break;
            case WALK: updateWalk(); break;
            case JUMP: updateJump(); break;
            case DOWN: updateDown(); break;
            case SMASH: updateSmash(); break;
            case DIE: updateDie(); break;
            case DOWN_STAIR: updateDownStair(); break;
            case UP_STAIR: updateUpStair(); break;
            case RED_DOOR_IN: updateRedDoorIn(); break;
            case RED_DOOR_OUT: updateRedDoorOut(); break;
            case STAND_BY: updateStandBy(); break;
            case LEVEL_CLEARED: updateLevelCleared(); break;
            case NONE: updateNone(); break;
        }
        shotAnimation.update(1000 / 45);
        bodyAnimation.update(1000 / 45);
    }

    @Override
    public void updateAfterPhysics() {
        ensureNotGetStuckedFloor();
        updateAnimation();
    }

    private void checkLevelCleared() {
        if (state == LEVEL_CLEARED || state == STAND_BY || state == NONE || isDied()) {
            return;
        }
        if (body.getY() + body.getHeight() >= getModel().getLastFloorY() + 50) {
            levelCleared();
        }
    }
    
    private void ensureStayGluedElevatorFloor() {
        if (body.isRigid() && isOnFloor()) {
            body.addVelocityY(10);
            checkLandTooHigh();
        }
    }
    
    private void checkLandTooHigh() {
        if (!body.isRigid()) {
            return;
        }
        double lastHeight = lastFloorBodyPosition.y;
        lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);
        if (lastFloorBodyPosition.y - lastHeight > 24) {
            die((int) lastHeight);
        }
    }
    
    private void ensureNotGetStuckedFloor() {
        if (!body.isRigid()) {
            return;
        }
        boolean checkAgain = true;
        while (checkAgain) {
            checkAgain = false;
            rectsTmp[0].setRect(body.x, body.y + body.height - 4, body.width, 4);
            Set<Body> bodies = getWorld().retrieve(rectsTmp[0], true, false);
            for (Body b : bodies) {
                long cc = b.getCollisionCategory();
                if (cc == 1 || cc == 32 || cc == 128) {
                    body.translate(0, -1);
                    checkAgain = true;
                    break;
                }
            }
        }
    }

    private void updateAnimation() {
        updateBodyAnimation();
        updateShotAnimation();
    }
    
    private void updateBodyAnimation() {
        String animationName = "";
        String stateStr = state.toString().toLowerCase();
        String directionStr = direction > 0 ? "right" : "left";
        String holdingDoc = isHoldingDoc() ? "doc" : "normal";
        switch (state) {
            case NONE: 
            case STAND_BY: 
            case START: 
                return;
            case SMASH: 
                animationName = "player_" + stateStr;
                break;
            case DIE: 
            case DOWN_STAIR: 
            case UP_STAIR: 
            case RED_DOOR_IN: 
            case RED_DOOR_OUT: 
                animationName = "player_" + stateStr + "_" + directionStr;
                break;
            case LEVEL_CLEARED: 
                stateStr = body.getVelocityX() != 0 ? "walk" : "idle";
            case IDLE: 
            case WALK: 
            case JUMP: 
            case DOWN: 
                animationName = "player_" + stateStr + "_" + holdingDoc + "_" + directionStr;
                break;
        }
        // System.out.println("animationName: " + animationName);
        Animation animation = getG2D().getAnimations().get(animationName);
        if (!(bodyAnimation.equals(animation))) {
            bodyAnimation = animation;
            if (!bodyAnimation.playing) {
                bodyAnimation.stop();
                bodyAnimation.play();
            }
        }
        bodyAnimationPosition.x = (int) body.x;
        bodyAnimationPosition.y = (int) body.y;
    }
    
    private void updateShotAnimation() {
        String directionStr = direction > 0 ? "right" : "left";
        Animation animation = getG2D().getAnimations().get("player_shot_" + directionStr);
        animation.currentFrameIndex = shotAnimation.currentFrameIndex;
        animation.playing = shotAnimation.playing;
        shotAnimation = animation;
        switch (state) {
            case NONE: 
            case LEVEL_CLEARED: 
            case STAND_BY: 
            case START: 
            case SMASH: 
            case DIE: 
            case DOWN_STAIR: 
            case UP_STAIR: 
            case RED_DOOR_IN: 
            case RED_DOOR_OUT: 
                shotAnimationPosition.x = -50;
                shotAnimationPosition.y = -50;
                break;
            case IDLE: 
            case WALK: 
            case JUMP: 
                updateShotAnimationPosition(direction > 0 ? 5 : -9, 4);
                break;
            case DOWN: 
                updateShotAnimationPosition(direction > 0 ? 5 : -9, 4);
                break;
        }
    }
    
    private void updateShotAnimationPosition(double tx, double ty) {
        shotAnimationPosition.x = (int) (body.x + tx);
        shotAnimationPosition.y = (int) (body.y + ty);
    }
    
    private void updateInput() {
        if (isKeyPressed(KeyEvent.VK_1)) {
            getScene().changeEntityZOrder(this, -1);
        }
        if (isKeyPressed(KeyEvent.VK_2)) {
            getScene().changeEntityZOrder(this, 1);
        }
        
        updateUpDownStairInfo();
        updateRedDoorInfo();
        
        if (state == IDLE && redDoorInfo != null 
                && ((RedDoor) redDoorInfo.getOwner()).getDirection() > 0 && isKeyDown(VK_LEFT)) {
            redDoorIn(1);
        }
        else if (state == IDLE && redDoorInfo != null 
                && ((RedDoor) redDoorInfo.getOwner()).getDirection() < 0 && isKeyDown(VK_RIGHT)) {
            redDoorIn(-1);
        }
        else if (upStairInfo != null && isKeyDown(VK_UP)) {
            upStair();
        }
        else if (downStairInfo != null && isKeyDown(VK_DOWN)) {
            downStair();
        }
        else if (isKeyDown(VK_LEFT) && isKeyDown(VK_DOWN)) {
            down(-1);
        }
        else if (isKeyDown(VK_RIGHT) && isKeyDown(VK_DOWN)) {
            down(1);
        }
        else if (isKeyDown(VK_DOWN)) {
            down(0);
        }
        else if (isKeyDown(VK_LEFT) && isKeyPressed(VK_X)) {
            jump(-1);
        }
        else if (isKeyDown(VK_RIGHT) && isKeyPressed(VK_X)) {
            jump(1);
        }
        else if (isKeyPressed(VK_X)) {
            jump(0);
        }
        else if (isKeyDown(VK_LEFT)) {
            walk(-1);
        }
        else if (isKeyDown(VK_RIGHT)) {
            walk(1);
        }
        else {
            idle();
        }
        
        updateInputShot();
    }
    
    private void updateInputShot() {
        if (isKeyPressed(VK_Z)) {
            shot();
        }
    }
    
    private void updateStart() {
        if (bodyAnimation.playing) {
            return;
        }
        ((LevelScene) getScene()).startElevators();
        setStartSetup();
        idle();
        updateIdle();
    }

    private void updateIdle() {
        updateInput();
    }

    private void updateWalk() {
        updateInput();
    }

    private void updateJump() {
        if (isOnFloor()) {
            body.y -= 4;
            idle();
            return;
        }
        body.setVelocityX(jumpDirection);
        updateInputShot();        
    }

    private void updateDown() {
        updateInput();
        if (state != DOWN) {
            body.y -= 4;
        }
    }

    private void updateSmash() {
        if (bodyAnimation.playing) {
            return;
        }
        body.setRigid(false);
        lastFloorBodyPosition.setRigid(false);
        setVisible(false);
        camera.setLocked(true);
        standBy();
    }

    private void updateDie() {
        if (bodyAnimation.playing) {
            return;
        }
        body.setRigid(false);
        lastFloorBodyPosition.setRigid(false);
        setVisible(false);
        camera.setLocked(true);
        standBy();
    }
    
    private void updateDownStair() {
        if (bodyAnimation.playing) {
            return;
        }
        if (direction < 0) {
            body.x += 48;
            body.y += 32;
        }
        else {
            body.x += 0;
            body.y += 16;
        }
        direction = -direction;
        body.setRigid(true);
        body.setAffectedByGravity(true);
        lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);        
        idle();
    }
    
    private void updateUpStair() {
        if (bodyAnimation.playing) {
            return;
        }
        if (direction < 0) {
            body.x += 8;
            body.y -= 16;
        }
        else {
            body.x += 40;
            body.y -= 30;
        }
        body.setRigid(true);
        body.setAffectedByGravity(true);
        lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);        
        direction = -direction;
        idle();
    }
    
    private void updateRedDoorIn() {
        if (bodyAnimation.playing) {
            return;
        }
        redDoorOut();
    }
    
    private void updateRedDoorOut() {
        if (bodyAnimation.playing) {
            return;
        }
        if (direction < 0) {
            body.x += -11;
            body.y += 6;
        }
        else {
            body.x += 16;
            body.y += 6;
        }
        ((RedDoor) redDoorInfo.getOwner()).addScorePoint();
        holdingDocStartTime = System.currentTimeMillis();
        body.setRigid(true);
        body.setAffectedByGravity(true);
        lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);        
        idle();
    }

    private int updateStandByInstructionPointer;
    
    private void updateStandBy() {
        LevelScene scene = (LevelScene) getScene();
        switch (updateStandByInstructionPointer) {
            case 0:
                if (System.currentTimeMillis() - standByStartTime < 2000) {
                    return;
                }
                updateStandByInstructionPointer = 1;
            case 1:
                if (getModel().getLives() == 1) {
                    nextLife(0, 0);
                    return;
                }
                scene.getCurtain().close();
                updateStandByInstructionPointer = 2;
            case 2:
                if (scene.getCurtain().isOpeningOrClosing()) {
                    return;
                }
                scene.standByAllEnemies();
                Body floorPosition = getModel().getClosestFloorY(diedYPosition - 28);
                nextLife((int) (floorPosition.getX() - body.getWidth() - 2), (int) (floorPosition.getY() - body.getHeight()));
                camera.updatePosition(body);
                scene.getCurtain().open();
        }
    }
    
    private void updateLevelCleared() {
        if (body.getX() > getControllingElevator().x) {
            direction = -1;
            body.setVelocityX(direction);
            return;
        }
        else if (body.getY() + body.getHeight() < getModel().getLastFloorY() + 50 ) {
            return;
        }
        setVisible(false);
        body.setRigid(false);
        body.setVelocityX(0);
        ((LevelScene) getScene()).showLevelCleared();
        changeState(NONE);
    }

    private void updateNone() {
        // do nothing
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.translate(shotAnimationPosition.x, shotAnimationPosition.y);
        shotAnimation.draw(g);
        g.translate(-shotAnimationPosition.x, -shotAnimationPosition.y);
        
        g.translate(bodyAnimationPosition.x, bodyAnimationPosition.y);
        bodyAnimation.draw(g);
        g.translate(-bodyAnimationPosition.x, -bodyAnimationPosition.y);
        
        // debug
        //g.setColor(Color.RED);
        //g.draw(body);
        //g.setColor(Color.BLUE);
        //g.draw(rectsTmp[0]);
    }

    private void playerStateChanged(PlayerState oldState, PlayerState state) {
        // do nothing
    }
    
    public boolean isBeenSmashed() {
        boolean ceil = false, floor = false;
        rectsTmp[0].setRect(body.x, body.y - 1, body.width, body.height);
        rectsTmp[1].setRect(body.x, body.y + 1, body.width, body.height);
        for (int i = 0; i < rectsTmp.length; i++) {
            Rectangle rectTmp = rectsTmp[i];
            Set<Body> bodies = getWorld().retrieve(rectTmp, true, false);
            for (Body b : bodies) {
                long cc = b.getCollisionCategory();
                if (cc == 1 || cc == 32 || cc == 128) {
                    if (i == 0) {
                        ceil = true;
                    }
                    else {
                        floor = true;
                    }
                }
            }
        }
        return ceil && floor;
    }

    public void updateUpDownStairInfo() {
        downStairInfo = null;
        upStairInfo = null;
        Set<Body> bodies = getWorld().retrieve(body, false, true, Object.class, String.class);
        for (Body b : bodies) {
            String stairInfo = b.getOwner().toString();
            if (body.intersects(b) && stairInfo.startsWith("stair_top")) {
                downStairInfo = b;
            }
            else if (body.intersects(b) && stairInfo.startsWith("stair_bottom")) {
                upStairInfo = b;
            }
        }
    }
    
    public void updateRedDoorInfo() {
        redDoorInfo = null;
        Set<Body> bodies = getWorld().retrieve(body, false, true, Object.class, RedDoor.class);
        for (Body b : bodies) {
            RedDoor redDoor = (RedDoor) b.getOwner();
            if (body.intersects(b) && !redDoor.isOpened()) {
                redDoorInfo = b;
                break;
            }
        }
    }

    public boolean isOnFloor() {
        rectsTmp[0].setRect(body.x, body.y + 1, body.width, body.height);
        Set<Body> bodies = getWorld().retrieve(rectsTmp[0], true, false);
        for (Body b : bodies) {
            long cc = b.getCollisionCategory();
            if (cc == 1 || cc == 32 || cc == 128) {
                return true;
            }
        }
        return false;
    }

    public Body getControllingElevator() {
        Set<Body> bodies = getWorld().retrieve(body, true, false, Object.class, Elevator.class);
        for (Body elevatorBody : bodies) {
            if (elevatorBody.getCollisionCategory() == 64) {
                if (elevatorBody.intersects(body)) {
                    return elevatorBody;
                }
            }
        }
        return null;
    }

    public boolean isInsideElevator() {
        Set<Body> bodies = getWorld().retrieve(body, true, false, Object.class, Elevator.class);
        for (Body elevatorBody : bodies) {
            if (elevatorBody.getCollisionCategory() == 64) {
                if (elevatorBody.contains(body)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHoldingDoc() {
        return System.currentTimeMillis() - holdingDocStartTime < 3000;
    }
    
    // ---
    
    public void start() {
        getScene().changeEntityZOrder(this, 6);
        camera.setPosition(0, 0);
        camera.setLocked(true);
        bodyAnimation = getG2D().getAnimations().get("player_start");
        bodyAnimation.stop();
        bodyAnimation.play();
        setVisible(true);
        changeState(START);
    }
    
    public void setStartSetup() {
        getScene().changeEntityZOrder(this, 6);
        body.x = 119;
        body.y = 106;
        
        // teste
        //body.x = 148;
        //body.y = 1480;
        //lastFloorBodyPosition.y = body.y + body.height;
        
        body.setAffectedByGravity(true);
        camera.setLocked(false);
        setVisible(true);
    }
    
    public void idle() {
        getScene().changeEntityZOrder(this, 6);
        body.height = 24;
        body.setVelocityX(0);
        changeState(IDLE);
    }
    
    public void walk(int d) {
        if (!isOnFloor()) {
            return;
        }
        getScene().changeEntityZOrder(this, 6);
        direction = d;
        body.height = 24;
        body.setVelocityX(d);
        changeState(WALK);
    }
    
    public void jump(int d) {
        if (!isOnFloor()) {
            return;
        }
        getScene().changeEntityZOrder(this, 6);
        direction = d != 0 ? d : direction;
        jumpDirection = d;
        body.height = 16;
        body.setVelocityX(d);
        body.setVelocityY(-2.75);
        changeState(JUMP);
    }

    public void down(int d) {
        if (!isOnFloor() || isInsideElevator()) {
            return;
        }
        getScene().changeEntityZOrder(this, 6);
        direction = d != 0 ? d : direction;
        body.height = 15;
        body.setVelocityX(0);
        changeState(DOWN);
    }

    public void smash() {
        if (isDied()) {
            return;
        }
        diedYPosition = (int) body.getY();
        getScene().changeEntityZOrder(this, 6);
        body.height = 16;
        body.setVelocityX(0);
        body.setAffectedByGravity(false);
        changeState(SMASH);
        updateAnimation();
        hud.died();
    }

    public void die(int dieYPosition) {
        if (isDied()) {
            return;
        }
        this.diedYPosition = dieYPosition;
        getScene().changeEntityZOrder(this, 6);
        body.height = 24;
        body.setVelocityX(0);
        changeState(DIE);
        updateAnimation();
        hud.died();
    }
    
    public boolean isDied() {
        return state == SMASH || state == DIE;
    }

    public void shot() {
        //if (shotAnimation.playing) {
        //    return;
        //}
        if (state != IDLE && state != WALK 
                && state != DOWN && state != JUMP) {
            return;
        }
        Bullet bullet = getAvailableBullet();
        if (bullet != null) {
            int bulletX = shotAnimationPosition.x + (direction == 1 ? 8 : 0);
            int bulletY = shotAnimationPosition.y + 2;
            bullet.spawn(bulletX, bulletY, direction);
            shotAnimation.stop();
            shotAnimation.play();
        }
    }
    
    private Bullet getAvailableBullet() {
        for (Bullet bullet : bullets) {
            if (!bullet.isVisible()) {
                return bullet;
            }
        }
        return null;
    }
    
    public void downStair() {
        if (downStairInfo == null) {
            return;
        }
        getScene().changeEntityZOrder(this, 2);
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.setAffectedByGravity(false);
        body.setRigid(false);
        direction = downStairInfo.getOwner().toString().contains("right") ? 1 : -1;
        if (direction < 0) {
            body.x = 0;
            body.y = downStairInfo.y + 8;
        }
        else {
            body.x = downStairInfo.x - 45;
            body.y = downStairInfo.y + 14;
        }
        changeState(DOWN_STAIR);
    }

    public void upStair() {
        if (upStairInfo == null) {
            return;
        }
        getScene().changeEntityZOrder(this, 2);
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.setAffectedByGravity(false);
        body.setRigid(false);
        direction = upStairInfo.getOwner().toString().contains("right") ? 1 : -1;
        if (direction < 0) {
            body.x = 0;
            body.y = upStairInfo.y - 40;
        }
        else {
            body.x = upStairInfo.x - 5;
            body.y = upStairInfo.y - 33;
        }
        changeState(UP_STAIR);
    }
    
    public void redDoorIn(int d) {
        if (redDoorInfo == null) {
            return;
        }
        getScene().changeEntityZOrder(this, 4);
        RedDoor redDoor = (RedDoor) redDoorInfo.getOwner();
        redDoor.open();
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.setAffectedByGravity(false);
        body.setRigid(false);
        direction = d;
        if (direction < 0) {
            body.x = redDoorInfo.x + 1;
            body.y = redDoorInfo.y - 21;
        }
        else {
            body.x = redDoorInfo.x - 15;
            body.y = redDoorInfo.y - 21;
        }
        changeState(RED_DOOR_IN);
    }
    
    public void redDoorOut() {
        if (redDoorInfo == null) {
            return;
        }
        getScene().changeEntityZOrder(this, 4);
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.setAffectedByGravity(false);
        body.setRigid(false);
        if (direction < 0) {
            body.x = redDoorInfo.x + 1;
            body.y = redDoorInfo.y - 21;
        }
        else {
            body.x = redDoorInfo.x - 15;
            body.y = redDoorInfo.y - 21;
        }
        changeState(RED_DOOR_OUT);
    }

    public boolean isHitable() {
        return state == DOWN || state == IDLE || state == JUMP || state == WALK;
    }
    
    public void hit() {
        if (state == RED_DOOR_IN || state == RED_DOOR_OUT) {
            return;
        }
        die((int) body.getY());
    }
    
    public void standBy() {
        updateStandByInstructionPointer = 0;
        standByStartTime = System.currentTimeMillis();
        changeState(STAND_BY);
    }
    
    public void nextLife(int x, int y) {
        // if no more life, show 'game over'
        getModel().decLives();
        if (getModel().getLives() <= 0) {
            ((LevelScene) getScene()).showGameOver();
            changeState(NONE);
        }
        else {
            body.x = x;
            body.y = y;
            body.setAffectedByGravity(true);
            camera.setLocked(false);
            setVisible(true);
            body.setAffectedByGravity(true);
            body.setRigid(true);
            idle();
            updateIdle();
            lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);        
        }
    }

    public void levelCleared() {
        changeState(LEVEL_CLEARED);
    }
    
}
