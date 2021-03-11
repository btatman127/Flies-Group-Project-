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

public class GUI extends JFrame {
    private int currentFrame;
    private String fileName;
    private String movieDir;
    private Video movie;
    private int tempLarvaIndex;
    private boolean nextPrevFrameEnabled = false;

    private JPanel buttonPanel;
    private JButton openMovie;
    private JButton nextFrame;
    private JButton prevFrame;
    private JButton startCrop;
    private JButton endCrop;
    private JButton startLarvaeSelection;
    private JButton finishLarvaeSelection;
    private JButton exportCSV;
    private JButton screenshot;
    private JButton retrackPosition;
    private JButton stopRetrackPosition;
    private JButton undo;
    private final JProgressBar cropProgress;
    private JCheckBox showPaths;
    private JTextPane displayFrameNum;
    private int[] point1;
    private int[] point2;
    private FileDialog fd;
    public ImageComponent frame;

    public Stack<Integer> history;
    public final int CLICKING = 0;

    public GUI() {
        fd = new FileDialog(this, "Choose a File", FileDialog.LOAD);
        fd.setDirectory("C:\\");

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        point1 = new int[2];
        point2 = new int[2];

        history = new Stack<>();

        //construct components
        currentFrame = 0;
        tempLarvaIndex = -1;

        //make buttons for frames
        openMovie = new JButton("Open Movie");
        nextFrame = new JButton("Next Frame");
        prevFrame = new JButton("Previous Frame");
        startCrop = new JButton("Start Crop");
        endCrop = new JButton("Finish Crop");
        startLarvaeSelection = new JButton("Start Larvae Selection");
        finishLarvaeSelection = new JButton("Finish Larvae Selection");
        showPaths = new JCheckBox("Show Larvae Paths");
        exportCSV = new JButton(("Export as CSV file"));
        screenshot = new JButton(("Screenshot current frame"));
        retrackPosition = new JButton("Retrack Larva @ Current Frame");
        stopRetrackPosition = new JButton("Finish Larva Retrack");
        undo = new JButton("Undo");
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
        buttonPanel.add(finishLarvaeSelection);
        buttonPanel.add(showPaths);
        buttonPanel.add(retrackPosition);
        buttonPanel.add(stopRetrackPosition);
        buttonPanel.add(exportCSV);
        buttonPanel.add(screenshot);
        buttonPanel.add(displayFrameNum);
        buttonPanel.add(undo);
        buttonPanel.add(cropProgress);

        //make sure some of the buttons can't be pressed yet
        nextFrame.setVisible(false);
        prevFrame.setVisible(false);
        startCrop.setVisible(false);
        endCrop.setVisible(false);
        startLarvaeSelection.setVisible(false);
        finishLarvaeSelection.setVisible(false);
        showPaths.setVisible(false);
        retrackPosition.setVisible(false);
        stopRetrackPosition.setVisible(false);
        exportCSV.setVisible(false);
        screenshot.setVisible(false);
        displayFrameNum.setVisible(false);
        cropProgress.setVisible(false);
        undo.setVisible(false);

        disableNextPrevFrame();

        //add the image component to the screen
        frame = new ImageComponent("welcome.png");
        frame.setBorder(BorderFactory.createEtchedBorder());

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
        ScreenshotAction screenshotAction = new ScreenshotAction();
        RetrackPositionAction retrackPositionAction = new RetrackPositionAction(this);
        StopRetrackAction stopRetrackAction = new StopRetrackAction(this);
        UndoAction undoAction = new UndoAction();

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
        finishLarvaeSelection.addActionListener(stopLarvaeAction);
        showPaths.addActionListener(showPathAction);
        retrackPosition.addActionListener(retrackPositionAction);
        stopRetrackPosition.addActionListener(stopRetrackAction);
        exportCSV.addActionListener(exportAction);
        screenshot.addActionListener(screenshotAction);
        undo.addActionListener(undoAction);

        //add our components and panels as a gridbag layout
        add(buttonPanel, new GBC(1, 0).setFill(GBC.EAST).setWeight(100, 0).setInsets(1));
        add(frame, new GBC(2, 0, 1, 4).setFill(GBC.BOTH).setWeight(800, 800));
        pack();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() ->
        {
            GUI frame = new GUI();
            frame.setTitle("The Larvae Tracker 5000");
            WindowListener exitListener = new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    if(frame.movie!= null) {
                        frame.deleteDirectory(frame.movie.getImgDir());
                        frame.deleteDirectory(frame.movie.getOutputPathLong());
                    }
                    System.exit(0);
                }
            };
            frame.addWindowListener(exitListener);
            //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    /**
     * disable next frame and previous buttons
     * disable arrow keys
     */
    private void disableNextPrevFrame() {
        nextFrame.setEnabled(false);
        prevFrame.setEnabled(false);
        nextPrevFrameEnabled = false;
    }

