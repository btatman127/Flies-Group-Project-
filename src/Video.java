import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.File;

public class Video {
    private static final int FPS = 1;
    private static final int MIN_FRAMES_TO_TRAVEL_ACROSS_SCREEN = 6;
    public static final int MIN_ISLAND_SIZE = 2;

    private final File originalVideo;
    private final Path shortenedVideo;
    private final Path imgDir;
    private final int numImages;
    private final ArrayList<Larva> larvae;

    private Region[][][] regions; //Region[frame number][x-coordinate][y-coordinate]
    private boolean[][][] larvaLoc;
    private int[][][] avgDarkness;
    private int darknessThreshold;
    private int regionDim;
    private int numRegionsX;
    private int numRegionsY;

    public Video(File movie, int startTime, int endTime, int darknessThreshold) throws
            IOException, InterruptedException {
        originalVideo = movie;
        larvae = new ArrayList<>();
        this.darknessThreshold = darknessThreshold;

        // Create input and output paths for ffmpeg to use
        this.imgDir = Files.createTempDirectory("fly_tracker");
        imgDir.toFile().deleteOnExit();

        shortenedVideo = imgDir.resolve("video.mov");
        String outputPath = imgDir.resolve("img%04d.png").toString();

        // Extract images with ffmpeg
        PreProcessor.cropVideo(startTime, endTime, originalVideo.getAbsolutePath(), getShortenedVideo().toString());
        PreProcessor.extractFrames(getShortenedVideo().toString(), outputPath, FPS);
        numImages = Objects.requireNonNull(imgDir.toFile().listFiles()).length - 1;
    }

    public void setDarknessThreshold(int darknessThreshold) {
        this.darknessThreshold = darknessThreshold;
    }

    public ArrayList<Larva> getLarvae() { return this.larvae; }

    public void initializeColorCorrectedFrames() throws IOException {
        PreProcessor.colorCorrectFrames(numImages, imgDir);
        BufferedImage image = ImageIO.read(imgDir.resolve(String.format("img%04d.png", 1)).toFile());

        regionDim = image.getHeight() / 100;
        numRegionsX = image.getWidth() / regionDim;
        numRegionsY = image.getHeight()/regionDim;
        regions = new Region[numImages][numRegionsX][numRegionsY];
        larvaLoc = new boolean[numImages][numRegionsX][numRegionsY];

        for (int i = 0; i < numImages; i++) {
            createRegions(i, ImageIO.read(imgDir.resolve(String.format("cc%04d.png", i+1)).toFile()));
        }
        avgDarkness = getAvgDarkness(numImages);
    }

    /**
     * Last step to initialize a movie.
     * Stores coordinates of each larva in each frame
     */
    public void createFrame(int currentFrame) throws IOException {
        findLarvaeLocation(currentFrame);
        trackLarvae(currentFrame);
    }

    public void retrackLarvaPosition(int currentFrame, int larvaIndex, Double[] pt) {
        Larva l = larvae.get(larvaIndex);

        l.trimPositions(currentFrame);

        l.setNewPosition(pt);
    }

    public void stopTracking(int larvaIndex, int frame) {
        Larva l = larvae.get(larvaIndex);
        l.trimPositions(frame - 1);
        l.setNewPosition(null);
    }

    public void deleteFrame(int currentFrame) {
        for (Larva l : larvae) {
            l.trimPositions(currentFrame);
        }
    }

    /**
     * Splits an image into a set number of subimages called regions, which are stored in the Video's regions array.
     *
     * @param frame The Fframe number of the image.
     * @param image An image to create regions on.
     */
    private void createRegions(int frame, BufferedImage image) {
        for (int i = 0; i < numRegionsX; i++) {
            for (int j = 0; j < numRegionsY; j++) {
                Region region = new Region(image.getSubimage(i * regionDim, j * regionDim, regionDim, regionDim));
                regions[frame][i][j] = region;
            }
        }
    }

    private int[][][] getAvgDarkness(int numFrames){
        int[][][] avgDarkness = new int[numFrames][numRegionsX][numRegionsY];
        for(int i = 0; i < numFrames; i++){
            for(int j = 0; j < numRegionsX; j++){
                for (int k = 0; k < numRegionsY; k++) {
                    avgDarkness[i][j][k] = getSample(i, j, k);
                }
            }
        }
        return avgDarkness;
    }

    private int getSample(int frame, int x, int y) {
        int average = 0;
        int count = 0;
        int kernelSize = 3;
        for (int i = x - (kernelSize / 2); i <= x + (kernelSize / 2); i++) {
            if (i >= 0 && i < numRegionsX) {
                for (int j = y - (kernelSize / 2); j <= y + (kernelSize / 2); j++) {
                    if (j >= 0 && j < numRegionsY) {
                        count++;
                        average += regions[frame][i][j].getAvgValue();
                    }
                }
            }
        }
        return average / count;
    }

