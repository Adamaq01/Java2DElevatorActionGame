package br.ol.ge.core;

/**
 * Time class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Time {

    private final static double desiredFrameRateTime = 1000000000 / 45.0; // desired frame rate time (45 frames/sec)
    
    private static double currentTime;
    private static double lastTime;
    private static double deltaTime;
    private static double unprocessedTime;
    private static int updateCount;

    private static int fpsTime;
    private static int fpsCount;
    private static int fps;
    
    public static double getDelta() {
        return desiredFrameRateTime;
    }

    public static int getUpdateCount() {
        return updateCount;
    }

    public static void decUpdateCount() {
        updateCount--;
    }

    public static int getFPS() {
        return fps;
    }
    
    public static void update() {
        if (currentTime == 0) {
            currentTime = lastTime = System.nanoTime();
        }
        currentTime = System.nanoTime();
        deltaTime = currentTime - lastTime;
        unprocessedTime += deltaTime;
        while (unprocessedTime > desiredFrameRateTime) {
            unprocessedTime -= desiredFrameRateTime;
            updateCount++;
            fpsCount++;
        }
        fpsTime += deltaTime;
        if (fpsTime > 1000000000) {
            fpsTime -= 1000000000;
            fps = fpsCount;
            fpsCount = 0;
        }
        lastTime = currentTime;
    } 

}