    /**
     * enable next frame and previous buttons
     * enable arrow keys
     */
    private void enableNextPrevFrame() {
        nextFrame.setEnabled(true);
        prevFrame.setEnabled(true);
        nextPrevFrameEnabled = true;
    }

    boolean deleteDirectory(String dirName) {
        if (dirName == null) return false;
        else return deleteDirectory(new File(dirName));
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        if (!directoryToBeDeleted.exists()) return false;
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public int getTempLarvaIndex() {
        return tempLarvaIndex;
    }

    public void setTempLarvaIndex(int tempLarvaIndex) {
        this.tempLarvaIndex = tempLarvaIndex;
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
            //File Dialog to Select Movie to Open
            fd.setVisible(true);
            String name = fd.getFile();
            String dir = fd.getDirectory();

            //if user hits Cancel
            if (name == null) {
                return;
            }

            if (movie != null) {
                deleteDirectory(frame.movie.getOutputPathLong());
                deleteDirectory(movie.getImgDir());
            }
            fileName = name;
            movieDir = dir;
            currentFrame = 0;

            //Double Option Test
            String startValue;
            String endValue;
            JTextField startTime = new JTextField();
            JTextField endTime = new JTextField();
            Object[] message = {
                    "Please enter Start and Stop time in seconds.",
                    "Movie duration: " + PreProcessor.getDurationSeconds(movieDir, fileName) + " seconds.",
                    "Start time:", startTime,
                    "End Time:", endTime
            };

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Choose Movie Length", JOptionPane.OK_CANCEL_OPTION);
            if(result==JOptionPane.CANCEL_OPTION || result==JOptionPane.CLOSED_OPTION){
                return;
            }

            if (startTime.getText().equals("") || Integer.parseInt(startTime.getText()) < 0) {
                startValue = "0";
            } else {
                startValue = startTime.getText();
            }

            if (endTime.getText().equals("") || Integer.parseInt(endTime.getText()) >
                    Integer.parseInt(PreProcessor.getDurationSeconds(movieDir, fileName))) {
                endValue = PreProcessor.getDurationSeconds(movieDir, fileName);
            } else {
                endValue = endTime.getText();
            }

            while (!PreProcessor.validateTime(startTime.getText(),
                    PreProcessor.getDurationSeconds(movieDir, fileName)) ||
                    !PreProcessor.validateTime(endTime.getText(),
                            PreProcessor.getDurationSeconds(movieDir, fileName))) {
                System.out.println("Invalid Time. ");
                JOptionPane.showMessageDialog(null, message);

                if (PreProcessor.validateTime(startTime.getText(),
                        PreProcessor.getDurationSeconds(movieDir, fileName)) &&
                        PreProcessor.validateTime(endTime.getText(),
                                PreProcessor.getDurationSeconds(movieDir, fileName))) {
                    startValue = startTime.getText();
                    endValue = endTime.getText();
                }
            }

            //Create new movie
            try {
                movie = new Video(movieDir, fileName, Integer.parseInt(startValue), Integer.parseInt(endValue));
            } catch (IOException | InterruptedException e1) {
                e1.printStackTrace();
            }

            frame.movie = movie;
            frame.squares = new ArrayList<>();
            //DIRECTLY AFTER OPENING MOVIE FILE
            nextFrame.setVisible(true);
            prevFrame.setVisible(true);
            startCrop.setVisible(true);
            endCrop.setVisible(true);
            startLarvaeSelection.setVisible(true);
            finishLarvaeSelection.setVisible(true);
            showPaths.setVisible(true);
            exportCSV.setVisible(true);
            screenshot.setVisible(true);
            retrackPosition.setVisible(true);
            stopRetrackPosition.setVisible(true);
            undo.setVisible(true);

            startCrop.setEnabled(true);
            startLarvaeSelection.setEnabled(false);
            finishLarvaeSelection.setEnabled(false);
            endCrop.setEnabled(false);
            showPaths.setEnabled(false);
            showPaths.setSelected(false);
            frame.displayPaths = false;
            exportCSV.setEnabled(false);
            screenshot.setEnabled(false);
            retrackPosition.setEnabled(false);
            stopRetrackPosition.setEnabled(false);
            displayFrameNum.setVisible(true);
            displayFrameNum.setText("Frame " + currentFrame + " of " + movie.getNumImages());
            displayFrameNum.setEditable(false);
            undo.setEnabled(false);

            pack();
            frame.setImage(movie.getPathToFrame(currentFrame + 1));
            validate();
            repaint();
        }
    }

