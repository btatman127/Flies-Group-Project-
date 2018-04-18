import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.awt.geom.*;
import java.util.concurrent.TimeUnit;

public class GUI extends JFrame {
    private int currentFrame;
    private JPanel buttonPanel;
    private String fileName;
    private String movieDir;
    private Video movie;
    private JButton openMovie;
    private JButton nextFrame;
    private JButton prevFrame;
    private JButton startCrop;
    private JButton endCrop;
    private JButton startLarvaeSelection;
    private JButton endLarvaeSelection;
    private JButton exportCSV;
	private final JProgressBar cropProgress;
    private JCheckBox showPaths;
    private JTextPane displayFrameNum;
    private int[] point1;
    private int[] point2;
    private FileDialog fd;
    //private ArrayList<Larva> larvae; //moved to video class (movie variable)
    public ImageComponent frame;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 100;


    public GUI() {
        //larvae = new ArrayList<>(); //moved to video class

        fd = new FileDialog(this, "Choose a File", FileDialog.LOAD);
        fd.setDirectory("C:\\");

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        point1 = new int[2];
        point2 = new int[2];

        //construct components
        currentFrame = 1;
        //setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        //make buttons for frames
        openMovie = new JButton("Open Movie");
        nextFrame = new JButton("Next Frame");
        prevFrame = new JButton("Previous Frame");
        startCrop = new JButton("Start Crop");
        endCrop = new JButton("End Crop");
        startLarvaeSelection = new JButton("Start Larvae Selection");
        endLarvaeSelection = new JButton("End Larvae Selection");
        showPaths = new JCheckBox("Show Larvae Paths");
        exportCSV = new JButton(("Export as CSV file"));
		cropProgress = new JProgressBar();
		

        DefaultStyledDocument sd = new DefaultStyledDocument();
        displayFrameNum = new JTextPane(sd);
        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);
        displayFrameNum.setParagraphAttributes(as, true);





        //make new panel for buttons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        //add the buttons to the panel
        buttonPanel.add(openMovie);
        buttonPanel.add(nextFrame);
        buttonPanel.add(prevFrame);
        buttonPanel.add(startCrop);
        buttonPanel.add(endCrop);
        buttonPanel.add(startLarvaeSelection);
        buttonPanel.add(endLarvaeSelection);
        buttonPanel.add(showPaths);
        buttonPanel.add(exportCSV);
        buttonPanel.add(displayFrameNum);
		buttonPanel.add(cropProgress);

        // UNCOMMENT THIS WHEN YOU WANT TO UTILIZE THE OPEN FUNCTION OF THE GUI
        //make sure some of the buttons can't be pressed yet
        nextFrame.setVisible(false);
        prevFrame.setVisible(false);
        startCrop.setVisible(false);
        endCrop.setVisible(false);
        startLarvaeSelection.setVisible(false);
        endLarvaeSelection.setVisible(false);
        showPaths.setVisible(false);
        exportCSV.setVisible(false);
        displayFrameNum.setVisible(false);
		cropProgress.setVisible(false);

        // COMMENT THIS OUT WHEN YOU WANT TO UTILIZE THE OPEN FUNCTION OF THE GUI
//        openMovie.setEnabled(false);
//        startLarvaeSelection.setEnabled(false);
//        endLarvaeSelection.setEnabled(false);
//        endCrop.setEnabled(false);

        frame = new ImageComponent("pic1.png");
        frame.setBorder(BorderFactory.createEtchedBorder());
        //add the image component to the screen

        //buttonPanel.setLayout(layout);

        //create actions for the buttons
        OpenL openAction = new OpenL();
        Action nextAction = new StepAction(1);
        Action prevAction = new StepAction(-1);
        StartCropAction startCropAction = new StartCropAction();
        StopCropAction stopCropAction = new StopCropAction();
        StartLarvaeAction startLarvaeAction = new StartLarvaeAction();
        StopLarvaeAction stopLarvaeAction = new StopLarvaeAction();
        ShowPathAction showPathAction = new ShowPathAction();
        CSVExportAction exportAction = new CSVExportAction();

