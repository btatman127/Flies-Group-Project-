import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.awt.geom.*;
import java.util.List;

import static java.lang.System.exit;

public class GUI extends JFrame {
    private static final String DOCUMENTATION_URL = "https://docs.google.com/document/d/1sjLI7ZV7KjzImU58LhgWHW0HjSjJrRMwSgS6w88wju8/edit";
    private final static int MAX_LARVAE = 5;
    //The grid is 3" by 3", and we convert to millimeters
    private static final double GRID_DIMENSIONS = 76.2;
    private final static int DEFAULT_DARKNESS_THRESHOLD = 204;

    private File originalMovie;
    private Video movie;
    private int tempLarvaIndex;
    private boolean changeFrameEnabled = false;
    private double zoneRadius = 4.5;

    private final JPanel buttonPanel;
    private final JButton openMovie = new JButton("Open Movie");
    private final JButton nextFrame = new JButton("Next Frame");
    private final JButton prevFrame = new JButton("Previous Frame");
    private final JButton startCrop = new JButton("Start Crop");
    private final JButton confirmCrop = new JButton("Confirm Crop");
    private final JButton startLarvaeSelection = new JButton("Start Larvae Selection");
    private final JButton confirmLarvaeSelection = new JButton("Confirm Larvae Selection");
    private final JCheckBox showPaths = new JCheckBox("Show Larvae Paths", true);
    private final CheckboxPanel pathCheckboxes = new CheckboxPanel(
        MAX_LARVAE,
        true,
        i -> "Show path for larva " + (i + 1),
        TogglePathAction::new
    );
    private final JCheckBox showZones = new JCheckBox("Show Larvae Zones", false);
    private final CheckboxPanel zoneCheckboxes = new CheckboxPanel(
        MAX_LARVAE,
        false,
        i -> "Show zones for larva " + (i + 1),
        ToggleZoneAction::new
    );
    private final JButton setZoneRadius = new JButton("Set zone radius");
    private final JButton exportCSV = new JButton(("Export as CSV file"));
    private final JButton screenshot = new JButton(("Screenshot current frame"));
    private final JButton retrackPosition = new JButton("Retrack Larva @ Current Frame");
    private final JButton stopTracking = new JButton("Stop Tracking Larva");
    private final JButton confirmRetrackPosition = new JButton("Confirm Larva Retrack");
    private final JButton undo = new JButton("Undo");
    private final JProgressBar cropProgress;
    private JSlider darknessThreshold = new JSlider(0, 255, DEFAULT_DARKNESS_THRESHOLD);
    private final JLabel sliderValue = new JLabel();
    private final JButton swapImage = new JButton("Show detected larvae");
    private JTextPane displayFrameNum;
    private JTextPane displayZoneRadius;
    private int[] point1;
    private int[] point2;
    private FileDialog fd;
    public ImageComponent frame;

    private boolean sliderUseable = false;

    public Stack<Integer> history;
    public final int CLICKING = 0;

    private enum ButtonState {
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

    private enum ProgramState {
        OPEN(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE),
        PRE_CROP(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE),
        CROPPING(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.DISABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE),
        POST_CROP(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.INVISIBLE, ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.DISABLED, ButtonState.INVISIBLE),
        SELECTING_LARVAE(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.INVISIBLE),
        TRACKING(ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.ENABLED, ButtonState.ENABLED),
        RETRACKING(ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE,
                ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.ENABLED, ButtonState.INVISIBLE, ButtonState.INVISIBLE, ButtonState.ENABLED,
                ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.INVISIBLE);

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
        final ButtonState darknessThreshold;
        final ButtonState stopTracking;

        ProgramState(ButtonState openMovie, ButtonState changeFrame, ButtonState startCrop,
                     ButtonState confirmCrop, ButtonState startLarvaeSelection, ButtonState confirmLarvaeSelection,
                     ButtonState showPaths, ButtonState showZones, ButtonState exportPaths,
                     ButtonState retrackPosition, ButtonState confirmRetrackPosition, ButtonState undo,
                     ButtonState darknessThreshold, ButtonState stopTracking) {
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
            this.darknessThreshold = darknessThreshold;
            this.stopTracking = stopTracking;
        }
    }

