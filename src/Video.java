import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
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
    private int threshold;// = 255 - (int) (255 * .2);
    private int regionDim;// = 8;
    //Array of islands for each frame
    //Array of (arraylists of (double arrays))
    private ArrayList<ArrayList<Double[]>> islands;
    private ArrayList<Integer> collisionFrameIndex;



    // length and/or width of each grid square in mm
    private ArrayList<Larva> larvae;

    //factor that converts pixels to mm
    private double scaleFactor;

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

        String[] command = new String[]{"mkdir", imgDir};
        java.lang.Process p = rt.exec(command);
        p.waitFor();

        String inputPath = this.movieDir + "/" + this.movieName;
        String outputPath = System.getProperty("user.dir") + "/" + imgDir + "/img%04d.png";
        int fps = 1;

		System.out.println("before preprocessor");
        //call ffmpeg extractor
		int duration = endTime - startTime;
        PreProcessor.extractFrames(inputPath, outputPath, fps);
		// ExtractFrames ef = new ExtractFrames();
		// ef.extract(inputPath, outputPath, fps, duration, imgDir);
		// System.out.println("after preprocessor");
        numImages = new File(System.getProperty("user.dir") + "/" + imgDir).listFiles().length;


        threshold = 255 - (int) (255 * .2);

    }

    /** Last step to initialize a movie.
     *  Stores coordinates of each larva in each frame
     *
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
                //ImageIO.write(averages, "png", new File("assets/avg" + String.format("%04d", f+1) + ".png"));
                //ImageIO.write(locations, "png", new File("assets/bool" + String.format("%04d", f+1) + ".png"));


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


    void resetLarvaPosition(int firstFrame, int larvaIndex, Double pt[]){
        //starting and frameIndex
        //overwrite position values for larvae[larvaIndex] for each frame
        if(!videoInitialized){
            System.out.println("!!attempted to resetLarvaPosition before fully initializing video!!");
            return;
        }

        Larva l = larvae.get(larvaIndex);

        l.trimPositions(firstFrame); // this will destroy the record of coordinates including and after the firstFrame

        l.setNewPosition(pt);


        for(int f = firstFrame + 1; f < numImages; f++){
            ArrayList<Double[]> currentIslands = islands.get(f);

            Double[] previousPt = l.getPosition(f-1);

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

            if (minDistance < getDimensions()[1] / 24.0) { //TODO: this 24.0 value should be a class constant
                l.setNewPosition(islands.get(f).get(minIndex));
            } else {
                break;
            }
        }
    }


    public ArrayList<Double[]> getLarvaCoordinates(int frame) {
        return islands.get(frame);
    }

    /** goes through larvae positions and checks for position overlap on same frames **/
    private boolean findCollisions() {
        boolean collisionFound = false;
        for (int f = 0; f < numImages; f++) {
            //for each larva position
            for(int i = 0; i < larvae.size()-1;  i++){
                for( int j = i+1; j < larvae.size(); j++) {
                    //System.out.printf("L1: %lf %lf L2: %lf %lf\n", 1,2,3,4 );
                    //System.out.printf("L1: %lf %lf L2: %lf %lf\n", larvae.get(i).getPosition(f)[0], larvae.get(j).getPosition(f)[0], larvae.get(i).getPosition(f)[1], larvae.get(j).getPosition(f)[1] );
                    //System.out.println(larvae.get(i).getPosition(f)[0]+ " "+ larvae.get(j).getPosition(f)[0]+ " " + larvae.get(i).getPosition(f)[1]+ " " + larvae.get(j).getPosition(f)[1] );

                    if(larvae.get(i).getPositionsSize() > f && larvae.get(j).getPositionsSize() > f) {
                        if (larvae.get(i).getPosition(f)[0] == larvae.get(j).getPosition(f)[0] && larvae.get(i).getPosition(f)[1] == larvae.get(j).getPosition(f)[1]) {
                            collisionFrameIndex.add(f);
                            collisionFound = true;
                            System.out.println("Collision @: " + collisionFrameIndex.get(collisionFrameIndex.size() - 1));

                        }
                    }
                }
            }
                // is there a duplicate?
                    //if so push frame number to collision frame index
            //larva[f][][]
        }
        return collisionFound;
    }

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
                        //System.out.println("frame " + frame + " i,j " + i +"," +j);
                    }
                }
            }
        }
        return average /= count;
    }

    private BufferedImage fillLarvaLoc(int frame) {
        BufferedImage array = new BufferedImage(regions[0].length, regions[0][0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {
                int avg = getSample(frame, i, j);
                larvaLoc[frame][i][j] = avg < threshold;
                //System.out.println((avg < threshold) + "  avg < thresh " + avg + " " + threshold );
                int b = 255;
                if (larvaLoc[frame][i][j]) {
                    b = 0;
                }
                array.setRGB(i, j, new Color(b, b, b).getRGB());
            }
        }
        return array;
    }

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

    private ArrayList<double[]> islandDFS(int frame, int x, int y, boolean[][] visited, ArrayList<double[]> points) {
        double[] here = {x, y};
        points.add(here);
        //ArrayList<double[]> directions = new ArrayList<double[]>();
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

    private boolean validCoords(int x, int y) {
        return x >= 0 && x < larvaLoc[0].length && y >= 0 && y < larvaLoc[0][0].length;
    }


    private void trackLarvae() {
        //TODO Replace initial larva position (currently user-clicked) with true larva center locations

        for (Larva l : larvae) {
            for (int i = 1; i < numImages; i++) {
                Double[] old = l.getPosition(i - 1);

                double minDistance = 100000;
                int minIndex = -1;


                for (int j = 0; j < islands.get(i).size(); j++) {
                    double distance = distance(old, islands.get(i).get(j));


                    if (distance < minDistance) {
                        minDistance = distance;
                        minIndex = j;
                    }
                }

                if (minDistance < getDimensions()[1] / 24.0) {
                    l.setNewPosition(islands.get(i).get(minIndex));
                } else {
                    break;
                }
            }
        }
    }

    private double distance(Double[] a, Double[] b) {
        return Math.sqrt(Math.pow((a[0] - b[0]), 2) + Math.pow((a[1] - b[1]), 2));
    }



    public ArrayList<Larva> getLarva() {
        return larvae;
    }

    public void addLarva(Larva l) {
        Double[] a = l.getPosition(0);
        larvae.add(l);
    }

    public String getPathToFrame(int index) {
        String path;

        path = imgDir + "/img" + String.format("%04d", index) + ".png";

        return path;
    }

    public String getImgDir() {
        return imgDir;
    }

    public String getMovieDir() {
        return movieDir;
    }

    public String getMovieName() {
        return movieName;
    }

    public int getNumImages() {
        return numImages;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }


    public boolean isVideoInitialized() {
        return videoInitialized;
    }

    public double[] getDimensions() {
        try {
            BufferedImage im = ImageIO.read(new File(imgDir + "/img" + String.format("%04d", 1) + ".png"));
            return new double[]{im.getWidth(), im.getHeight()};
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public int getCollisionFrameIndex(int index){
        return collisionFrameIndex.get(index);
    }

}