    /**
     * action that, when activated, changes the image being drawn
     * implements AbstractAction so that it works with Input map we made and the on screen buttons
     */
    private class StepAction extends AbstractAction {
        private int number;

        public StepAction(int direction) {
            number = direction;
        }

        public void actionPerformed(ActionEvent event) {
            if (!nextPrevFrameEnabled) return;

            if (currentFrame + number >= 0 && currentFrame + number < movie.getNumImages()) {
                currentFrame += number;
                frame.currentFrame = currentFrame;
                frame.setImage(movie.getPathToFrame(currentFrame + 1));

                displayFrameNum.setText("Frame " + (currentFrame + 1) + " of " + movie.getNumImages());
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
            frame.squares = new ArrayList<>();
            startCrop.setEnabled(false);
            endCrop.setEnabled(true);
            startLarvaeSelection.setEnabled(false);
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

                new CropImages(point1, point2, movie.getNumImages(),
                        movie.getImgDir(), cropProgress).run();
                pack();
                revalidate();
                repaint();

                startLarvaeSelection.setEnabled(true);
                startCrop.setEnabled(false);
                endCrop.setEnabled(false);
                undo.setEnabled(false);

                history = new Stack<>();

            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.out.println("Need to have 2 squares to crop the image.");
            }

            currentFrame = 1;
            frame.setImage(movie.getPathToFrame(currentFrame));
            displayFrameNum.setText("Frame " + currentFrame + " of " + movie.getNumImages());
            revalidate();
            repaint();

            history = new Stack<>();
            undo.setEnabled(false);
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
            frame.setImage(movie.getPathToFrame(currentFrame));
            displayFrameNum.setText("Frame " + currentFrame + " of " + movie.getNumImages());
            frame.maxSquares = 5;
            startCrop.setEnabled(false);
            endCrop.setEnabled(false);
            startLarvaeSelection.setEnabled(false);
            finishLarvaeSelection.setEnabled(true);
            pack();
            revalidate();
            repaint();
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
            boolean collisionFound;
            double xratio = movie.getDimensions()[0] / (double) frame.getImage().getWidth(null);
            double yratio = movie.getDimensions()[1] / (double) frame.getImage().getHeight(null);
            for (Rectangle2D r : frame.squares) {
                Larva addition = new Larva(r.getCenterX() * xratio, r.getCenterY() * yratio);

                movie.addLarva(addition);
            }

            undo.setEnabled(false);
            history = new Stack<>();

            for (int i = frame.squares.size() - 1; i >= 0; i--) {
                frame.remove(frame.squares.get(i));
            }

            frame.maxSquares = 0;
            startLarvaeSelection.setEnabled(false);
            finishLarvaeSelection.setEnabled(false);
            showPaths.setEnabled(true);
            showPaths.setSelected(true);
            frame.displayPaths = true;
            exportCSV.setEnabled(true);
            screenshot.setEnabled(true);
            retrackPosition.setVisible(true);
            retrackPosition.setEnabled(true);
            stopRetrackPosition.setVisible(true);
            stopRetrackPosition.setEnabled(false);
            enableNextPrevFrame();

            //Initializes the tracking process within the Video class
            collisionFound = movie.createFrames();
            if (collisionFound) {
                Object[] message = {
                        "A collision was detected at frame #" + (movie.getCollisionFrameIndex(0) + 1) + "."
                };
                JOptionPane.showMessageDialog(null, message);
            }
            frame.vidInitialized = true;
            buttonPanel.requestFocus();
            repaint();
        }
    }