    /**
     * For a specific frame, find the probable locations of larvae as represented by a black and white image.
     *
     * @param frame The frame to search
     * @return A low-resolution representation of a frame, in pure black and white, where black areas are likely to be larvae
     * and white areas are not.
     */
    public BufferedImage findLarvaeLocation(int frame) {
        if(regions == null) return null;
        BufferedImage image = new BufferedImage(numRegionsX, numRegionsY, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < numRegionsX; i++) {
            for (int j = 0; j < numRegionsY; j++) {
                int avg = avgDarkness[frame][i][j];
                larvaLoc[frame][i][j] = (avg < darknessThreshold);
                int b = 255;
                if (larvaLoc[frame][i][j]) {
                    b = 0;
                }
                image.setRGB(i, j, new Color(b, b, b).getRGB());
            }
        }
        return image;
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
    private Double[] getIsland(int frame, int x, int y, boolean[][] visited) {
        Double[] island = new Double[3]; // island is defined as {x, y, mass}
        ArrayList<double[]> points = islandDFS(frame, x, y, visited, new ArrayList<>());
        double mass = points.size();
        double xc = 0;
        double yc = 0;
        for (double[] point : points) {
            xc += point[0];
            yc += point[1];
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
        if (isCoordValid(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy]) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        //E
        xx = x + 1;
        yy = y;
        if (isCoordValid(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy]) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        //S
        xx = x;
        yy = y - 1;
        if (isCoordValid(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy]) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }

        //W
        xx = x - 1;
        yy = y;
        if (isCoordValid(xx, yy)) {
            if (!(visited[xx][yy]) && larvaLoc[frame][xx][yy]) {
                visited[xx][yy] = true;
                points = islandDFS(frame, xx, yy, visited, points);
            }
        }
        return points;
    }

    /**
     * @return true only if (x,y) is a valid index into the Video's representation of a frame (regions and larvaLoc)
     */
    private boolean isCoordValid(int x, int y) {
        return x >= 0 && x < numRegionsX && y >= 0 && y < numRegionsY;
    }


    /**
     * Locate the larva location closest to
     * the last know larva location, using the user clicked points as a baseline.
     */
    private void trackLarvae(int currentFrame) {
        if (currentFrame == 0) return;
        for(Larva l : larvae){
            Double[] old = l.getPosition(currentFrame - 1);
            if (old == null) {
                l.setNewPosition(null);
                continue;
            }

            int r = (int) (old[0] / regionDim);
            int c = (int) (old[1] / regionDim);

            Double[] newPosition = findClosestIsland(currentFrame, r, c);
            l.setNewPosition(newPosition);
        }
    }

    private Double[] findClosestIsland(int currentFrame, int r, int c) {
        PriorityQueue<double[]> locations = new PriorityQueue<>(Comparator.comparingDouble(a -> a[2]));
        boolean[][] visited = new boolean[numRegionsX][numRegionsY];
        locations.add(new double[]{r, c, 0});
        visited[r][c] = true;

        double[] originalLoc = {r, c};
        double maxDistance = (double) numRegionsX / MIN_FRAMES_TO_TRAVEL_ACROSS_SCREEN;

        int[] dRow = { -1, 0, 1, 0 };
        int[] dCol = { 0, 1, 0, -1 };

        while(!locations.isEmpty()){
            double[] oldLoc = locations.poll();
            if(oldLoc[2] > maxDistance){
                return null;
            }

            for(int i = 0; i < 4; i++){
                int newRow = (int) (oldLoc[0]) + dRow[i];
                int newCol = (int) (oldLoc[1]) + dCol[i];

                if(isCoordValid(newRow, newCol) && !visited[newRow][newCol]){
                    double dis = distance(originalLoc, new double[] {newRow, newCol});
                    locations.add(new double[]{newRow, newCol, dis});
                    visited[newRow][newCol] = true;

                    if(larvaLoc[currentFrame][newRow][newCol]){
                        Double[] island = getIsland(currentFrame, newRow, newCol, new boolean[numRegionsX][numRegionsY]);
                        if(island[2] > MIN_ISLAND_SIZE){
                            return island;
                        }

                    }
                }
            }

        }
        return null;
    }

    /**
     * @return The euclidean distance between points a and b.
     */
    private double distance(double[] a, double[] b) {
        return Math.sqrt(Math.pow((a[0] - b[0]), 2) + Math.pow((a[1] - b[1]), 2));
    }


    public ArrayList<Larva> getLarva() {
        return larvae;
    }

    public void addLarva(Larva l) {
        larvae.add(l);
    }

    public Path getPathToFrame(int index) {
        return imgDir.resolve(String.format("img%04d.png", index));
    }

    public Path getImgDir() {
        return imgDir;
    }

    public String getOriginalMovieName() {
        String name = originalVideo.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            lastDot = name.length();
        }
        return name.substring(0, lastDot);
    }

    public Path getShortenedVideo() {
        return shortenedVideo;
    }

    public int getNumImages() {
        return numImages;
    }

    /**
     * @return An array of length 2, where index 0 is the width of the video and index 1 is the height of the video.
     * This is calculated when the method is called; this is not the original width and height of the video!
     */
    public double[] getDimensions() {
        try {
            BufferedImage im = ImageIO.read(imgDir.resolve(String.format("img%04d.png", 1)).toFile());
            return new double[]{im.getWidth(), im.getHeight()};
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
