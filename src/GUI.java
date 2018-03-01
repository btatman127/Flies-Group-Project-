//import com.sun.tools.javadoc.Start;
//import javafx.scene.paint.Stop;

import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.awt.geom.*;

public class GUI extends JFrame {
    private int currentFrame;
    private JPanel buttonPanel;
    private JButton nextFrame;
    private JButton prevFrame;
    private JButton startCrop;
    private JButton endCrop;
    private JButton startLarvaeSelection;
    private JButton endLarvaeSelection;
    private int[] point1;
    private int[] point2;
    private ArrayList<Larva> larvae;
    public ImageComponent frame;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 100;


    public GUI(){
        larvae = new ArrayList<>();

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        point1 = new int[2];
        point2 = new int[2];

        //construct components
        currentFrame = 1;
        //setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        //make buttons for frames
        nextFrame = new JButton("Next Frame");
        prevFrame = new JButton("Previous Frame");
        startCrop = new JButton("Start Crop");
        endCrop = new JButton("End Crop");
        startLarvaeSelection = new JButton("Start Larvae Selection");
        endLarvaeSelection = new JButton("End Larvae Selection");


        //make new panel for buttons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        //add the buttons to the panel
        buttonPanel.add(nextFrame);
        buttonPanel.add(prevFrame);
        buttonPanel.add(startCrop);
        buttonPanel.add(endCrop);
        buttonPanel.add(startLarvaeSelection);
        buttonPanel.add(endLarvaeSelection);

        startLarvaeSelection.setEnabled(false);
        endLarvaeSelection.setEnabled(false);
        endCrop.setEnabled(false);
        //add an image component and make it draw the first image
        frame = new ImageComponent("pic0.png");

        frame.setBorder(BorderFactory.createEtchedBorder());
        //add the image component to the screen


        //buttonPanel.setLayout(layout);

        //create actions for the buttons
        Action nextAction = new StepAction(1);
        Action prevAction = new StepAction(-1);
        StartCropAction startCropAction = new StartCropAction();
        StopCropAction stopCropAction = new StopCropAction();
        StartLarvaeAction startLarvaeAction = new StartLarvaeAction();
        StopLarvaeAction stopLarvaeAction = new StopLarvaeAction();


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
        startCrop.addActionListener(startCropAction);
        endCrop.addActionListener(stopCropAction);
        startLarvaeSelection.addActionListener(startLarvaeAction);
        endLarvaeSelection.addActionListener(stopLarvaeAction);


        add(buttonPanel, new GBC(1, 0).setFill(GBC.EAST).setWeight(100, 0).setInsets(1));
        add(frame, new GBC(2, 0, 1, 4).setFill(GBC.BOTH).setWeight(800, 800));
        pack();
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

    private class StepAction extends AbstractAction {
        private int number;
        private ImageComponent ourFrame;

        public StepAction(int direction) {
            number = direction;

        }

        public void actionPerformed(ActionEvent event) {
            if(currentFrame+number > 0) {
                currentFrame += number;
                String frameToDraw = "assets/img" + String.format("%03d", currentFrame) + ".png";
                frame.setImage(frameToDraw);
                revalidate();
                repaint();
            }
        }
    }


    private class StartCropAction implements ActionListener
    {

        public StartCropAction() {

        }

        public void actionPerformed(ActionEvent event) {
                frame.maxSquares = 2;
                startLarvaeSelection.setEnabled(false);
                endCrop.setEnabled(true);
                repaint();

        }

    }
    private class StopCropAction implements ActionListener
    {

        public StopCropAction(){
        }

        public void actionPerformed(ActionEvent event) {

            if(frame.squares.size() == 2) {
                point1[0] = (int) frame.squares.get(0).getCenterX();
                point1[1] = (int) frame.squares.get(0).getCenterY();

                point2[0] = (int) frame.squares.get(1).getCenterX();
                point2[1] = (int) frame.squares.get(1).getCenterY();

                frame.remove(frame.squares.get(1));
                frame.remove(frame.squares.get(0));

                frame.maxSquares = 0;

                PreProcessor.crop(point1, point2, 90);

                startLarvaeSelection.setEnabled(true);

                revalidate();
                repaint();
            } else {
                System.out.println("Need to have 2 squares to crop the image.");
            }

        }

    }


    private class StartLarvaeAction implements ActionListener
    {

        public StartLarvaeAction(){
        }

        public void actionPerformed(ActionEvent event) {
            frame.maxSquares = 4;
            startCrop.setEnabled(false);
            endCrop.setEnabled(false);
            endLarvaeSelection.setEnabled(true);
            repaint();

        }

    }

    private class StopLarvaeAction implements ActionListener {

        public StopLarvaeAction() {
        }

        public void actionPerformed(ActionEvent event) {
            for (Rectangle2D r : frame.squares) {
                Larva addition = new Larva(r.getCenterX(), r.getCenterY());
                larvae.add(addition);
            }
            for(int i = frame.squares.size() - 1; i >= 0 ; i--){
                frame.remove(frame.squares.get(i));
            }

            frame.maxSquares = 0;
            startLarvaeSelection.setEnabled(false);
            endLarvaeSelection.setEnabled(false);
            repaint();

        }

    }
}


/**
 * A component that displays a tiled image
 */
class ImageComponent extends JComponent {

    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 800;
    private static final int SIDELENGTH = 7;
    public int maxSquares;
    public ArrayList<Rectangle2D> squares;
    private Rectangle2D current; // the square containing the mouse cursor
    private Image image;