    private class RetrackPositionAction implements ActionListener {
        GUI gui;

        public RetrackPositionAction(GUI gui) {
            this.gui = gui;
        }

        public void actionPerformed(ActionEvent event) {
            disableNextPrevFrame();

            String[] larvaeNumber = new String[movie.getLarva().size()];
            for (int i = 0; i < movie.getLarva().size(); i++) {
                larvaeNumber[i] = "" + (i + 1);
            }

            for (int i = 0; i < movie.getLarva().size(); i++) {
                larvaeNumber[i] = "" + (i + 1);
            }

            gui.setTempLarvaIndex(-1);
            JComboBox larvaNumberOption = new JComboBox(larvaeNumber);
            Object[] message = {
                    "Please select larva number to retrack position.",
                    larvaNumberOption,
                    "Select ok and then select new point."
            };

            JOptionPane.showMessageDialog(null, message);
            gui.setTempLarvaIndex(larvaNumberOption.getSelectedIndex());

            undo.setEnabled(false); //change in the future to allow user to not retrack if they missclicked
            if(gui.tempLarvaIndex == -1) {
                stopRetrackPosition.setEnabled(false);
                retrackPosition.setEnabled(true);
                exportCSV.setEnabled(true);
                screenshot.setEnabled(true);
                frame.maxSquares = 0;
            } else {
                stopRetrackPosition.setEnabled(true);
                retrackPosition.setEnabled(false);
                exportCSV.setEnabled(false);
                screenshot.setEnabled(false);
                frame.maxSquares = 1;
            }
        }
    }

    private class StopRetrackAction implements ActionListener {
        GUI gui;

        public StopRetrackAction(GUI gui) {
            this.gui = gui;
        }

