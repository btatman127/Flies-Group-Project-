import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class Video {
    private static final int FPS = 1;
    private static final double MAX_LARVA_SPEED = 5.0;
    public static final int MIN_ISLAND_SIZE = 2;

    private final File originalVideo;
    private final Path shortenedVideo;
    private final Path imgDir;
    private final int numImages;
    private final ArrayList<Larva> larvae;

    private Region[][][] regions; //Region[frame number][x-coordinate][y-coordinate]
    private boolean[][][] larvaLoc;
    private int darknessThreshold;
    private int regionDim;
    private ArrayList<Double[]> islands;


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

    public void initializeColorCorrectedFrames() throws IOException {
        PreProcessor.colorCorrectFrames(numImages, imgDir);
        BufferedImage image = ImageIO.read(imgDir.resolve(String.format("img%04d.png", 1)).toFile());

        regionDim = image.getHeight() / 100;
        regions = new Region[numImages][image.getWidth() / regionDim][image.getHeight() / regionDim];
        larvaLoc = new boolean[numImages][image.getWidth() / regionDim][image.getHeight() / regionDim];
        islands = new ArrayList<>();

        islands = getIslandList(0);
        createRegions(0, ImageIO.read(imgDir.resolve("cc0001.png").toFile()));
    }

    /**
     * Last step to initialize a movie.
     * Stores coordinates of each larva in each frame
     */
    public void createFrame(int currentFrame) throws IOException {
        BufferedImage image = ImageIO.read(imgDir.resolve(String.format("cc%04d.png", currentFrame + 1)).toFile());
        createRegions(currentFrame, image);
        findLarvaeLocation(currentFrame);

        islands = getIslandList(currentFrame);
        trackLarvae(currentFrame);
    }


    public void retrackLarvaPosition(int currentFrame, int larvaIndex, Double[] pt) {
        Larva l = larvae.get(larvaIndex);

        l.trimPositions(currentFrame);

        l.setNewPosition(pt);
    }

    public void deleteFrame(int currentFrame) {
        for (Larva l : larvae) {
            l.trimPositions(currentFrame);
        }
    }

    /**
     * Splits an image into a set number of subimages called regions, which are stored in the Video's regions array.
     *
     * @param frame The frame number of the image.
     * @param image An image to create regions on.
     */
    private void createRegions(int frame, BufferedImage image) {
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
        BufferedImage image = new BufferedImage(regions[0].length, regions[0][0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {
                int avg = getSample(frame, i, j);
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
     * Searches a frame for islands.
     * An island is a probable larva location
     *
     * @param frame The frame to search
     * @return An arraylist of Double array of length 3, where index 0 is the x-coordinate of the center,
     * index 1 is the y-coordinate of the center,
     * and index 2 is the mass of the island, ie, how many points made up the island.
     */
    private ArrayList<Double[]> getIslandList(int frame) {
        //depth first search
        boolean[][] visited = new boolean[larvaLoc[0].length][larvaLoc[0][0].length];
        ArrayList<Double[]> coords = new ArrayList<>();
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {

                if (larvaLoc[frame][i][j] && !visited[i][j]) {
                    visited[i][j] = true;
                    Double[] island = getIsland(frame, i, j, visited);
                    if (island[2] > MIN_ISLAND_SIZE) {
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
        yy = 0;
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
        return x >= 0 && x < larvaLoc[0].length && y >= 0 && y < larvaLoc[0][0].length;
    }


    /**
     * Locate the larva location closest to
     * the last know larva location, using the user clicked points as a baseline.
     */
    private void trackLarvae(int currentFrame) {
        if (currentFrame == 0) return;

        //Double[] in hashmap represents {distance to larvae, island-x, island-y}
        HashMap<Larva, Double[]> closestIsland = new HashMap<>();

        //Finds the larva closest to each island
        for (Double[] island : islands) {
            double minDistance = Integer.MAX_VALUE;
            Larva closestLarva = null;

            for (Larva l : larvae) {
                Double[] old = l.getPosition(currentFrame - 1);
                if (old == null) {
                    continue;
                }

                double distance = distance(old, island);
                if (distance < minDistance && distance < getDimensions()[0] / MAX_LARVA_SPEED) {
                    minDistance = distance;
                    closestLarva = l;
                }
            }
            if(closestLarva != null){
                //Check if another island has the same larva as its closest. Keep the closer island.
                if(closestIsland.containsKey(closestLarva)){
                    if(closestIsland.get(closestLarva)[0] > minDistance) {
                        closestIsland.put(closestLarva, new Double[]{minDistance, island[0], island[1]});
                    }
                }
                else{
                    closestIsland.put(closestLarva, new Double[]{minDistance, island[0], island[1]});
                }
            }
        }

        for(Larva l : larvae){
            if(closestIsland.containsKey(l)) {
                Double[] position = closestIsland.get(l);
                l.setNewPosition(new Double[]{position[1], position[2]});
            }
            else{
                l.setNewPosition(null);
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