    public GUI() {
        fd = new FileDialog(this, "Choose a File", FileDialog.LOAD);

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        point1 = new int[2];
        point2 = new int[2];

        history = new Stack<>();

        tempLarvaIndex = -1;

        cropProgress = new JProgressBar();
        cropProgress.setVisible(false);

        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);

        displayFrameNum = new JTextPane(new DefaultStyledDocument());
        displayFrameNum.setParagraphAttributes(as, true);
        displayFrameNum.setVisible(false);
        displayFrameNum.setEditable(false);

        displayZoneRadius = new JTextPane(new DefaultStyledDocument());
        displayZoneRadius.setParagraphAttributes(as, true);
        displayZoneRadius.setVisible(false);
        displayZoneRadius.setEditable(false);
        displayZoneRadius.setText("Zone radius: " + zoneRadius + " mm");

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
        buttonPanel.add(pathCheckboxes);
        pathCheckboxes.setVisible(false);

        buttonPanel.add(showZones);
        buttonPanel.add(zoneCheckboxes);
        zoneCheckboxes.setVisible(false);
        buttonPanel.add(setZoneRadius);
        buttonPanel.add(displayZoneRadius);
        setZoneRadius.setVisible(false);

        buttonPanel.add(retrackPosition);
        buttonPanel.add(confirmRetrackPosition);
        buttonPanel.add(stopTracking);
        buttonPanel.add(exportCSV);
        buttonPanel.add(screenshot);
        buttonPanel.add(displayFrameNum);
        buttonPanel.add(undo);
        buttonPanel.add(cropProgress);
        buttonPanel.add(swapImage);
        buttonPanel.add(darknessThreshold);
        buttonPanel.add(sliderValue);

        darknessThreshold.setMajorTickSpacing(25);
        darknessThreshold.setPaintLabels(true);
        darknessThreshold.setPaintTicks(true);

        //add the image component to the screen
        frame = new ImageComponent("welcome.png");
        frame.setBorder(BorderFactory.createEtchedBorder());

        // Set drag-and-drop target
        setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    File video = droppedFiles.get(0);
                    initializeMovie(video);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        //this below is to make arrow keys work for changing frames
        //create a map of inputs and name them
        InputMap imap = buttonPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke("RIGHT"), "panel.next");
        imap.put(KeyStroke.getKeyStroke("LEFT"), "panel.prev");


        AbstractAction nextFrameAction = new StepAction(1);
        AbstractAction previousFrameAction = new StepAction(-1);
        //map those names of inputs to actions
        ActionMap amap = buttonPanel.getActionMap();
        amap.put("panel.next", nextFrameAction);
        amap.put("panel.prev", previousFrameAction);

        //attach the actions to the buttons
        openMovie.addActionListener(new VideoSelectionAction());
        nextFrame.addActionListener(nextFrameAction);
        prevFrame.addActionListener(previousFrameAction);
        startCrop.addActionListener(new StartCropAction());
        confirmCrop.addActionListener(new StopCropAction());
        startLarvaeSelection.addActionListener(new StartLarvaeAction());
        confirmLarvaeSelection.addActionListener(new StopLarvaeAction());
        showPaths.addActionListener(new ShowPathAction());
        showZones.addActionListener(new ShowZoneAction());
        setZoneRadius.addActionListener(new SetZoneRadiusAction());
        retrackPosition.addActionListener(new RetrackPositionAction());
        stopTracking.addActionListener(new StopTrackingAction());
        confirmRetrackPosition.addActionListener(new StopRetrackAction());
        exportCSV.addActionListener(new CSVExportAction());
        screenshot.addActionListener(new ScreenshotAction());
        undo.addActionListener(new UndoAction());
        darknessThreshold.addChangeListener(new SliderAction());
        swapImage.addActionListener(new SwapAction());

        //add our components and panels as a gridbag layout
        add(buttonPanel, new GBC(1, 0).setFill(GBC.EAST).setWeight(100, 0).setInsets(1));
        add(frame, new GBC(2, 0, 1, 4).setFill(GBC.BOTH).setWeight(800, 800));

        setButtonStates(ProgramState.OPEN);
        