        //this below is to make arrow keys work for changing frames
        //create a map of inputs and name them
        InputMap imap = buttonPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke("RIGHT"), "panel.next");
        imap.put(KeyStroke.getKeyStroke("LEFT"), "panel.prev");

        //map those names of inputs to actions
        ActionMap amap = buttonPanel.getActionMap();
        amap.put("panel.next", nextAction);
        amap.put("panel.prev", prevAction);

        //attach the actions to the buttons
        openMovie.addActionListener(openAction);
        nextFrame.addActionListener(nextAction);
        prevFrame.addActionListener(prevAction);
        startCrop.addActionListener(startCropAction);
        endCrop.addActionListener(stopCropAction);
        startLarvaeSelection.addActionListener(startLarvaeAction);
        endLarvaeSelection.addActionListener(stopLarvaeAction);
        showPaths.addActionListener(showPathAction);
        exportCSV.addActionListener(exportAction);

        //add our components and panels as a gridbag layout
        add(buttonPanel, new GBC(1, 0).setFill(GBC.EAST).setWeight(100, 0).setInsets(1));
        add(frame, new GBC(2, 0, 1, 4).setFill(GBC.BOTH).setWeight(800, 800));
        pack();
    }

    public static void main(String[] args) {
        //System.out.println("Hello World!");

        EventQueue.invokeLater(() ->
        {
            GUI frame = new GUI();
            frame.setTitle("The Larvae Tracker 5000");
            WindowListener exitListener = new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e){
                    try {
                        frame.removeDirectory(frame.movie);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    System.exit(0);
                }
            };
            frame.addWindowListener(exitListener);
            //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);


        });


    }
    /** removes directory that was created by ffmpeg **/
    public void removeDirectory(Video movie) throws IOException, InterruptedException {

        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        if (movie != null) {
            //System.out.println( System.getProperty("user.dir") + "/" + movie.getImgDir());
            String[] command = new String[]{"rm", "-rf", System.getProperty("user.dir") + "/" + movie.getImgDir()};
            java.lang.Process p = rt.exec(command);
            p.waitFor();
        }
    }


    /**
     * Allows the user to select a file from the computer
     * Saves the file name to the global variable fileName
     * Saves the directory of the file to the global variable movieDir
     * If a file is selected then all other buttons are made visible and the initially useful ones are enabled
     * If cancel is selected nothing happens
     */
    class OpenL implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            fd.setVisible(true);
            String name = fd.getFile();
            String dir = fd.getDirectory();

            if(name != null){
                try {
                    removeDirectory(movie);
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                fileName = name;
                movieDir = dir;
                currentFrame = 1;


            }

            String startValue;
            startValue = JOptionPane.showInputDialog("Please enter desired start time (in seconds).");
            String endValue;
            endValue = JOptionPane.showInputDialog("Please enter desired end time (in seconds).");

            try {
                movie = new Video(movieDir, fileName, Integer.valueOf(startValue), Integer.valueOf(endValue));
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

//            openMovie.setVisible(false);
//            openMovie.setEnabled(false);

            nextFrame.setVisible(true);
            prevFrame.setVisible(true);
            startCrop.setVisible(true);
            endCrop.setVisible(true);
            startLarvaeSelection.setVisible(true);
            endLarvaeSelection.setVisible(true);
            showPaths.setVisible(true);
            exportCSV.setVisible(true);

            startLarvaeSelection.setEnabled(true);
            endLarvaeSelection.setEnabled(true);
            endCrop.setEnabled(false);
            displayFrameNum.setVisible(true);
            displayFrameNum.setText("Frame " + String.valueOf(currentFrame) + " of " + String.valueOf(movie.getNumImages()));
            displayFrameNum.setEditable(false);

            pack();
            String frameToDraw = movie.getPathToFrame(currentFrame);
            frame.setImage(frameToDraw); //(movie.getPathToFrame(currentFrame));
            validate();
            repaint();
        }

//            if (rVal == JFileChooser.CANCEL_OPTION) {
//
//            }

        public void removeDirectory(Video movie) throws IOException, InterruptedException {

            java.lang.Runtime rt = java.lang.Runtime.getRuntime();
            if (movie != null) {
                //System.out.println( System.getProperty("user.dir") + "/" + movie.getImgDir());
                String[] command = new String[]{"rm", "-rf", System.getProperty("user.dir") + "/" + movie.getImgDir()};
                java.lang.Process p = rt.exec(command);
                p.waitFor();
            }
        }
    }

    /**
     * action that, when activated, changes the image being drawn
     * implements AbstractAction so that it works with Input map we made and the on screen buttons
     */
    private class StepAction extends AbstractAction {
        private int number;
        //private ImageComponent ourFrame;


        public StepAction(int direction) {
            number = direction;
        }

        public void actionPerformed(ActionEvent event) {
            if (currentFrame + number > 0 && currentFrame + number <= movie.getNumImages()) {
                currentFrame += number;
                frame.currentFrame = currentFrame;
                String frameToDraw = movie.getPathToFrame(currentFrame);
                frame.setImage(frameToDraw); //(movie.getPathToFrame(currentFrame));
                displayFrameNum.setText("Frame " + String.valueOf(currentFrame) + " of " + String.valueOf(movie.getNumImages()));
                pack();
                revalidate();
                repaint();
            }
        }
    }

    /**
     * When activated, the action allows a maximum of 2 squares to be made on the Image component
     * Enables the end crop button, and disables the start crop button
     */
    private class StartCropAction implements ActionListener {

        public StartCropAction() {
        }

        public void actionPerformed(ActionEvent event) {
            frame.maxSquares = 2;
            startCrop.setEnabled(false);
            endCrop.setEnabled(true);
            repaint();
        }
    }

    /**
     * Stores the location of the center of the two squares on the screen
     * Removes the squares from the image component and prevents more from being drawn
     * Sends the cropping dimensions to a function that will crop the images
     * Enables "Start Larvae Selection" and "Start Crop" buttons, and disables "End Crop" button
     */

    private class StopCropAction implements ActionListener {

        public StopCropAction() {
        }

        public void actionPerformed(ActionEvent event) {
				cropProgress.setVisible(true);
            try {

                System.out.println(movie.getImgDir());
                BufferedImage image = ImageIO.read(new File(movie.getImgDir() + "/" + "img0001.png"));
                double xratio = image.getWidth(null) / (double) frame.getImage().getWidth(null);
                double yratio = image.getHeight(null) / (double) frame.getImage().getHeight(null);


                point1[0] = (int) (frame.squares.get(0).getCenterX() * xratio);
                point1[1] = (int) (frame.squares.get(0).getCenterY() * yratio);

                point2[0] = (int) (frame.squares.get(1).getCenterX() * xratio);
                point2[1] = (int) (frame.squares.get(1).getCenterY() * yratio);


                frame.remove(frame.squares.get(1));
                frame.remove(frame.squares.get(0));
                frame.maxSquares = 0;
				
				cropProgress.setVisible(true);
				cropProgress.setMaximum(movie.getNumImages());
				cropProgress.setMinimum(0);
				pack();
				revalidate();
				repaint();
				System.out.println("before loop");	
				
				new Thread(new CropThread("crop", point1, point2, movie.getNumImages(), movie.getImgDir(), cropProgress)).start();
				
				repaint();
				movie.setScaleFactor(PreProcessor.setScaleFactor(point1, point2));


                startLarvaeSelection.setEnabled(true);
                startCrop.setEnabled(true);
                endCrop.setEnabled(false);
				
				

                String frameToDraw = movie.getPathToFrame(currentFrame);
                frame.setImage(frameToDraw); //(movie.getPathToFrame(currentFrame));
				

                revalidate();
                repaint();

            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.out.println("Need to have 2 squares to crop the image.");
            }
        }
    }

    /**
     * Allows a maximum of 4 squares to be added to the Image component
     * Disables "Start Crop" and "End Crop" buttons
     * Enables "End Larvae" selection button
     */

    private class StartLarvaeAction implements ActionListener {

        public StartLarvaeAction() {
        }

        public void actionPerformed(ActionEvent event) {
            currentFrame = 1;
            String frameToDraw = movie.getPathToFrame(currentFrame);
            frame.setImage(frameToDraw); //(movie.getPathToFrame(currentFrame));
            displayFrameNum.setText("Frame " + String.valueOf(currentFrame) + " of " + String.valueOf(movie.getNumImages()));
            frame.maxSquares = 5;
            startCrop.setEnabled(false);
            endCrop.setEnabled(false);
            endLarvaeSelection.setEnabled(true);
            pack();
            revalidate();
            repaint();
        }
    }

    private class CSVExportAction implements ActionListener {

        public CSVExportAction() {
        }

        public void actionPerformed(ActionEvent event) {
			int frames = movie.getLarva().get(0).getCoordinates().size();
            CSVExport exporter = new CSVExport(movie.getLarva(), frames);
            exporter.export();
        }
    }

    /**
     * Searches through all the squares in the Image Component and adds their locations as new Larvae to larvae array
     * Removes all the squares from the Image Component and prevent more from being made
     * Disable "Start Larvae Selection" and "End Larvae Selection" buttons
     */
    private class StopLarvaeAction implements ActionListener {

        public StopLarvaeAction() {
        }

        public void actionPerformed(ActionEvent event) {
            for (Rectangle2D r : frame.squares) {
                Larva addition = new Larva(r.getCenterX(), r.getCenterY());

                movie.addLarva(addition);

                frame.larvae.add(addition);
            }
            for (int i = frame.squares.size() - 1; i >= 0; i--) {
                frame.remove(frame.squares.get(i));
            }

            frame.maxSquares = 0;
            startLarvaeSelection.setEnabled(false);
            endLarvaeSelection.setEnabled(false);
            repaint();
        }
    }


    private class ShowPathAction implements ActionListener {

        public ShowPathAction() {

        }

        public void actionPerformed(ActionEvent event) {
            frame.displayPaths = !frame.displayPaths;
            repaint();
        }

    }

}

