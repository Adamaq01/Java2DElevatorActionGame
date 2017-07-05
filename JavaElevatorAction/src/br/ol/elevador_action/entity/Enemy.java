package br.ol.elevador_action.entity;

import br.ol.elevador_action.ElevadorActionEntity;
import br.ol.elevador_action.ElevadorActionScene;
import static br.ol.elevador_action.entity.Enemy.EnemyState.*;
import br.ol.elevador_action.scene.LevelScene;
import br.ol.g2d.Animation;
import br.ol.ge.physics.Body;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enemy class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Enemy extends ElevadorActionEntity {
    
    public static enum EnemyState { AI, STAND_BY, IDLE, WALK, JUMP, DOWN, SMASH, DIE
        , DOWN_STAIR, UP_STAIR, DOOR_IN, DOOR_OUT }
    
    private EnemyState state = STAND_BY;
    private Camera camera;
    private Player player;
    private int direction;
    private int jumpDirection;
    private Body<Enemy> body;
    private Body<Enemy> lastFloorBodyPosition;
    
    private final Map<String, Animation> animations = new HashMap<String, Animation>();
    private Animation bodyAnimation;
    private final Point bodyAnimationPosition = new Point();
    private Animation shotAnimation;
    private final Point shotAnimationPosition = new Point();
    
    private final Rectangle[] rectsTmp = { new Rectangle(), new Rectangle() };
    private final Bullet[] bullets = new Bullet[1];
    private Body<String> downStairInfo;
    private Body<String> upStairInfo;
    private long idleStartTime;
    private int walkTargetX;
    
    private long standByWaitTime;
    private long standByTimeStartTime;
    
    private boolean shooting;
    private int shootingDirection;
    private long lastShotTime;
    
    private Body<Enemy> sensor;
    private final PossibleActions possibleActions = new PossibleActions();
    private long lastPlayerReachableTime;
    private long giveUpTime = 15000;
    private Body currentBlueDoorBody;
    
    private class PossibleActions {

        private boolean down;
        private int downDirection;
        private boolean jump;
        private int jumpDirection;
        private boolean shot;
        private int shotDirection;
        private boolean walk;
        private int walkPositionX;
        private boolean walkToStairDown;
        private int stairDownPositionX;
        private boolean stairDown;
        private boolean walkToStairUp;
        private int stairUpPositionX;
        private boolean stairUp;
        private boolean walkToElevator;
        private int elevatorPositionX;
        private Elevator elevator;
        private boolean elevatorUp;
        private boolean elevatorDown;
        private boolean walkToBlueDoor;
        private int blueDoorPositionX;
        private boolean doorIn;
        private Body blueDoorBody;
        
        public void clear() {
            down = false;
            downDirection = 0;
            jump = false;
            jumpDirection = 0;
            shot = false;
            shotDirection = 0;
            walk = false;
            walkPositionX = 0;
            walkToStairDown = false;
            stairDownPositionX = 0;
            stairDown = false;
            walkToStairUp = false;
            stairUpPositionX = 0;
            stairUp = false;
            elevator = null;
            walkToElevator = false;
            elevatorPositionX = 0;
            elevatorUp = false;
            elevatorDown = false;
            walkToBlueDoor = false;
            blueDoorPositionX = 0;
            doorIn = false;
            blueDoorBody = null;
        }
        
    }
    
    public Enemy(ElevadorActionScene scene, long standByWaitTime) {
        super(scene);
        this.standByWaitTime = standByWaitTime;
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
            bullets[i] = new Bullet(getScene(), camera, 2); // 2 = player
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public EnemyState getState() {
        return state;
    }

    public Body<Enemy> getBody() {
        return body;
    }

    public void changeState(EnemyState state) {
        if (this.state != state) {
            EnemyState oldState = this.state;
            this.state = state;
            playerStateChanged(oldState, state);
        }
    }

    @Override
    public void init() {
        direction = -1;
        bodyAnimation = getAnimation("enemy_walk_left");
        shotAnimation = getAnimation("enemy_shot_left");
        shotAnimation.currentFrameIndex = shotAnimation.lastFrameIndex;
        body = new Body<Enemy>(this, true, true, 1, 120, 112, 10, 24, 4, 417);
        lastFloorBodyPosition = new Body<Enemy>(this, true, true, 1, 120, body.getY() + body.getHeight(), 1, 2, 4, 417);
        sensor = new Body<Enemy>(this, false, true, 0, 0, body.getY(), 0, 0, 0, 0);
        body.setAffectedByGravity(false);
        getScene().getWorld().addBody(body);
        getScene().getWorld().addBody(lastFloorBodyPosition);
        getScene().getWorld().addBody(sensor);
        standBy(standByWaitTime);
        setZorder(5);
    }

    @Override
    public void update() {
        checkOutOfCamera();
        updateUpDownStairInfo();
        
        ensureStayGluedElevatorFloor();
        switch (state) {
            case AI: updateAI(); break;
            case STAND_BY: updateStandBy(); break;
            case IDLE: updateIdle(); break;
            case WALK: updateWalk(); break;
            case JUMP: updateJump(); break;
            case DOWN: updateDown(); break;
            case SMASH: updateSmash(); break;
            case DIE: updateDie(); break;
            case DOWN_STAIR: updateDownStair(); break;
            case UP_STAIR: updateUpStair(); break;
            case DOOR_IN: updateDoorIn(); break;
            case DOOR_OUT: updateDoorOut(); break;
        }
        shotAnimation.update(1000 / 45);
        bodyAnimation.update(1000 / 45);
    }
    
    @Override
    public void updateAfterPhysics() {
        ensureNotGetStuckedFloor();
        updateAnimation();
    }

    private void checkOutOfCamera() {
        if (isDead()) {
            return;
        }
        if ((body.getY() > camera.getArea().getY() + camera.getArea().getHeight())
                || (body.getY() < camera.getArea().getY() - body.getHeight())) {
            standBy();
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
            die();
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
    
    private Animation getAnimation(String name) {
        Animation animation = animations.get(name);
        if (animation == null) {
            animation = getG2D().getAnimations().getCopy(name);
            animations.put(name, animation);
        }
        return animation;
    }

    private void updateAnimation() {
        updateBodyAnimation();
        updateShotAnimation();
    }
    
    private void updateBodyAnimation() {
        String animationName = "";
        String stateStr = state.toString().toLowerCase();
        String directionStr = direction > 0 ? "right" : "left";
        switch (state) {
            case AI: 
                animationName = "enemy_" + "idle" + "_" + directionStr;
                break;
            case STAND_BY: 
                return;
            case SMASH: 
                animationName = "enemy_" + stateStr;
                break;
            case DOWN_STAIR: 
            case UP_STAIR: 
            case DIE: 
            case DOOR_IN: 
                animationName = "enemy_" + stateStr + "_" + directionStr;
                break;
            case DOOR_OUT: 
                animationName = "enemy_" + stateStr + "_" + directionStr;
                break;
            case IDLE: 
            case WALK: 
            case JUMP: 
            case DOWN: 
                animationName = "enemy_" + stateStr + "_" + directionStr;
                break;
        }
        // System.out.println("animationName: " + animationName);
        Animation animation = getAnimation(animationName);
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
        Animation animation = getAnimation("enemy_shot_" + directionStr);
        animation.currentFrameIndex = shotAnimation.currentFrameIndex;
        animation.playing = shotAnimation.playing;
        shotAnimation = animation;
        switch (state) {
            case STAND_BY: 
            case SMASH: 
            case DIE: 
            case DOWN_STAIR: 
            case UP_STAIR: 
            case DOOR_IN: 
            case DOOR_OUT: 
                shotAnimationPosition.x = -50;
                shotAnimationPosition.y = -50;
                break;
            case AI: 
            case IDLE: 
            case WALK: 
            case JUMP: 
            case DOWN: 
                updateShotAnimationPosition(direction > 0 ? 5 : -10, 4);
                break;
        }
        
        if (shooting && shotAnimation.currentFrameIndex > 14) {
            commitShot();
        }
    }
    
    private void commitShot() {
        Bullet bullet = getAvailableBullet();
        int bulletX = shotAnimationPosition.x + (direction == 1 ? 8 : 0);
        int bulletY = shotAnimationPosition.y + 2;
        bullet.spawn(bulletX, bulletY, direction);
        lastShotTime = System.currentTimeMillis();
        shooting = false;
    }
    
    private void updateShotAnimationPosition(double tx, double ty) {
        shotAnimationPosition.x = (int) (body.x + tx);
        shotAnimationPosition.y = (int) (body.y + ty);
    }

    private void updateAI() {
        int random = (int) (2 * Math.random());
        evaluatePossibleActions();
        
        double playerEnemyDistanceY = player.getBody().getY() - body.getY();
        boolean giveUpChasing = playerEnemyDistanceY > 96 || System.currentTimeMillis() - lastPlayerReachableTime > giveUpTime;
        // System.out.println("giveup: " + giveUpChasing + " playerEnemyDistanceY=" + playerEnemyDistanceY);
        
        if (possibleActions.down) {
            down(possibleActions.downDirection);
        }
        else if (possibleActions.jump) {
            jump(random > 0 ? possibleActions.jumpDirection : 0);
        }
        else if (possibleActions.shot) {
            direction = possibleActions.shotDirection;
            shot();
        }

        // can't reach player, so give up and door in
        else if (possibleActions.walkToBlueDoor && giveUpChasing) {
            walkTo(possibleActions.blueDoorPositionX);
        }
        else if (possibleActions.doorIn && giveUpChasing) {
            doorIn(possibleActions.blueDoorBody);
        }

        else if (possibleActions.elevatorUp) {
            possibleActions.elevator.up();
        }
        else if (possibleActions.elevatorDown) {
            possibleActions.elevator.down();
        }
        else if (possibleActions.walkToElevator) {
            walkTo(possibleActions.elevatorPositionX);
        }
        
        else if (possibleActions.stairUp) {
            upStair();
        }
        else if (possibleActions.stairDown) {
            downStair();
        }
        else if (possibleActions.walkToStairUp) {
            walkTo(possibleActions.stairUpPositionX);
        }
        else if (possibleActions.walkToStairDown) {
            walkTo(possibleActions.stairDownPositionX);
        }
        
        else if (possibleActions.walk) {
            walkTo(possibleActions.walkPositionX);
        }
        else {
            walkTargetX = (int) (body.getX() + 20 - (40 * Math.random()));
            walkTo(walkTargetX);
        }
        
    }
    
    private void updateStandBy() {
        if (System.currentTimeMillis() - standByTimeStartTime < standByWaitTime) {
            return;
        }
        doorOut();
    }

    private void updateIdle() {
        evaluatePossibleActions();
        if (possibleActions.down || possibleActions.jump) {
            thinkNextAction();
            return;
        }
        else if (System.currentTimeMillis() - idleStartTime < 1000) {
            return;
        }
        thinkNextAction();
    }

    private void updateWalk() {
        body.setVelocityX(direction);
        evaluatePossibleActions();
        if (possibleActions.down || possibleActions.jump) {
            thinkNextAction();
        }
        else if ((direction > 0 && body.getX() >= walkTargetX)
                || (direction < 0 && body.getX() <=walkTargetX)
                || (direction > 0 && body.getX() >= sensor.getX() + sensor.getWidth() - body.getWidth() - 2)
                || (direction < 0 && body.getX() <= sensor.getX() + 2)) {
            body.setVelocityX(0);
            idle();
        }
    }

    private void updateJump() {
        if (isOnFloor()) {
            body.y -= 4;
            idle();
            return;
        }
        body.setVelocityX(jumpDirection);
    }
    
    private long downFinishStartTime;
    private boolean downFinished;
    
    private void updateDown() {
        if (!downFinished) {
            evaluatePossibleActions();
            if (possibleActions.down) {
                return;
            }
            else {
                downFinished = true;
                downFinishStartTime = System.currentTimeMillis();
                
                // 50% chance shot
                int random = (int) (2 * Math.random());
                if (random == 0) {
                    shot();
                }
            }
            return;
        }
        // wait 700 ms
        if (System.currentTimeMillis() - downFinishStartTime < 700) {
            return;
        }
        body.y -= 4;
        idle();
    }

    private void updateSmash() {
        if (bodyAnimation.playing) {
            return;
        }
        standBy();
    }

    private void updateDie() {
        if (bodyAnimation.playing) {
            return;
        }
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
        thinkNextAction();
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
        thinkNextAction();
    }
    
    private void updateDoorIn() {
        if (bodyAnimation.playing) {
            return;
        }
        getModel().markDoorOccupied(currentBlueDoorBody, false);
        currentBlueDoorBody = null;
        standBy(1000 + (long) (2000 * Math.random()));
    }
    
    private void updateDoorOut() {
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
        body.setRigid(true);
        body.setAffectedByGravity(true);
        lastFloorBodyPosition.setRect(body.getX() + body.getWidth() / 2, body.getY() + body.getHeight(), 1, 2);        
        getModel().markDoorOccupied(currentBlueDoorBody, false);
        currentBlueDoorBody = null;
        idle();
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

    private void playerStateChanged(EnemyState oldState, EnemyState state) {
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

    private final List<Body> bodyListTmp = new ArrayList<Body>();
    
    public boolean isInsideElevator() {
        bodyListTmp.clear();
        bodyListTmp.addAll(getWorld().retrieve(body, true, false, Object.class, Elevator.class));
        for (Body elevatorBody : bodyListTmp) {
            if (elevatorBody.getCollisionCategory() == 64) {
                if (elevatorBody.contains(body)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---
    
    public void thinkNextAction() {
        changeState(AI);
    }

    public void standBy() {
        standBy(2000 + (long) (3000 * Math.random()));
    }
    
    public void standBy(long time) {
        setVisible(false);
        body.setRigid(false);
        lastFloorBodyPosition.setRigid(false);
        standByTimeStartTime = System.currentTimeMillis();
        shotAnimation.currentFrameIndex = shotAnimation.lastFrameIndex;
        standByWaitTime = time;
        changeState(STAND_BY);
    }
    
    public void idle() {
        getScene().changeEntityZOrder(this, 5);
        body.height = 24;
        body.setVelocityX(0);
        body.setAffectedByGravity(true);
        idleStartTime = System.currentTimeMillis();
        changeState(IDLE);
        updateAnimation();
    }

    public void walkTo(int targetX) {
        if (!isOnFloor() || shooting) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        direction = (targetX - body.getX()) > 0 ? 1 : -1;
        walkTargetX = (int) targetX;
        body.height = 24;
        body.setVelocityX(direction);
        changeState(WALK);
    }
        
    public void walk(int dx) {
        if (!isOnFloor()) {
            return;
        }
        // can walk just inside sensor area
        else if ((direction > 0 && body.getX() >= sensor.getX() + sensor.getWidth() - body.getWidth() - 2)
                || (direction < 0 && body.getX() <= sensor.getX() + 2)) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        direction = dx > 0 ? 1 : -1;
        if (direction != shootingDirection && shotAnimation.playing) {
            return;
        }
        walkTargetX = (int) (body.getX() + dx);
        body.height = 24;
        body.setVelocityX(direction);
        changeState(WALK);
    }
    
    public void jump(int d) {
        if (!isOnFloor()) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        direction = d != 0 ? d : direction;
        jumpDirection = d;
        body.height = 16;
        body.setVelocityX(d);
        body.setVelocityY(-2.75);
        changeState(JUMP);
    }

    public void down(int d) {
        if (!isOnFloor()) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        downFinished = false;
        direction = d != 0 ? d : direction;
        body.height = 15;
        body.setVelocityX(0);
        changeState(DOWN);
    }

    public void smash() {
        if (isDead()) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        body.height = 16;
        body.setVelocityX(0);
        body.setAffectedByGravity(false);
        changeState(SMASH);
        updateAnimation();
    }

    public void die() {
        if (isDead()) {
            return;
        }
        getScene().changeEntityZOrder(this, 5);
        body.height = 24;
        body.setVelocityX(0);
        changeState(DIE);
        updateAnimation();
    }
    
    public boolean isDead() {
        return state == STAND_BY || state == SMASH || state == DIE;
    }

    public void shot() {
        // can shot only each 3 seconds
        if (System.currentTimeMillis() - lastShotTime < 3000) {
            return;
        }
        
        if (state != AI && state != IDLE && state != WALK 
                && state != DOWN && state != JUMP) {
            return;
        }
        
        if (canShot()) {
            shotAnimation.stop();
            shotAnimation.play();
            shooting = true;
            shootingDirection = direction;
        }
    }
    
    private boolean canShot() {
        return !shooting && getAvailableBullet() != null;
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
    
    public void doorIn(Body doorBody) {
        if (getModel().isBlueDoorOccupied(doorBody)) {
            standBy();
            return;
        }
        getModel().markDoorOccupied(doorBody, true);
        getScene().changeEntityZOrder(this, 4);
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.setAffectedByGravity(false);
        body.setRigid(false);
        direction = doorBody.getOwner().toString().contains("_right") ? 1 : -1;
        int tx = direction > 0 ? -8 : 0;
        body.x = (int) doorBody.x + tx;
        body.y = (int) doorBody.y;
        body.height = 28;
        currentBlueDoorBody = doorBody;
        changeState(DOOR_IN);
        updateAnimation();
    }

    private void doorOut() {
        Body<String> randomBlueDoor = ((LevelScene) getScene()).getRandomBlueDoorPosition();
        if (randomBlueDoor == null) {
            standBy();
            return;
        }
        getModel().markDoorOccupied(randomBlueDoor, true);
        getScene().changeEntityZOrder(this, 4);
        direction = randomBlueDoor.getOwner().toString().contains("left") ? -1 : 1;
        bodyAnimationPosition.x = (int) randomBlueDoor.x;
        bodyAnimationPosition.y = (int) randomBlueDoor.y;
        int tx = direction > 0 ? -8 : 0;
        body.setVelocityX(0);
        body.setVelocityY(0);
        body.x = (int) randomBlueDoor.x + tx;
        body.y = (int) randomBlueDoor.y;
        body.height = 28;
        body.setAffectedByGravity(false);
        body.setRigid(false);
        currentBlueDoorBody = randomBlueDoor;
        changeState(DOOR_OUT);
        updateAnimation();
        setVisible(true);
        giveUpTime = 10000 + (long) (15000 * Math.random());
        lastPlayerReachableTime = System.currentTimeMillis();
    }
    
    public boolean isHitable() {
        return state == AI || state == DOWN || state == IDLE || state == JUMP || state == WALK
                || state == DOOR_IN || state == DOOR_OUT;
    }

    public void hit() {
        if (state == DOOR_IN || state == DOOR_OUT) {
            return;
        }
        die();
    }    

    // --- AI ---
    
    private final List<Body> bodyListTmp2 = new ArrayList<Body>();
    
    private void evaluatePossibleActions() {
        possibleActions.clear();
        if (isDead()) {
            return;
        }
        resizeSensor();
        bodyListTmp2.clear();
        bodyListTmp2.addAll(getWorld().retrieve(sensor, true, true));
        for (Body b : bodyListTmp2) {
            if (b.getOwner() instanceof Bullet) {
                analyzeBullet((Bullet) b.getOwner());
            }
            else if (b.getOwner() instanceof Player) {
                analyzePlayer((Player) b.getOwner());
            }
            else if (b.getOwner().toString().startsWith("stair_")) {
                analyzeStair(b);
            }
            else if (b.getOwner().toString().startsWith("elevator_area")) {
                analyzeElevatorArea(b);
            }
            else if (b.getCollisionCategory() == 64) {
                analyzeElevator(b);
            }
            else if (b.getOwner().toString().startsWith("door_")) {
                analyzeBlueDoor(b);
            }
        }
        tryChasePlayerUpDown();
    }
    
    private void resizeSensor() {
        sensor.height = 22;
        sensor.y = body.y + body.height - 24;
        sensor.x = body.x;
        sensor.width = body.width;
        rectsTmp[0].setRect(sensor);
        // increase left
        do {
            rectsTmp[0].x -= 4;
            rectsTmp[0].width += 4;
        } while (!hitsSensorLimit(rectsTmp[0]) 
                && rectsTmp[0].width < getWorld().getWidth());
        rectsTmp[0].x += 8;
        rectsTmp[0].width -= 8;
        // increase right
        do {
            rectsTmp[0].width += 4;
        } while (!hitsSensorLimit(rectsTmp[0]) 
                && rectsTmp[0].width < getWorld().getWidth());
        rectsTmp[0].width -= 8;
        // set correct sensor size
        sensor.setRect(rectsTmp[0]);
    }
    
    private final List<Body> bodyListTmp3 = new ArrayList<Body>();
    
    private boolean hitsSensorLimit(Rectangle2D rect) {
        bodyListTmp3.clear();
        bodyListTmp3.addAll(getWorld().retrieve(rect, true, true));
        
        // check elevator current floor
        boolean elevatorCurrentFloor = false;
        for (Body b : bodyListTmp3) {
            if (b.getCollisionCategory() == 32 || b.getCollisionCategory() == 64 
                    || b.getCollisionCategory() == 128) {
                elevatorCurrentFloor = true;
                break;
            }
        }
        
        for (Body b : bodyListTmp3) {
            if (!elevatorCurrentFloor && b.getOwner().toString().equals("elevator_area")) {
                return true;
            }
            else if (b.getCollisionCategory() == 1) {
                return true;
            }
        }
        return false;
    }
    
    private void analyzeBullet(Bullet bullet) {
        double bulletEnemyDistance = body.getX() - bullet.getBody().getX();
        
        // bullet is still active
        if (!bullet.isVisible()) {
            return;
        }
        // bullet can kill enemy ?
        else if (bullet.getTarget() != 4) {
            return;
        }
        // bullet can hit this enemy ?
        else if ((bullet.getDirection() < 0 && body.getX() > bullet.getBody().getX()) 
                || (bullet.getDirection() > 0 && body.getX() < bullet.getBody().getX())) {
            return;
        }
        // bullet is still too distance
        else if (Math.abs(bulletEnemyDistance) > 50) {  
            return;
        }
        
        int dir = bulletEnemyDistance > 0 ? -1 : 1;
        
        if (Math.abs(sensor.getY() - bullet.getBody().getY()) < 7) {
            possibleActions.down = true;
            possibleActions.downDirection = dir;
        }
        else {
            possibleActions.jump = true;
            possibleActions.jumpDirection = dir;
        }
    }
    
    private void tryChasePlayerUpDown() {
        double playerEnemyDistanceY = player.getBody().getY() - body.getY();
        double dy = Math.abs(playerEnemyDistanceY);
        // needs to up
        if (dy > 24 && playerEnemyDistanceY < 0 && upStairInfo != null) {
            possibleActions.stairUp = true;
        }
        // needs to down
        else if (dy > 24 && playerEnemyDistanceY > 0 && downStairInfo != null) {
            possibleActions.stairDown = true;
        }
        // player is in the same floor, so just shot
        else if (dy < 8 && canShot() && player.isHitable()) {
            double playerEnemyDistanceX = player.getBody().getX() - body.getX();
            possibleActions.shot = true;
            possibleActions.shotDirection = playerEnemyDistanceX > 0 ? 1 : -1;
        } 
    }
    
    private void analyzePlayer(Player player) {
        // player is in the same floor, so try to shot
        if (canShot() && player.isHitable()) {
            double playerEnemyDistanceX = player.getBody().getX() - body.getX();
            possibleActions.shot = true;
            possibleActions.shotDirection = playerEnemyDistanceX > 0 ? 1 : -1;
        }
        // can't shot, so just try to walk randomically
        else {
            possibleActions.walk = true;
            possibleActions.walkPositionX = (int) (body.getX() + 40 - (80 * Math.random()));
        }
        lastPlayerReachableTime = System.currentTimeMillis();
    }
    
    private void analyzeStair(Body stairBody) {
        double playerEnemyDistanceY = player.getBody().getY() - body.getY();
        double dy = Math.abs(playerEnemyDistanceY);
        boolean isStairUp = stairBody.getOwner().toString().contains("bottom");
        // needs to up
        if (dy > 24 && playerEnemyDistanceY < 0 && isStairUp && upStairInfo == null) {
            possibleActions.walkToStairUp = true;
            int v1 = possibleActions.stairUpPositionX;
            double d1 = Math.abs(getBody().getX() - v1);
            int v2 = (int) (stairBody.getX() - 4);
            double d2 = Math.abs(getBody().getX() - v2);
            possibleActions.stairUpPositionX = getShorter(v1, d1, v2, d2);
        }
        // needs to down
        else if (dy > 24 && playerEnemyDistanceY > 0 && !isStairUp && downStairInfo == null) {
            possibleActions.walkToStairDown = true;
            int v1 = possibleActions.stairDownPositionX;
            double d1 = Math.abs(getBody().getX() - v1);
            int v2 = (int) (stairBody.getX() - 4);
            double d2 = Math.abs(getBody().getX() - v2);
            possibleActions.stairDownPositionX = getShorter(v1, d1, v2, d2);
        }
    }
    
    private void analyzeElevatorArea(Body elevatorAreaBody) {
        // this elevator can't reach player
        boolean canReachPlayer = elevatorAreaBody.contains(elevatorAreaBody.x, player.getBody().y);
        if (!canReachPlayer) {
            return;
        }
        // ---
        double playerEnemyDistanceY = player.getBody().getY() - body.getY();
        double dy = Math.abs(playerEnemyDistanceY);
        // needs to up
        if (dy > 24 && playerEnemyDistanceY < 0) {
            possibleActions.walkToElevator = true;
            int v1 = possibleActions.elevatorPositionX;
            double d1 = Math.abs(getBody().getX() - v1);
            int randomPosition = (int) ((elevatorAreaBody.getWidth() - body.width - 2) * Math.random());
            int v2 = (int) (elevatorAreaBody.getX() + randomPosition + 1);
            //int v2 = (int) (elevatorAreaBody.getX() + elevatorAreaBody.getWidth() / 2 - body.width / 2);
            double d2 = Math.abs(getBody().getX() - v2);
            possibleActions.elevatorPositionX = getShorter(v1, d1, v2, d2);
        }
        // needs to down
        else if (dy > 24 && playerEnemyDistanceY > 0) {
            possibleActions.walkToElevator = true;
            int v1 = possibleActions.elevatorPositionX;
            double d1 = Math.abs(getBody().getX() - v1);
            int randomPosition = (int) ((elevatorAreaBody.getWidth() - body.width - 2) * Math.random());
            int v2 = (int) (elevatorAreaBody.getX() + randomPosition + 1);
            //int v2 = (int) (elevatorAreaBody.getX() + elevatorAreaBody.getWidth() / 2 - body.width / 2);
            double d2 = Math.abs(getBody().getX() - v2);
            possibleActions.elevatorPositionX = getShorter(v1, d1, v2, d2);
        }
    }
    
    private void analyzeElevator(Body elevatorBody) {
        double playerEnemyDistanceY = player.getBody().getY() - body.getY();
        double dy = Math.abs(playerEnemyDistanceY);
        Elevator elevator = (Elevator) elevatorBody.getOwner();
        if (elevator.isEnemyInside(this) && !elevator.canPlayerControl()) {
            possibleActions.elevator = elevator;
            possibleActions.elevatorUp = dy > 24 && playerEnemyDistanceY < 0;
            possibleActions.elevatorDown = dy > 24 && playerEnemyDistanceY > 0;
        }
    }

    private void analyzeBlueDoor(Body blueDoorBody) {
        // check if blue door is really blue door (can be red), and if it's not occupied by other enemies
        if (getModel().isBlueDoorOccupied(blueDoorBody)) {
            return;
        }
        int doorEnemyDistance = (int) (body.getX() - blueDoorBody.getX());
        if (blueDoorBody.intersects(body) && (doorEnemyDistance == -6 || doorEnemyDistance == 6)) {
            possibleActions.doorIn = true;
            possibleActions.blueDoorBody = blueDoorBody;
            possibleActions.walkToBlueDoor = false;
        }
        else if (!possibleActions.doorIn) {
            int tx = blueDoorBody.getOwner().toString().contains("_right") ? 6 : -6;
            possibleActions.walkToBlueDoor = true;
            int v1 = possibleActions.blueDoorPositionX;
            double d1 = Math.abs(getBody().getX() - v1);
            int v2 = (int) (blueDoorBody.getX() + tx);
            double d2 = Math.abs(getBody().getX() - v2);
            possibleActions.blueDoorPositionX = getShorter(v1, d1, v2, d2);
        }
    }

    private int getShorter(int v1, double d1, int v2, double d2) {
        return v1 > 0 && d1 < d2 ? v1 : v2;
    }
    
}
