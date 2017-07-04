package main;

import br.ol.elevador_action.ElevadorActionGame;
import br.ol.elevador_action.ElevadorActionModel;
import br.ol.ge.core.Display;
import br.ol.ge.core.Game;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main class.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Game game = new ElevadorActionGame();
                Display display = new Display(game, 256, 240, 2.5, 2);
                JFrame view = new JFrame();
                view.setTitle("Java 2D Elevator Action");
                view.getContentPane().add(display);
                view.pack();
                view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                view.setResizable(false);
                view.setLocationRelativeTo(null);
                view.setVisible(true);
                display.requestFocus();
                display.start();
            }
            
        });
    }
    
}
