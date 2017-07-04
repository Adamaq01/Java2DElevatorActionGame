package br.ol.elevador_action;

import br.ol.elevador_action.entity.Elevator;
import br.ol.elevador_action.scene.LevelScene;
import br.ol.elevador_action.scene.OLPresentsScene;
import br.ol.elevador_action.scene.TitleScene;
import br.ol.ge.core.Game;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ElevadorActionGame class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class ElevadorActionGame extends Game {
                
    private final ElevadorActionModel model;
    private final Map<String, Class> scenes = new HashMap<String, Class>();
    
    public ElevadorActionGame() {
        model = new ElevadorActionModel(this);
        createAllScenes();
        
        initStartScene();
        // model.startGame(); // test
    }

    public ElevadorActionModel getModel() {
        return model;
    }
    
    private void initStartScene() {
        changeScene("ol_presents");
    }

    private void createAllScenes() {
        scenes.put("ol_presents", OLPresentsScene.class);
        scenes.put("title", TitleScene.class);
        scenes.put("level", LevelScene.class);
    }

    public void changeScene(String sceneName) {
        ElevadorActionScene scene;
        try {
            scene = (ElevadorActionScene) scenes.get(sceneName).newInstance();
            scene.setGame(this);
            scene.start();
            setScene(scene);
            System.gc();
        } catch (Exception ex) {
            Logger.getLogger(ElevadorActionGame.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }

}
