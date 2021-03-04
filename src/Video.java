import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Video {
    private String movieDir;
    private String movieName;
    private String movieNameLong;
    private String imgDir;
    private int numImages;
    private String outputPathLong;
    private boolean videoInitialized;


    //TRACKER
    private Region[][][] regions; //Region[frame number][x-coordinate][y-coordinate]
    private boolean[][][] larvaLoc;
    private static final int THRESHOLD = 204;
    private int regionDim;// = 8;
    //Array of islands for each frame
    //Array of (arraylists of (double arrays))
    private ArrayList<ArrayList<Double[]>> islands;
    private ArrayList<Integer> collisionFrameIndex;
    private double islandConstant = 24.0;


    // length and/or width of each grid square in mm
    private ArrayList<Larva> larvae;

    public String getOutputPathLong() {
        return outputPathLong;
    }

    /**
     * Constructor for a Video object
     *
     * @param movieDir      the movieDir file where the movie is located
     * @param movieNameLong the name of the movie
     */
    public Video(String movieDir, String movieNameLong, int startTime, int endTime) throws
            IOException, InterruptedException {
        videoInitialized = false;
        this.movieDir = movieDir;
        this.movieNameLong = movieNameLong;


        //create a list of larva for this video
        larvae = new ArrayList<Larva>();


        //create input and output paths for the whole video
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        this.movieName = this.movieNameLong.substring(0, this.movieNameLong.length() - 4) + "SHORTER" + timestamp + ".mov";

        outputPathLong = movieDir + "/" + this.movieName;
        String inputPathLong = movieDir + "/" + this.movieNameLong;

        //call ffmpeg crop method
        PreProcessor.cropVideo(startTime, endTime, inputPathLong, outputPathLong);

        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        Long l = new Long(System.currentTimeMillis() / 1000L);
        this.imgDir = "vidID" + l.toString();

        new File(imgDir).mkdir();

        String inputPath = this.movieDir + "/" + this.movieName;
        String outputPath = System.getProperty("user.dir") + "/" + imgDir + "/img%04d.png";
        int fps = 1;

        //call ffmpeg extractor
        int duration = endTime - startTime;
        PreProcessor.extractFrames(inputPath, outputPath, fps);
        numImages = new File(System.getProperty("user.dir") + "/" + imgDir).listFiles().length;
    }


    /**
     * Last step to initialize a movie.
     * Stores coordinates of each larva in each frame
     */
    public boolean createFrames() {
        boolean collisionFound = false;
        PreProcessor.colorCorrectFrames(numImages, imgDir);
        try {
            BufferedImage im = ImageIO.read(new File(imgDir + "/img" + String.format("%04d", 1) + ".png"));
            regionDim = im.getHeight() / 100; //should be a function of cc
            regions = new Region[numImages][im.getWidth() / regionDim][im.getHeight() / regionDim];
            larvaLoc = new boolean[numImages][im.getWidth() / regionDim][im.getHeight() / regionDim];
            islands = new ArrayList<ArrayList<Double[]>>(numImages);// islands[f][island][coord]

            for (int f = 0; f < numImages; f++) {
                BufferedImage image = ImageIO.read(new File(imgDir + "/cc" + String.format("%04d", f + 1) + ".png"));
                createRegions(f, image);
                fillLarvaLoc(f);

                //Array of islands for each frame
                //Array of (arraylists of (double arrays))
                islands.add(getIslandList(f));
            }

            collisionFrameIndex = new ArrayList<>();

            trackLarvae();

            collisionFound = findCollisions();

            videoInitialized = true;


        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return collisionFound;
    }


    void retrackLarvaPositiom(int firstFrame, int larvaIndex, Double pt[]) {
        //starting and frameIndex
        //overwrite position values for larvae[larvaIndex] for each frame
        if (!videoInitialized) {
            System.out.println("!!attempted to resetLarvaPosition before fully initializing video!!");
            return;
        }

        Larva l = larvae.get(larvaIndex);

        l.trimPositions(firstFrame); // this will destroy the record of coordinates including and after the firstFrame

        l.setNewPosition(pt);


        for (int f = firstFrame + 1; f < numImages; f++) {
            ArrayList<Double[]> currentIslands = islands.get(f);

            Double[] previousPt = l.getPosition(f - 1);

            if(previousPt == null){
                l.setNewPosition(null);
                continue;
            }
            double minDistance = 100000;
            int minIndex = -1;

            //for each island in currentIslands
            for (int j = 0; j < currentIslands.size(); j++) {
                double distance = distance(previousPt, islands.get(f).get(j));

                if (distance < minDistance) {
                    minDistance = distance;
                    minIndex = j;
                }
            }

            if (minDistance < getDimensions()[1] / islandConstant) {
                l.setNewPosition(islands.get(f).get(minIndex));
            } else {
                l.setNewPosition(null);
            }
        }
    }

    void resetSingleLarvaPosition(int firstFrame, int larvaIndex, Double pt[]) {
        if (!videoInitialized) {
            System.out.println("!!attempted to resetLarvaPosition before fully initializing video!!");
            return;
        }

        Larva l = larvae.get(larvaIndex);
        l.getCoordinates().get(firstFrame)[0] = pt[0];
        l.getCoordinates().get(firstFrame)[1] = pt[1];

    }

    /**
     * goes through larvae positions and checks for position overlap on same frames
     **/
    private boolean findCollisions() {
        boolean collisionFound = false;
        for (int f = 0; f < numImages; f++) {
            //for each larva position
            for (int i = 0; i < larvae.size() - 1; i++) {
                if (larvae.get(i).getPositionsSize() <= f || larvae.get(i).getPosition(f) == null) {
                    continue;
                }
                for (int j = i + 1; j < larvae.size(); j++) {
                    if (larvae.get(j).getPositionsSize() <= f || larvae.get(j).getPosition(f) == null) {
                        continue;
                    }
                    if (larvae.get(i).getPosition(f)[0] == larvae.get(j).getPosition(f)[0] && larvae.get(i).getPosition(f)[1] == larvae.get(j).getPosition(f)[1]) {
                        collisionFrameIndex.add(f);
                        collisionFound = true;
                    }
                }
            }
            // is there a duplicate?
            //if so push frame number to collision frame index
            //larva[f][][]
        }
        return collisionFound;
    }

    /**
     * Splits an image into a set number of subimages called regions, which are stored in the Video's regions array.
     *
     * @param frame The frame number of the image.
     * @param image An image to create regions on.
     */
    private void createRegions(int frame, BufferedImage image) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        for (int i = 0; i < regions[0].length; i++) {
            for (int j = 0; j < regions[0][0].length; j++) {
                Region region = new Region(image.getSubimage(i * regionDim, j * regionDim, regionDim, regionDim));
                regions[frame][i][j] = region;
            }
        }
    }


    private int getSample(int frame, int x, int y) {
        int average = 0;
        int count = 0;
        int kernelSize = 3;
        for (int i = x - (kernelSize / 2); i <= x + (kernelSize / 2); i++) {
            if (i >= 0 && i < regions[0].length) {
                for (int j = y - (kernelSize / 2); j <= y + (kernelSize / 2); j++) {
                    if (j >= 0 && j < regions[0][0].length) {
                        count++;
                        average += regions[frame][i][j].getAvgValue();
                    }
                }
            }
        }
        return average /= count;
    }

    /**
     * For a specific frame, find the probable locations of larvae as represented by a black and white image.
     *
     * @param frame The frame to search
     * @return A low-resolution representation of a frame, in pure black and white, where black areas are likely to be larvae
     * and white areas are not.
     */
    private BufferedImage fillLarvaLoc(int frame) {
        BufferedImage array = new BufferedImage(regions[0].length, regions[0][0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {
                int avg = getSample(frame, i, j);
                larvaLoc[frame][i][j] = avg < THRESHOLD;
                int b = 255;
                if (larvaLoc[frame][i][j]) {
                    b = 0;
                }
                array.setRGB(i, j, new Color(b, b, b).getRGB());
            }
        }
        return array;
    }

    /**
     * Searches a frame for islands.
     *
     * @param frame The frame to search
     * @return An arraylist of Double array of length 3, where index 0 is the x-coordinate of the center,
     * index 1 is the y-coordinate of the center,
     * and index 2 is the mass of the island, ie, how many points made up the island.
     */
    private ArrayList<Double[]> getIslandList(int frame) {
        //depth first search
        boolean visited[][] = new boolean[larvaLoc[0].length][larvaLoc[0][0].length];
        ArrayList<Double[]> coords = new ArrayList<Double[]>();
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {

                if (larvaLoc[frame][i][j] && !visited[i][j]) {
                    visited[i][j] = true;
                    Double[] island = getIsland(frame, i, j, visited);
                    if (island[2] > 2) {
                        coords.add(island);
                    }

                } else {
                    visited[i][j] = true;
                }

            }
        }

        return coords;
    }

    /**
     * For a specific point known to be on an island, finds the rest of the island, and calculates its properties.
     *
     * @param frame   The frame to search
     * @param x       The x-coordinate of a known island
     * @param y       The y-coordinate of a known island
     * @param visited The list of visited locations
     * @return An array of length 3 of Doubles where index 0 is the x-coordinate of the center, index 1 is the y-coordinate of the center,
     * and index 2 is the mass of the island, ie, how many points made up the island.
     */
    private Double[] getIsland(int frame, int x, int y, boolean[][] visited) { //cc as in contiguousCoords
        Double[] island = new Double[3]; // island is defined as {x, y, mass}
        ArrayList<double[]> points = islandDFS(frame, x, y, visited, new ArrayList<double[]>());
        double mass = points.size();
        double xc = 0;
        double yc = 0;
        for (int i = 0; i < mass; i++) {
            xc += points.get(i)[0];
            yc += points.get(i)[1];
        }
        island[0] = xc / mass * regionDim;
        island[1] = yc / mass * regionDim;
        island[2] = mass;
        return island;
    }

    /**
     * Performs depth-first search on an island to determine its size and center.
     *
     * @param frame   The frame to search
     * @param x       The x-coordinate of a known island
     * @param y       The y-coordinate of a known island
     * @param visited The list of coordinates that we have already checked
     * @param points  A list of coordinates known to be in the island
     * @return An arraylist of double[] containing all the coordinates of points in the island.
     */
    private ArrayList<double[]> islandDFS(int frame, int x, int y, boolean[][] visited, ArrayList<double[]> points) {
        double[] here = {x, y};
        points.add(here);
        visited[x][y] = true;
        int xx, yy;

        //N
        xx = x;
        yy = y + 1;
        if (validCoords(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }


        //E
        xx = x + 1;
        yy = y;
        if (validCoords(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        //S
        xx = x;
        yy = y - 1;
        if (validCoords(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        //W
        xx = x - 1;
        yy = 0;
        if (validCoords(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        return points;
    }

    /**
     * @return true only if (x,y) is a valid index into the Video's representation of a frame (regions and larvaLoc)
     */
    private boolean validCoords(int x, int y) {
        return x >= 0 && x < larvaLoc[0].length && y >= 0 && y < larvaLoc[0][0].length;
    }


    /**
     * Loops over the frames of the video, and for each frame, locates the larva location closest to
     * the last know larva location, using the first points clicked as a baseline.
     */
    private void trackLarvae() {
        //TODO Replace initial larva position (currently user-clicked) with true larva center locations

        for (Larva l : larvae) {
            for (int i = 1; i < numImages; i++) {
                Double[] old = l.getPosition(i - 1);
                if(old == null){
                    l.setNewPosition(null);
                    continue;
                }
                double minDistance = 100000;
                int minIndex = -1;

                for (int j = 0; j < islands.get(i).size(); j++) {
                    double distance = distance(old, islands.get(i).get(j));

                    if (distance < minDistance) {
                        minDistance = distance;
                        minIndex = j;
                    }
                }
                if (minDistance < getDimensions()[1] / islandConstant) {
                    l.setNewPosition(islands.get(i).get(minIndex));
                } else {
                    l.setNewPosition(null);
                }
            }
        }
    }

    /**
     * @return The euclidean distance between points a and b.
     */
    private double distance(Double[] a, Double[] b) {
        return Math.sqrt(Math.pow((a[0] - b[0]), 2) + Math.pow((a[1] - b[1]), 2));
    }


    public ArrayList<Larva> getLarva() {
        return larvae;
    }
    public void reInitializeLarvaArrayList() {larvae = new ArrayList<Larva>();}

    public void addLarva(Larva l) {
        Double[] a = l.getPosition(0);
        larvae.add(l);
    }

    public void removeLarvaFromArrayListByIndex(int larvaIndex) {
        larvae.remove(larvaIndex);
    }

    public String getPathToFrame(int index) {
        String path;

        path = imgDir + "/img" + String.format("%04d", index) + ".png";

        return path;
    }

    public String getImgDir() {
        return imgDir;
    }

    public String getOriginalMovieName() {
        int lastDot = movieNameLong.lastIndexOf('.');
        if (lastDot == -1) {
            lastDot = movieNameLong.length();
        }
        return movieNameLong.substring(0, lastDot);
    }

    public String getMovieName() {
        return movieName;
    }

    public int getNumImages() {
        return numImages;
    }

    public boolean isVideoInitialized() {
        return videoInitialized;
    }

    /**
     * @return An array of length 2, where index 0 is the width of the video and index 1 is the height of the video.
     * This is calculated when the method is called; this is not the original width and height of the video!
     */
    public double[] getDimensions() {
        try {
            BufferedImage im = ImageIO.read(new File(imgDir + "/img" + String.format("%04d", 1) + ".png"));
            return new double[]{im.getWidth(), im.getHeight()};
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public int getCollisionFrameIndex(int index) {
        return collisionFrameIndex.get(index);
    }

}
