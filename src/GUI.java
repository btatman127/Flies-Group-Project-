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
    private boolean changeFrameEnabled = false;
    private final static double ZONE_RADIUS = 4.5;

    private final JPanel buttonPanel;
    private final JButton openMovie = new JButton("Open Movie");
    private final JButton nextFrame = new JButton("Next Frame");
    private final JButton prevFrame = new JButton("Previous Frame");
    private final JButton startCrop = new JButton("Start Crop");
    private final JButton confirmCrop = new JButton("Confirm Crop");
    private final JButton startLarvaeSelection = new JButton("Start Larvae Selection");
    private final JButton confirmLarvaeSelection = new JButton("Confirm Larvae Selection");
    private final JCheckBox showPaths = new JCheckBox("Show Larvae Paths", true);
    private final JCheckBox showZones = new JCheckBox("Show Larvae Zones", false);
    private JCheckBox[] toggleZones = new JCheckBox[5];
    private final JButton exportCSV = new JButton(("Export as CSV file"));
    private final JButton screenshot = new JButton(("Screenshot current frame"));
    private final JButton retrackPosition = new JButton("Retrack Larva @ Current Frame");
    private final JButton confirmRetrackPosition = new JButton("Confirm Larva Retrack");
    private final JButton undo = new JButton("Undo");
    private final JProgressBar cropProgress;
    private JTextPane displayFrameNum;
    private int[] point1;
    private int[] point2;
    private FileDialog fd;
    public ImageComponent frame;

    public Stack<Integer> history;
    public final int CLICKING = 0;

    private enum ButtonState{
        INVISIBLE(false, false),
        DISABLED(true, false),
        ENABLED(true, true);

        final boolean visible;
        final boolean enabled;

        ButtonState(boolean visible, boolean enabled) {
            this.visible = visible;
            this.enabled = enabled;
        }
    }

    private enum ProgramState{
        OPEN(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE),
        PRE_CROP(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE),
        CROPPING(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.DISABLED),
        POST_CROP(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.INVISIBLE, ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE),
        SELECTING_LARVAE(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.DISABLED),
        TRACKING(ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.ENABLED, ButtonState.ENABLED,  ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE),
        RETRACKING(ButtonState.ENABLED,  ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.ENABLED, ButtonState.INVISIBLE,  ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.DISABLED);

        final ButtonState openMovie;
        final ButtonState changeFrame;
        final ButtonState startCrop;
        final ButtonState confirmCrop;
        final ButtonState startLarvaeSelection;
        final ButtonState confirmLarvaeSelection;
        final ButtonState showPaths;
        final ButtonState showZones;
        final ButtonState exportPaths;
        final ButtonState retrackPosition;
        final ButtonState confirmRetrackPosition;
        final ButtonState undo;

        ProgramState(ButtonState openMovie, ButtonState changeFrame, ButtonState startCrop,
                     ButtonState confirmCrop, ButtonState startLarvaeSelection, ButtonState confirmLarvaeSelection,
                     ButtonState showPaths, ButtonState showZones, ButtonState exportPaths,
                     ButtonState retrackPosition, ButtonState confirmRetrackPosition, ButtonState undo){
            this.openMovie = openMovie;
            this.changeFrame = changeFrame;
            this.startCrop = startCrop;
            this.confirmCrop = confirmCrop;
            this.startLarvaeSelection = startLarvaeSelection;
            this.confirmLarvaeSelection = confirmLarvaeSelection;
            this.showPaths = showPaths;
            this.showZones = showZones;
            this.exportPaths = exportPaths;
            this.retrackPosition = retrackPosition;
            this.confirmRetrackPosition = confirmRetrackPosition;
            this.undo = undo;
        }
    }

    public GUI() {
        fd = new FileDialog(this, "Choose a File", FileDialog.LOAD);

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        point1 = new int[2];
        point2 = new int[2];

        history = new Stack<>();

        //construct components
        currentFrame = 0;
        tempLarvaIndex = -1;

        cropProgress = new JProgressBar();
        cropProgress.setVisible(false);

        DefaultStyledDocument sd = new DefaultStyledDocument();
        displayFrameNum = new JTextPane(sd);
        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);
        displayFrameNum.setParagraphAttributes(as, true);
        displayFrameNum.setVisible(false);

        //make new panel for buttons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        //add the buttons to the panel
        buttonPanel.add(openMovie);
        buttonPanel.add(nextFrame);
        buttonPanel.add(prevFrame);
        buttonPanel.add(startCrop);
        buttonPanel.add(confirmCrop);
        buttonPanel.add(startLarvaeSelection);
        buttonPanel.add(confirmLarvaeSelection);
        buttonPanel.add(showPaths);
        buttonPanel.add(showZones);
        buttonPanel.add(retrackPosition);
        buttonPanel.add(confirmRetrackPosition);
        buttonPanel.add(exportCSV);
        buttonPanel.add(screenshot);
        buttonPanel.add(displayFrameNum);
        buttonPanel.add(undo);
        buttonPanel.add(cropProgress);

        setButtonStates(ProgramState.OPEN);

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
        ShowZoneAction showZoneAction = new ShowZoneAction();
        CSVExportAction exportAction = new CSVExportAction();
        ScreenshotAction screenshotAction = new ScreenshotAction();
        RetrackPositionAction retrackPositionAction = new RetrackPositionAction(this);
        StopRetrackAction stopRetrackAction = new StopRetrackAction(this);
        UndoAction undoAction = new UndoAction();

        ToggleZoneAction[] toggleZoneActions = new ToggleZoneAction[5];
        for (int i = 0; i < 5; i++) {
            toggleZones[i] = new JCheckBox("Show zones for larva " + (i+1));
            toggleZones[i].setVisible(false);
            buttonPanel.add(toggleZones[i]);
            toggleZoneActions[i] = new ToggleZoneAction(i);
            toggleZones[i].addActionListener(toggleZoneActions[i]);
        }

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
        confirmCrop.addActionListener(stopCropAction);
        startLarvaeSelection.addActionListener(startLarvaeAction);
        confirmLarvaeSelection.addActionListener(stopLarvaeAction);
        showPaths.addActionListener(showPathAction);
        showZones.addActionListener(showZoneAction);
        retrackPosition.addActionListener(retrackPositionAction);
        confirmRetrackPosition.addActionListener(stopRetrackAction);
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

    private void setButtonStates(ProgramState programState){
        openMovie.setVisible(programState.openMovie.visible);
        openMovie.setEnabled(programState.openMovie.enabled);

        prevFrame.setVisible(programState.changeFrame.visible);
        prevFrame.setEnabled(programState.changeFrame.enabled);

        nextFrame.setVisible(programState.changeFrame.visible);
        nextFrame.setEnabled(programState.changeFrame.enabled);

        changeFrameEnabled = programState.changeFrame.enabled;

        startCrop.setVisible(programState.startCrop.visible);
        startCrop.setEnabled(programState.startCrop.enabled);

        confirmCrop.setVisible(programState.confirmCrop.visible);
        confirmCrop.setEnabled(programState.confirmCrop.enabled);

        startLarvaeSelection.setVisible(programState.startLarvaeSelection.visible);
        startLarvaeSelection.setEnabled(programState.startLarvaeSelection.enabled);

        confirmLarvaeSelection.setVisible(programState.confirmLarvaeSelection.visible);
        confirmLarvaeSelection.setEnabled(programState.confirmLarvaeSelection.enabled);

        showPaths.setVisible(programState.showPaths.visible);
        showPaths.setEnabled(programState.showPaths.enabled);

        showZones.setVisible(programState.showZones.visible);
        showZones.setEnabled(programState.showZones.enabled);

        exportCSV.setVisible(programState.exportPaths.visible);
        exportCSV.setEnabled(programState.exportPaths.enabled);

        screenshot.setVisible(programState.exportPaths.visible);
        screenshot.setEnabled(programState.exportPaths.enabled);

        retrackPosition.setVisible(programState.retrackPosition.visible);
        retrackPosition.setEnabled(programState.retrackPosition.enabled);

        confirmRetrackPosition.setVisible(programState.confirmRetrackPosition.visible);
        confirmRetrackPosition.setEnabled(programState.confirmRetrackPosition.enabled);

        undo.setVisible(programState.undo.visible);
        undo.setEnabled(programState.undo.enabled);

        pack();
        revalidate();
        repaint();
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
        private int parseVideoLengthInput(String input) throws NumberFormatException{
            int temp = -1;
            try {
                temp = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
            }

            return temp;
        }

        public void actionPerformed(ActionEvent e) throws NumberFormatException{
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
            currentFrame = 1;

            //Double Option Test
            JTextField startTime = new JTextField();
            JTextField endTime = new JTextField();
            Object[] message = {
                    "Please enter Start and Stop time in seconds.\n Leave blank to default to full video length.",
                    "Movie duration: " + PreProcessor.getDurationSeconds(movieDir, fileName) + " seconds.",
                    "Start time:", startTime,
                    "End Time:", endTime
            };

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Choose Movie Length", JOptionPane.OK_CANCEL_OPTION);
            if(result==JOptionPane.CANCEL_OPTION || result==JOptionPane.CLOSED_OPTION){
                return;
            }

            int startValue = parseVideoLengthInput(startTime.getText());
            int finalTime = parseVideoLengthInput(PreProcessor.getDurationSeconds(movieDir, fileName));
            if (startTime.getText().equals("")) {
                startValue = 0;
            } else if (startValue < 0 || startValue >= finalTime) {
                startValue = 0;
                JOptionPane.showMessageDialog(null, "Invalid Start Time. Defaulting to 0.");
            }

            int endValue = parseVideoLengthInput(endTime.getText());
            if (endTime.getText().equals("")) {
                endValue = finalTime;
            } else if (endValue > finalTime || endValue <= startValue) {
                endValue = finalTime;
                JOptionPane.showMessageDialog(null, "Invalid End Time. Defaulting to " +
                        endValue + ".");
            }

            //Create new movie
            try {
                movie = new Video(movieDir, fileName, startValue, endValue);
            } catch (IOException | InterruptedException e1) {
                e1.printStackTrace();
            }

            frame.movie = movie;
            frame.squares = new ArrayList<>();

            frame.displayPaths = false;
            displayFrameNum.setText("Frame " + currentFrame + " of " + movie.getNumImages());
            displayFrameNum.setEditable(false);
            displayFrameNum.setVisible(true);

            setButtonStates(ProgramState.PRE_CROP);

            pack();
            frame.setImage(movie.getPathToFrame(currentFrame));
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
            if (!changeFrameEnabled) return;

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
            setButtonStates(ProgramState.CROPPING);
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
            if(frame.squares.size() < frame.maxSquares) return;
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

                setButtonStates(ProgramState.POST_CROP);

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
            currentFrame = 0;
            frame.setImage(movie.getPathToFrame(currentFrame + 1));
            displayFrameNum.setText("Frame " + (currentFrame + 1) + " of " + movie.getNumImages());
            frame.maxSquares = 5;
            setButtonStates(ProgramState.SELECTING_LARVAE);
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
            if(frame.squares.size() < 1) return;
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
            frame.displayPaths = true;
            setButtonStates(ProgramState.TRACKING);

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

            frame.maxSquares = 1;
            setButtonStates(ProgramState.RETRACKING);
        }
    }

    private class StopRetrackAction implements ActionListener {
        GUI gui;

        public StopRetrackAction(GUI gui) {
            this.gui = gui;
        }

        public void actionPerformed(ActionEvent event) {
            if(frame.squares.size() < frame.maxSquares) return;
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

                setButtonStates(ProgramState.TRACKING);
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
            CSVExport exporter = new CSVExport(movie, frames, ZONE_RADIUS);
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

    private class ToggleZoneAction implements ActionListener {
        private int index;

        public ToggleZoneAction(int index){
            this.index = index;
        }

        public void actionPerformed(ActionEvent event) {
            frame.zoneToggled[index] = !frame.zoneToggled[index];
            repaint();
        }
    }

    private class ShowZoneAction implements ActionListener {
        public ShowZoneAction(){}

        public void actionPerformed(ActionEvent event) {
            frame.displayZones = !frame.displayZones;

            for (int i = 0; i < movie.getLarva().size(); i++) {
                toggleZones[i].setVisible(frame.displayZones);
                toggleZones[i].setEnabled(frame.displayZones);
                toggleZones[i].setSelected(false);
                frame.zoneToggled[i] = false;

            }
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
        public boolean displayZones;
        public boolean zoneToggled[] = new boolean[5];
        public boolean vidInitialized;
        public Video movie;

        private Rectangle2D currentMouseLocationRectangle;
        private Image image;

        public ImageComponent(String fileName) {
            maxSquares = 0;
            displayPaths = false;
            displayZones = false;

            image = new ImageIcon(getClass().getResource(fileName)).getImage();
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
            ArrayList<Larva> larvae = new ArrayList<>();
            double xratio = 0;
            double yratio = 0;
            if (movie!=null) {
                larvae = movie.getLarva();
                xratio = movie.getDimensions()[0] / (double) image.getWidth(null);
                yratio = movie.getDimensions()[1] / (double) image.getHeight(null);
            }
            //draw lines between larvae positions
            if (displayPaths) {
                for (Larva l : larvae) {
                    g2.setColor(colors[larvae.indexOf(l)]);
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

            if (displayZones){
                double mm = 76.2; //The grid is 3" by 3", which translates into about 76 mm.
                assert movie != null;
                double xScale = movie.getDimensions()[0]/mm;
                double yScale = movie.getDimensions()[0]/mm;
                for (Larva l : larvae) {
                    if(zoneToggled[larvae.indexOf(l)]) {
                        g2.setColor(colors[larvae.indexOf(l)]);
                        for (int i = 1; i < 13; i++) {
                            double radius = ZONE_RADIUS * i;
                            double xRadius = radius * xScale / xratio;
                            double yRadius = radius * yScale / yratio;
                            g2.draw(new Ellipse2D.Double((l.getPosition(0)[0] / xratio) - xRadius,
                                    (l.getPosition(0)[1] / yratio) - yRadius, xRadius * 2, yRadius * 2));
                        }
                    }

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