    public ImageComponent(String fileName)
    {

        maxSquares = 0;
        image = new ImageIcon(fileName).getImage();
        squares = new ArrayList<>();
        current = null;
        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseMotionHandler());
    }

    public void setImage(String fileName) {
        try{
            image = ImageIO.read(new File(fileName));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //image = new ImageIcon(fileName).getImage();

    }


    public void paintComponent(Graphics g)
    {

        if (image == null) return;

        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);

        // draw the image in the upper-left corner

        g.drawImage(image, 0, 0, null);
        // tile the image across the component
        Graphics2D g2 = (Graphics2D) g;

        // draw all squares
        for (Rectangle2D r : squares)
            g2.draw(r);

    }

    public Dimension getPreferredSize() {
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }


    /**
     * Finds the first square containing a point.
     *
     * @param p a point
     * @return the first square that contains p
     */
    public Rectangle2D find(Point2D p) {
        for (Rectangle2D r : squares) {
            if (r.contains(p)) return r;
        }
        return null;
    }

    /**
     * Adds a square to the collection.
     *
     * @param p the center of the square
     */
    public void add(Point2D p) {
        double x = p.getX();
        double y = p.getY();

        current = new Rectangle2D.Double(x - SIDELENGTH / 2, y - SIDELENGTH / 2, SIDELENGTH,
                SIDELENGTH);
        squares.add(current);
        repaint();
    }

    /**
     * Removes a square from the collection.
     *
     * @param s the square to remove
     */
    public void remove(Rectangle2D s) {
        if (s == null) return;
        if (s == current) current = null;
        squares.remove(s);
        repaint();
    }

    private class MouseHandler extends MouseAdapter {

        public void mousePressed(MouseEvent event) {
            // add a new square if the cursor isn't inside a square
            current = find(event.getPoint());
            if (squares.size() < maxSquares) {
                if (current == null) add(event.getPoint());
            }
        }


        public void mouseClicked(MouseEvent event) {
            // remove the current square if double clicked
            current = find(event.getPoint());
            if (current != null && event.getClickCount() >= 2) remove(current);
        }
    }

    private class MouseMotionHandler implements MouseMotionListener {
        public void mouseMoved(MouseEvent event) {
            // set the mouse cursor to cross hairs if it is inside
            // a rectangle


            if (find(event.getPoint()) == null) setCursor(Cursor.getDefaultCursor());
            else setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        public void mouseDragged(MouseEvent event) {
            if (current != null) {
                int x = event.getX();
                int y = event.getY();

                // drag the current rectangle to center it at (x, y)
                current.setFrame(x - SIDELENGTH / 2, y - SIDELENGTH / 2, SIDELENGTH, SIDELENGTH);
                repaint();
            }
        }
    }
}