/**
 * A component that displays a tiled image and allows for movable squares to be painted on it
 */
class ImageComponent extends JComponent {

    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 800;
    private static final int SIDELENGTH = 7;
    public int maxSquares;
    public ArrayList<Rectangle2D> squares;
    public ArrayList<Larva> larvae;
    public int currentFrame;
    public boolean displayPaths;

    private Rectangle2D current; // the square containing the mouse cursor
    private Image image;

    public ImageComponent(String fileName) {

        maxSquares = 0;
        displayPaths = false;
        image = new ImageIcon(fileName).getImage();
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        larvae = new ArrayList<>();
        squares = new ArrayList<>();
        current = null;
        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseMotionHandler());
    }

    public void setImage(String fileName) {
        image = PreProcessor.scale(fileName, this.getWidth(), this.getHeight());
    }

    public Image getImage() {
        return image;
    }


    public void paintComponent(Graphics g) {
        Color[] colors = {Color.cyan, Color.blue, Color.orange, Color.green, Color.red};
        if (image == null) return;

        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);

        // draw the image in the upper-left corner

        g.drawImage(image, 0, 0, null);
        // tile the image across the component
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.red);

        // draw all squares
        for (Rectangle2D r : squares) {
            g2.draw(r);
        }


        //draw lines between larvae positions
        if (displayPaths) {
            for (Larva l : larvae) {
                g2.setColor(colors[larvae.indexOf(l)]);

                for (int i = 0; i < currentFrame-1; i++) {
                    g2.setStroke(new BasicStroke(1));
                    g2.draw(new Line2D.Double(l.getPosition(i)[0], l.getPosition(i)[1], l.getPosition(i + 1)[0], l.getPosition(i + 1)[1]));
                    g2.draw(new Ellipse2D.Double(l.getPosition(i)[0]-3, l.getPosition(i)[1]-3, 6, 6));
                    g2.draw(new Ellipse2D.Double(l.getPosition(i + 1)[0]-3, l.getPosition(i + 1)[1]-3, 6, 6));

                    if (i == currentFrame - 2) {

                    }

                }
                g2.fill(new Ellipse2D.Double(l.getPosition(0)[0]-3, l.getPosition(0)[1]-3, 6, 6));
                g2.drawString(String.valueOf(larvae.indexOf(l) + 1), (int) (l.getPosition(currentFrame)[0] - 3), (int) (l.getPosition(currentFrame)[1] - 3));
            }
        }
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

    public void mouseClicked(MouseEvent event) {
        // remove the current square if double clicked
        current = find(event.getPoint());
        if (current != null && event.getClickCount() >= 2) remove(current);
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

