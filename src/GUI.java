import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class GUI extends JFrame{
    private int currentFrame;
    private JPanel buttonPanel;
    private JButton nextFrame;
    private JButton prevFrame;
    public ImageComponent frame;
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;

    public GUI(){
        currentFrame = 0;
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        //make buttons for frames
        nextFrame = new JButton("Next Frame");
        prevFrame = new JButton("Previous Frame");

        //make new panel for buttons
        buttonPanel = new JPanel();

        //add the buttons to the panel
        buttonPanel.add(nextFrame);
        buttonPanel.add(prevFrame);

        //add an image component and make it draw the first image
        frame = new ImageComponent("pic0.png");

        //add the image component to the screen
        add(frame);
        pack();

        //add the button panel to the screen
        add(buttonPanel);


        //create actions for the buttons
        Action nextAction = new StepAction(1);
        Action prevAction = new StepAction(-1);

        //create a map of inputs and name them
        InputMap imap = buttonPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke("RIGHT"), "panel.next");
        imap.put(KeyStroke.getKeyStroke("LEFT"), "panel.prev");

        //map those names of inputs to actions
        ActionMap amap = buttonPanel.getActionMap();
        amap.put("panel.next", nextAction);
        amap.put("panel.prev", prevAction);

        //attach the actions to the buttons
        nextFrame.addActionListener(nextAction);
        prevFrame.addActionListener(prevAction);

    }

    public static void main(String[] args) {
        System.out.println("Hello World!");

        EventQueue.invokeLater(() ->
        {
            JFrame frame = new GUI();
            frame.setTitle("GUI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });

    }

    private class StepAction extends AbstractAction
    {
        private int number;
        private ImageComponent ourFrame;

        public StepAction(int direction)
        {
            number = direction;

        }

        public void actionPerformed(ActionEvent event)
        {
            String frameToDraw;
            currentFrame += number;
            frameToDraw = "pic" + Integer.toString(currentFrame) + ".png";
            frame.setImage(frameToDraw);
            repaint();
        }
    }


    public void drawImage(String fileName){
        add(new ImageComponent(fileName));
        pack();
    }

}


/**
 * A component that displays a tiled image
 */
class ImageComponent extends JComponent {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 800;

    private Image image;

    public ImageComponent(String fileName)
    {
        image = new ImageIcon(fileName).getImage();
    }

    public void setImage(String fileName){
        image = new ImageIcon(fileName).getImage();

    }

    public void paintComponent(Graphics g)
    {
        if (image == null) return;

        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);

        // draw the image in the upper-left corner

        g.drawImage(image, 150, 300, null);
        // tile the image across the component

    }

    public Dimension getPreferredSize() { return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT); }
}