        public void actionPerformed(ActionEvent event) {
            try {
                BufferedImage image = ImageIO.read(new File(movie.getImgDir() + "/" + "img0001.png"));
                double xratio = image.getWidth(null) / (double) frame.getImage().getWidth(null);
                double yratio = image.getHeight(null) / (double) frame.getImage().getHeight(null);

                Double[] pt = new Double[2];
                if (frame.squares.size() > 0) {
                    pt[0] = (frame.squares.get(0).getCenterX() * xratio);
                    pt[1] = (frame.squares.get(0).getCenterY() * yratio);

                    movie.retrackLarvaPositiom(currentFrame, gui.getTempLarvaIndex(), pt);
                    frame.remove(frame.squares.get(0));
                    frame.maxSquares = 0;
                    movie.retrackLarvaPositiom(currentFrame, gui.getTempLarvaIndex(), pt);
                }

                stopRetrackPosition.setEnabled(false);
                retrackPosition.setEnabled(true);
                enableNextPrevFrame();
                exportCSV.setEnabled(true);
                screenshot.setEnabled(true);
                undo.setEnabled(false);
                history = new Stack<>();

                buttonPanel.requestFocus();
                revalidate();
                repaint();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private class UndoAction implements ActionListener {
        public UndoAction() {
        }

        public void actionPerformed(ActionEvent event) {
            history.pop();
            frame.squares.remove(frame.squares.size() - 1);
            repaint();
            if (history.isEmpty()) {undo.setEnabled(false);}
        }
    }

    private class CSVExportAction implements ActionListener {
        public CSVExportAction() {
        }

        public void actionPerformed(ActionEvent event) {

            FileDialog fd = new FileDialog(GUI.this, "Select where to save csv", FileDialog.SAVE);
            fd.setFile(movie.getOriginalMovieName() + ".csv");
            fd.setVisible(true);
            String name = fd.getDirectory() + fd.getFile();
            if (!name.endsWith(".csv")) name = name + ".csv";
            File file = new File(name);

            int frames = 0;
            for (Larva larva : movie.getLarva()) {
                if (larva.getCoordinates().size() > frames) {
                    frames = larva.getCoordinates().size();
                }
            }
            CSVExport exporter = new CSVExport(movie, frames);
            exporter.export(file);
        }
    }

    private class ScreenshotAction implements ActionListener {
        public ScreenshotAction() {
        }

        public void actionPerformed(ActionEvent event) {
            FileDialog fd = new FileDialog(GUI.this, "Select where to save image", FileDialog.SAVE);
            String defaultName = movie.getOriginalMovieName() + ".frame_" + (currentFrame + 1) + ".png";
            fd.setFile(defaultName);
            fd.setVisible(true);
            BufferedImage bi = new BufferedImage(frame.getImage().getWidth(null), frame.getImage().getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            frame.paint(g);
            g.dispose();

            String name = fd.getDirectory() + fd.getFile();
            if (!name.endsWith(".png")) name = name + ".png";
            File file = new File(name);
            try {
                ImageIO.write(bi, "png", file);
            } catch (IOException e) {
                e.printStackTrace();

            }
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


    /**
     * A component that displays a tiled image and allows for movable squares to be painted on it
     */
    class ImageComponent extends JComponent {

        private static final int DEFAULT_WIDTH = 1000;
        private static final int DEFAULT_HEIGHT = 800;
        private static final int SIDELENGTH = 7;
        public int maxSquares;
        public ArrayList<Rectangle2D> squares;
        public int currentFrame;
        public boolean displayPaths;
        public boolean vidInitialized;
        public Video movie;

        private Rectangle2D currentMouseLocationRectangle;
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
            squares = new ArrayList<>();
            currentMouseLocationRectangle = null;
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
                ArrayList<Larva> larvae = movie.getLarva();
                for (Larva l : larvae) {
                    g2.setColor(colors[larvae.indexOf(l)]);
                    double xratio = movie.getDimensions()[0] / (double) image.getWidth(null);
                    double yratio = movie.getDimensions()[1] / (double) image.getHeight(null);
                    for (int i = 0; i < currentFrame; i++) {

                        //convert pt image space --> window space
                        // img_pt * winWidth/imageWidth
                        if (i + 1 >= l.getPositionsSize()) {
                            break;
                        }
                        g2.setStroke(new BasicStroke(1));
                        if (l.getPosition(i) != null) {
                            if (l.getPosition(i + 1) != null) {
                                g2.draw(new Line2D.Double((l.getPosition(i)[0]) / xratio + 3,
                                        (l.getPosition(i)[1]) / yratio + 3,
                                        (l.getPosition(i + 1)[0]) / xratio + 3,
                                        (l.getPosition(i + 1)[1]) / yratio + 3));
                                g2.draw(new Ellipse2D.Double((l.getPosition(i)[0]) / xratio,
                                        (l.getPosition(i)[1]) / yratio, 6, 6));
                                g2.draw(new Ellipse2D.Double((l.getPosition(i + 1)[0]) / xratio,
                                        (l.getPosition(i + 1)[1]) / yratio, 6, 6));
                            } else {
                                for (int j = i + 2; j < l.getPositionsSize(); j++) {
                                    if (l.getPosition(j) != null) {
                                        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT,
                                                BasicStroke.JOIN_BEVEL, 0,
                                                new float[]{9}, 0);
                                        g2.setStroke(dashed);
                                        g2.draw(new Line2D.Double((l.getPosition(i)[0]) / xratio + 3,
                                                (l.getPosition(i)[1]) / yratio + 3,
                                                (l.getPosition(j)[0]) / xratio + 3,
                                                (l.getPosition(j)[1]) / yratio + 3));
                                        g2.setStroke(new BasicStroke());
                                        g2.draw(new Ellipse2D.Double((l.getPosition(i)[0]) / xratio,
                                                (l.getPosition(i)[1]) / yratio, 6, 6));
                                        g2.draw(new Ellipse2D.Double((l.getPosition(j)[0]) / xratio,
                                                (l.getPosition(j)[1]) / yratio, 6, 6));
                                        break;
                                    }
                                }
                            }
                        }
                        if (i == currentFrame - 1 && l.getPosition(i + 1) != null) {
                            g2.drawString(String.valueOf(larvae.indexOf(l) + 1),
                                    (int) ((l.getPosition(i + 1)[0]) / xratio - 3),
                                    (int) ((l.getPosition(i + 1)[1]) / yratio - 3));
                        }
                    }
                    g2.fill(new Ellipse2D.Double(l.getPosition(0)[0] / xratio,
                            l.getPosition(0)[1] / yratio, 6, 6));
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

            currentMouseLocationRectangle = new Rectangle2D.Double(x - SIDELENGTH / 2.0, y - SIDELENGTH / 2.0,
                    SIDELENGTH, SIDELENGTH);
            squares.add(currentMouseLocationRectangle);
            history.push(CLICKING);
            undo.setEnabled(true);
            repaint();
        }

        /**
         * Removes a square from the collection.
         *
         * @param s the square to remove
         */
        public void remove(Rectangle2D s) {
            if (s == null) return;
            if (s == currentMouseLocationRectangle) currentMouseLocationRectangle = null;
            squares.remove(s);
            repaint();
        }

        private class MouseHandler extends MouseAdapter {

            /**
             * add a new square if the cursor isn't inside a square
             */
            public void mousePressed(MouseEvent event) {
                currentMouseLocationRectangle = find(event.getPoint());
                if (squares.size() < maxSquares) {
                    if (currentMouseLocationRectangle == null) add(event.getPoint());
                }
            }

            /**
             * remove the current square if double clicked
             */
            public void mouseClicked(MouseEvent event) {
                currentMouseLocationRectangle = find(event.getPoint());
                if (currentMouseLocationRectangle != null && event.getClickCount() >= 2)
                    remove(currentMouseLocationRectangle);
            }
        }

        private class MouseMotionHandler implements MouseMotionListener {
            /**
             * set the mouse cursor to cross hairs if it is inside a rectangle
             */
            public void mouseMoved(MouseEvent event) {

                if (find(event.getPoint()) == null) setCursor(Cursor.getDefaultCursor());
                else setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }

            public void mouseDragged(MouseEvent event) {
                if (currentMouseLocationRectangle != null) {
                    int x = event.getX();
                    int y = event.getY();

                    // drag the current rectangle to center it at (x, y)
                    currentMouseLocationRectangle.setFrame(x - SIDELENGTH / 2.0, y - SIDELENGTH / 2.0, SIDELENGTH, SIDELENGTH);
                    repaint();
                }
            }
        }
    }


}