        render();
    }

    private static boolean isffmpegInstalled() {
        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec(new String[]{"ffmpeg"});
            rt.exec(new String[]{"ffprobe"});
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() ->
        {
            GUI frame = new GUI();
            frame.setTitle("The Larvae Tracker 6000");
            if (!isffmpegInstalled()) {
                try {
                    Desktop.getDesktop().browse(new URL(DOCUMENTATION_URL).toURI());
                    JOptionPane.showMessageDialog(null,
                            "ffmpeg is not installed.\nSee ffmpeg installation instructions.\nProgram exiting.");
                } catch (IOException | URISyntaxException e) {
                    // Ignore exceptions because we exit anyway.
                } finally {
                    exit(1);
                }
            }

            WindowListener exitListener = new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    if (frame.movie != null) {
                        frame.deleteDirectory(frame.movie.getImgDir());
                    }

                    exit(0);
                }
            };
            frame.addWindowListener(exitListener);
            //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private void setButtonStates(ProgramState programState) {
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

        stopTracking.setVisible(programState.stopTracking.visible);
        stopTracking.setEnabled(programState.stopTracking.enabled);

        confirmRetrackPosition.setVisible(programState.confirmRetrackPosition.visible);
        confirmRetrackPosition.setEnabled(programState.confirmRetrackPosition.enabled);

        undo.setVisible(programState.undo.visible);
        undo.setEnabled(programState.undo.enabled);

        if (programState != ProgramState.TRACKING && programState != ProgramState.RETRACKING) {
            resetButtons();
        }

        darknessThreshold.setVisible(programState.darknessThreshold.visible);
        darknessThreshold.setEnabled(programState.darknessThreshold.enabled);
        darknessThreshold.setPaintLabels(programState.darknessThreshold.visible);
        darknessThreshold.setPaintTicks(programState.darknessThreshold.visible);
        sliderUseable = programState.darknessThreshold.visible;

        sliderValue.setVisible(programState.darknessThreshold.visible);
        sliderValue.setEnabled(programState.darknessThreshold.enabled);

        swapImage.setVisible(programState.darknessThreshold.visible);
        swapImage.setEnabled(programState.darknessThreshold.enabled);

        if (programState == ProgramState.TRACKING) {
            pathCheckboxes.setVisible(frame.displayPaths);
            pathCheckboxes.reset(movie.getLarva().size());
            zoneCheckboxes.setVisible(frame.displayZones);
            zoneCheckboxes.reset(movie.getLarva().size());
        }
        else if(programState == ProgramState.PRE_CROP){
            resetButtons();
        }

        render();
    }

    private void resetButtons() {
        frame.displayPaths = true;
        showPaths.setSelected(frame.displayPaths);
        pathCheckboxes.setVisible(false);
        pathCheckboxes.reset(MAX_LARVAE);

        frame.displayZones = false;
        setZoneRadius.setVisible(frame.displayZones);
        displayZoneRadius.setVisible(frame.displayZones);
        showZones.setSelected(frame.displayZones);
        zoneCheckboxes.setVisible(false);
        zoneCheckboxes.reset(MAX_LARVAE);

        for (int i = 0; i < MAX_LARVAE; i++) {
            frame.zoneToggled[i] = frame.displayZones;
            frame.pathToggled[i] = frame.displayPaths;
        }
        resetSliderState();

    }

    private void resetSliderState() {
        sliderUseable = false;
        darknessThreshold.setValue(DEFAULT_DARKNESS_THRESHOLD);
        sliderValue.setText("Darkness Threshold = " + DEFAULT_DARKNESS_THRESHOLD + ".");
    }

    boolean deleteDirectory(Path dirName) {
        if (dirName == null) return false;
        else return deleteDirectory(dirName.toFile());
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

    private int parseVideoLengthInput(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }


    private void createMovie(int startValue, int endValue) {
        try {
            movie = new Video(originalMovie, startValue, endValue, DEFAULT_DARKNESS_THRESHOLD);
        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }

        frame.movie = movie;
        frame.squares = new ArrayList<>();

        frame.displayPaths = false;
        frame.displayLarvaLocationOverlay = false;
        swapImage.setText("Show detected larvae");
        displayFrameNum.setText("Frame " + frame.currentFrame + " of " + movie.getNumImages());
        displayFrameNum.setEditable(false);
        displayFrameNum.setVisible(true);

        setButtonStates(ProgramState.PRE_CROP);
        try {
            frame.setImage(movie.getPathToFrame(frame.currentFrame));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not find image.");
            e.printStackTrace();
            exit(1);
        }
        render();
    }

    private void openMovieDurationDialog() {
        int finalTime;
        try {
            finalTime = PreProcessor.getVideoDuration(originalMovie);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(null, "Could not find video.");
            error.printStackTrace();
            return;
        }

        JTextField startTime = new JTextField();
        JTextField endTime = new JTextField();
        Object[] message = {
                "Please enter Start and Stop time in seconds.",
                "Leave blank to default to full video length.",
                "Movie duration: " + finalTime + " seconds.",
                "Start time:", startTime,
                "End Time:", endTime
        };

        JOptionPane.showConfirmDialog(null, message,
                "Choose Movie Length", JOptionPane.DEFAULT_OPTION);

        int startValue = parseVideoLengthInput(startTime.getText());

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

        createMovie(startValue, endValue);
    }

    public void initializeMovie(File video) {
        if (movie != null) {
            deleteDirectory(movie.getImgDir());
        }

        originalMovie = video;
        frame.currentFrame = 1;

        openMovieDurationDialog();
    }

    private void render() {
        pack();
        validate();
        repaint();
    }

    /**
     * Opens a file dialog to let the user select a movie to open.
     * Code that runs for both drag-and-drop and the file dialog should go in initializeMovie();
     */
    private class VideoSelectionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) throws NumberFormatException {
            //File Dialog to Select Movie to Open
            fd.setVisible(true);
            File[] files = fd.getFiles();

            //if user hits Cancel
            if (files.length == 0) {
                return;
            }
            initializeMovie(files[0]);
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

            if (frame.currentFrame + number >= 0 && frame.currentFrame + number < movie.getNumImages()) {
                frame.currentFrame += number;
                try {
                    frame.setImage(movie.getPathToFrame(frame.currentFrame + 1));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Could not find image.");
                    e.printStackTrace();
                    exit(1);
                }
                displayFrameNum.setText("Frame " + (frame.currentFrame + 1) + " of " + movie.getNumImages());
                if (number == 1) {
                    try {
                        movie.createFrame(frame.currentFrame);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Could not find image.");
                        e.printStackTrace();
                        exit(1);
                    }
                }
                frame.setBlackAndWhiteImage(movie.findLarvaeLocation(frame.currentFrame));
                render();
            }
        }
    }

    /**
     * When activated, the action allows a maximum of 2 squares to be made on the Image component
     * Enables the end crop button, and disables the start crop button
     */
    private class StartCropAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            frame.maxSquares = 2;
            frame.squares = new ArrayList<>();
            setButtonStates(ProgramState.CROPPING);
            render();
        }
    }

    /**
     * Stores the location of the center of the two squares on the screen
     * Removes the squares from the image component and prevents more from being drawn
     * Sends the cropping dimensions to a function that will crop the images
     * Enables "Start Larvae Selection" and "Start Crop" buttons, and disables "End Crop" button
     */
    private class StopCropAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (frame.squares.size() < frame.maxSquares) return;
            cropProgress.setVisible(true);
            try {
                BufferedImage image = ImageIO.read(movie.getImgDir().resolve("img0001.png").toFile());
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
                render();

                new CropImages(point1, point2, movie.getNumImages(),
                        movie.getImgDir(), cropProgress).run();
                render();

                setButtonStates(ProgramState.POST_CROP);

                history = new Stack<>();

            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.out.println("Need to have 2 squares to crop the image.");
            }

            frame.currentFrame = 1;
            try {
                frame.setImage(movie.getPathToFrame(frame.currentFrame));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Could not find image.");
                e.printStackTrace();
                exit(1);
            }
            displayFrameNum.setText("Frame " + frame.currentFrame + " of " + movie.getNumImages());
            render();

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
        public void actionPerformed(ActionEvent event) {
            frame.currentFrame = 0;
            try {
                frame.setImage(movie.getPathToFrame(frame.currentFrame + 1));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Could not find image.");
                e.printStackTrace();
                exit(1);
            }
            displayFrameNum.setText("Frame " + (frame.currentFrame + 1) + " of " + movie.getNumImages());
            frame.maxSquares = 5;
            setButtonStates(ProgramState.SELECTING_LARVAE);
            render();
        }
    }

    /**
     * Searches through all the squares in the Image Component and adds their locations as new Larvae to larvae array
     * Removes all the squares from the Image Component and prevent more from being made
     * Disable "Start Larvae Selection" and "End Larvae Selection" buttons
     */
    private class StopLarvaeAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (frame.squares.size() < 1) return;
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
            try {
                movie.initializeColorCorrectedFrames();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "Could not find image.");
                ioe.printStackTrace();
                exit(1);
            }
            frame.setBlackAndWhiteImage(movie.findLarvaeLocation(frame.currentFrame));

            frame.vidInitialized = true;
            buttonPanel.requestFocus();
            render();
        }
    }

    private class RetrackPositionAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            String[] larvaeNumber = new String[movie.getLarva().size()];
            for (int i = 0; i < movie.getLarva().size(); i++) {
                larvaeNumber[i] = "" + (i + 1);
            }

            GUI.this.setTempLarvaIndex(-1);
            JComboBox larvaNumberOption = new JComboBox(larvaeNumber);
            Object[] message = {
                    "Please select larva number to retrack position.",
                    larvaNumberOption,
                    "Select ok and then select new point."
            };

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Select Larva", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return;
            }

            GUI.this.setTempLarvaIndex(larvaNumberOption.getSelectedIndex());
            frame.maxSquares = 1;
            setButtonStates(ProgramState.RETRACKING);
        }
    }

    private class StopRetrackAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (frame.squares.size() < frame.maxSquares) return;
            try {
                BufferedImage image = ImageIO.read(movie.getImgDir().resolve("img0001.png").toFile());
                double xratio = image.getWidth(null) / (double) frame.getImage().getWidth(null);
                double yratio = image.getHeight(null) / (double) frame.getImage().getHeight(null);

                Double[] pt = new Double[2];
                if (frame.squares.size() > 0) {
                    pt[0] = (frame.squares.get(0).getCenterX() * xratio);
                    pt[1] = (frame.squares.get(0).getCenterY() * yratio);

                    movie.retrackLarvaPosition(frame.currentFrame, GUI.this.getTempLarvaIndex(), pt);
                    frame.remove(frame.squares.get(0));
                    frame.maxSquares = 0;
                    movie.retrackLarvaPosition(frame.currentFrame, GUI.this.getTempLarvaIndex(), pt);
                }

                setButtonStates(ProgramState.TRACKING);
                history = new Stack<>();

                buttonPanel.requestFocus();
                render();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private class StopTrackingAction implements ActionListener {
        public StopTrackingAction() {}
        public void actionPerformed(ActionEvent event) {
            String[] larvaeNumber = new String[movie.getLarva().size()];
            for (int i = 0; i < movie.getLarva().size(); i++) {
                larvaeNumber[i] = "" + (i + 1);
            }

            JComboBox larvaNumberOption = new JComboBox(larvaeNumber);
            Object[] message = {
                    "Please select larva number to stop tracking.",
                    "This will remove current position",
                    "and future positions.",
                    larvaNumberOption,
            };

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Select Larva", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return;
            }

            if (frame.currentFrame == 0) {
                movie.getLarvae().remove(larvaNumberOption.getSelectedIndex());
                if (movie.getLarvae().size() == 0) {
                    setButtonStates(ProgramState.POST_CROP);
                }
            } else {
                movie.stopTracking(larvaNumberOption.getSelectedIndex(), frame.currentFrame + 1);
            }
            render();
        }
    }

    private class UndoAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            history.pop();
            frame.squares.remove(frame.squares.size() - 1);
            render();
            if (history.isEmpty()) {
                undo.setEnabled(false);
            }
        }
    }

    private class SliderAction implements ChangeListener {
        public SliderAction() {
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!sliderUseable) {
                return;
            }

            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                int value = source.getValue();
                sliderValue.setText("Darkness Threshold = " + value + ".");
                movie.setDarknessThreshold(value);
                frame.setBlackAndWhiteImage(movie.findLarvaeLocation(frame.currentFrame));
                repaint();
            }
        }


    }

    private class CSVExportAction implements ActionListener {
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
            CSVExport exporter = new CSVExport(movie, frames, zoneRadius);
            try {
                exporter.export(file);
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "Could not Export CSV");
            }
        }
    }

    private class ScreenshotAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            FileDialog fd = new FileDialog(GUI.this, "Select where to save image", FileDialog.SAVE);
            String defaultName = movie.getOriginalMovieName() + ".frame_" + (frame.currentFrame + 1) + ".png";
            fd.setFile(defaultName);
            fd.setVisible(true);
            BufferedImage bi = new BufferedImage(frame.getImage().getWidth(null),
                    frame.getImage().getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
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
        public void actionPerformed(ActionEvent event) {
            frame.displayPaths = !frame.displayPaths;
            pathCheckboxes.setVisible(frame.displayPaths);
            render();
        }
    }

    private class TogglePathAction implements ActionListener {
        private int index;

        public TogglePathAction(int index) {
            this.index = index;
        }

        public void actionPerformed(ActionEvent event) {
            frame.pathToggled[index] = !frame.pathToggled[index];
            render();
        }
    }

    private class ToggleZoneAction implements ActionListener {
        private int index;

        public ToggleZoneAction(int index) {
            this.index = index;
        }

        public void actionPerformed(ActionEvent event) {
            frame.zoneToggled[index] = !frame.zoneToggled[index];
            render();
        }
    }

    private class SwapAction implements ActionListener {
        public SwapAction() {
        }

        public void actionPerformed(ActionEvent event) {
            if (swapImage.getText().equals("Show detected larvae")) {
                frame.displayLarvaLocationOverlay = true;
                swapImage.setText("Hide detected larvae");
            } else {
                frame.displayLarvaLocationOverlay = false;
                swapImage.setText("Show detected larvae");
            }
            repaint();
        }
    }

    private class ShowZoneAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            frame.displayZones = !frame.displayZones;

            setZoneRadius.setVisible(frame.displayZones);
            displayZoneRadius.setVisible(frame.displayZones);
            zoneCheckboxes.setVisible(frame.displayZones);

            render();
        }
    }

    private class SetZoneRadiusAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JTextField radius = new JTextField(zoneRadius + "");
            Object[] message = {"Enter a zone radius in millimeters.", radius};

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Zone Radius", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return;
            }

            try {
                double newRadius = Double.parseDouble(radius.getText());
                if (newRadius > 0) {
                    zoneRadius = newRadius;
                }
            } catch (NumberFormatException e) {
                // Do nothing if the number is nonsensical.
            }

            displayZoneRadius.setText("Zone radius: " + zoneRadius + " mm");
            render();
        }
    }

    /**
     * A component that displays a tiled image and allows for movable squares to be painted on it
     */
    class ImageComponent extends JComponent {

        private static final int DEFAULT_WIDTH = 1000;
        private static final int DEFAULT_HEIGHT = 800;
        private final Color[] LARVAE_COLORS = {Color.cyan, Color.blue, Color.magenta, Color.green, Color.red};
        private static final int SIDELENGTH = 7;
        private double xRatio;
        private double yRatio;
        public int maxSquares;
        public ArrayList<Rectangle2D> squares;
        public int currentFrame;
        public boolean displayPaths;
        public boolean displayZones;
        public boolean displayLarvaLocationOverlay;
        public boolean[] pathToggled = new boolean[MAX_LARVAE];
        public boolean[] zoneToggled = new boolean[MAX_LARVAE];
        public boolean vidInitialized;
        public Video movie;

        private Rectangle2D currentMouseLocationRectangle;
        private Image image;
        private Image blackAndWhiteImage;

        public ImageComponent(String fileName) {
            maxSquares = 0;
            displayPaths = false;
            displayZones = false;
            displayLarvaLocationOverlay = false;

            image = new ImageIcon(getClass().getResource(fileName)).getImage();
            squares = new ArrayList<>();
            currentMouseLocationRectangle = null;
            addMouseListener(new MouseHandler());
            addMouseMotionListener(new MouseMotionHandler());
        }

        public void setImage(Path file) throws IOException {
            Image image = ImageIO.read(file.toFile());
            this.image = PreProcessor.scale(image, this.getWidth(), this.getHeight());
        }

        public void setBlackAndWhiteImage(Image image) {
            this.blackAndWhiteImage = PreProcessor.scale(image, this.getWidth(), this.getHeight());
        }

        public Image getImage() {
            return image;
        }

        public void paintComponent(Graphics g) {
            Image image = this.image;
            if (image == null) return;
            // draw the image in the upper-left corner

            g.drawImage(image, 0, 0, null);
            // tile the image across the component
            Graphics2D g2 = (Graphics2D) g;
            if (displayLarvaLocationOverlay) {
                Image overlayImage = blackAndWhiteImage;
                AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
                ((Graphics2D) g).setComposite(ac);
                g.drawImage(overlayImage, 0, 0, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            }
            g2.setColor(Color.red);

            // draw all squares
            for (Rectangle2D r : squares) {
                g2.draw(r);
            }
            ArrayList<Larva> larvae = new ArrayList<>();
            if (movie != null) {
                larvae = movie.getLarva();
                xRatio = movie.getDimensions()[0] / (double) image.getWidth(null);
                yRatio = movie.getDimensions()[1] / (double) image.getHeight(null);
            }
            //draw lines between larvae positions
            if (displayPaths) {
                drawPaths(g2, larvae);
            }

            if (displayZones) {
                drawZones(g2, larvae);

            }
        }

        private void drawPaths(Graphics2D g2, ArrayList<Larva> larvae) {
            for (int i = 0; i < larvae.size(); i++) {
                Larva l = larvae.get(i);
                g2.setColor(LARVAE_COLORS[i]);
                if (pathToggled[i]) {
                    for (int j = 0; j < currentFrame; j++) {
                        if (l.getPosition(j) != null) {
                            paintPaths(g2, l, j);
                        }
                    }
                    g2.fill(new Ellipse2D.Double(l.getPosition(0)[0] / xRatio,
                            l.getPosition(0)[1] / yRatio, 6, 6));
                    }
                Double[] lastKnownPosition = l.getLastTrackedPosition(currentFrame);
                if (lastKnownPosition != null) {
                    g2.setFont(new Font("Times New Roman", Font.BOLD, 30));
                    g2.drawString(String.valueOf(larvae.indexOf(l) + 1),
                            (int) ((lastKnownPosition[0]) / xRatio - 3),
                            (int) ((lastKnownPosition[1]) / yRatio - 3));
                }
            }
        }

        private void paintPaths(Graphics2D g2, Larva l, int startFrame) {
            for (int currentFrame = startFrame + 1; currentFrame < l.getPositionsSize(); currentFrame++) {
                if (l.getPosition(currentFrame) != null) {
                    if (currentFrame == startFrame + 1) {
                        g2.setStroke(new BasicStroke(1));
                    } else {
                        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_BEVEL, 0,
                                new float[]{9}, 0));
                    }

                    g2.draw(new Line2D.Double((l.getPosition(startFrame)[0]) / xRatio + 3,
                            (l.getPosition(startFrame)[1]) / yRatio + 3,
                            (l.getPosition(currentFrame)[0]) / xRatio + 3,
                            (l.getPosition(currentFrame)[1]) / yRatio + 3));
                    g2.setStroke(new BasicStroke());

                    int radius = 6;
                    g2.draw(new Ellipse2D.Double((l.getPosition(startFrame)[0]) / xRatio,
                            (l.getPosition(startFrame)[1]) / yRatio, radius, radius));
                    g2.draw(new Ellipse2D.Double((l.getPosition(currentFrame)[0]) / xRatio,
                            (l.getPosition(currentFrame)[1]) / yRatio, radius, radius));
                    return;
                }
            }
        }

        private void drawZones(Graphics2D g2, ArrayList<Larva> larvae) {
            int zones = (int) Math.ceil(GRID_DIMENSIONS * Math.sqrt(2) / zoneRadius);
            assert movie != null;

            for (Larva l : larvae) {
                double centerX = l.getPosition(0)[0] / xRatio;
                double centerY = l.getPosition(0)[1] / yRatio;
                if (zoneToggled[larvae.indexOf(l)]) {
                    g2.setColor(LARVAE_COLORS[larvae.indexOf(l)]);
                    for (int i = 1; i <= zones; i++) {
                        double radius = zoneRadius * i;
                        double xRadius = radius * image.getWidth(null) / GRID_DIMENSIONS;
                        double yRadius = radius * image.getHeight(null) / GRID_DIMENSIONS;
                        g2.setFont(new Font("Times New Roman", Font.BOLD, 16));
                        g2.drawString(String.valueOf(i), (int) centerX, (int) (centerY - yRadius + 15));
                        g2.draw(new Ellipse2D.Double(centerX - xRadius, centerY - yRadius,
                                xRadius * 2, yRadius * 2));
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
            render();
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
            render();
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

                    currentMouseLocationRectangle.setFrame(x - SIDELENGTH / 2.0, y - SIDELENGTH / 2.0, SIDELENGTH, SIDELENGTH);
                    render();
                }
            }
        }
    }
}

