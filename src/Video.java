import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;

public class Video {
    private String movieDir;
    private String movieName;
    private String imgDir;
    private int numImages;


    //TRACKER
    private Region[][][] regions; //Region[frame number][x-coordinate][y-coordinate]
    private boolean[][][] larvaLoc;
    private int threshold;// = 255 - (int) (255 * .2);
    private int regionDim;// = 8;



	// length and/or width of each grid square in mm
	private ArrayList<Larva> larvae;
	
    //factor that converts pixels to mm
    private double scaleFactor;

    /**
     * Constructor for a Video object
     * @param movieDir   the movieDir file where the movie is located
     * @param movieName  the name of the movie
     */
    public Video(String movieDir, String movieName) throws IOException, InterruptedException {
        this.movieDir = movieDir;
        this.movieName = movieName;
		
		//create a list of larva for this video
		larvae = new ArrayList<Larva>();
		
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        Long l = new Long(System.currentTimeMillis()/1000L);
        this.imgDir = "vidID" + l.toString();

        String[] command = new String[]{"mkdir", imgDir};
        java.lang.Process p = rt.exec(command);
        p.waitFor();

        String inputPath = movieDir + "/" + movieName;
        System.out.println(inputPath);
        String outputPath = System.getProperty("user.dir") + "/" + imgDir + "/img%04d.png";
        int fps = 1;

        //call ffmpeg extractor
        PreProcessor.extractFrames(inputPath, outputPath, fps);

        numImages = new File(System.getProperty("user.dir") + "/" + imgDir).listFiles().length;



        
        threshold = 255 - (int) (255 * .2);

    }



    public void createFrames() {
        PreProcessor.colorCorrectFrames(numImages, imgDir);
        try {
            BufferedImage im = ImageIO.read(new File(imgDir + "/img" + String.format("%04d", 1) + ".png"));
            regionDim = im.getHeight() / 100; //should be a function of cc
            regions = new Region[numImages][im.getWidth() / regionDim][im.getHeight() / regionDim];
            larvaLoc = new boolean[numImages][im.getWidth() / regionDim][im.getHeight() / regionDim];
            for (int f = 0; f < numImages; f++) {
                BufferedImage image = ImageIO.read(new File(imgDir + "/cc" + String.format("%04d", f+1) + ".png"));
                createRegions(f, image);
                BufferedImage locations = fillLarvaLoc(f);
                //ImageIO.write(averages, "png", new File("assets/avg" + String.format("%04d", f+1) + ".png"));
                ImageIO.write(locations, "png", new File("assets/bool" + String.format("%04d", f+1) + ".png"));



            }

            ArrayList<Double[]> islands = getIslandList(0);
            for ( Double[] island : islands ) {
                System.out.println("x: " + island[0] + " y: " + island[1]);

            }

//            for(int f = 0; f<numImages; f++){
//                //getIslandList(f);
//            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
    }

    private void  createRegions(int frame, BufferedImage image) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        for(int i = 0; i < regions[0].length; i++) {
            for (int j = 0; j < regions[0][0].length; j++){
                Region region = new Region(image.getSubimage(i*regionDim, j*regionDim, regionDim, regionDim));
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
                if(larvaLoc[frame][i][j]){ b= 0;}
                array.setRGB(i,j,new Color(b,b,b).getRGB());
            }
        }
        return array;
    }

    private ArrayList<Double[]> getIslandList(int frame){
        //depth first search
        boolean visited[][] = new boolean[larvaLoc[0].length][larvaLoc[0][0].length];
        ArrayList<Double[]> coords = new ArrayList<Double[]>();
        for(int i = 0; i < larvaLoc[0].length; i++){
            for(int j = 0; j< larvaLoc[0][0].length; j++){

                if(larvaLoc[frame][i][j] && !visited[i][j]) {
                    visited[i][j] = true;
                    Double[] island = getIsland(frame, i, j, visited);
                    coords.add(island);

                } else {
                    visited[i][j] = true;
                }

            }
        }

        return coords;
    }


    private Double[] getIsland(int frame, int x, int y, boolean[][] visited){ //cc as in contiguousCoords
        Double[] island = new Double[3]; // island is defined as {x, y, mass}
        ArrayList<double[]> points = islandDFS(frame, x,y,visited, new ArrayList<double[]>());
        double mass = points.size();
        double xc = 0;
        double yc = 0;
        for(int i = 0; i< mass; i++){
            xc += points.get(i)[0];
            yc += points.get(i)[1];
        }
        island[0] = xc/mass * regionDim;
        island[1] = yc/mass * regionDim;
        island[2] = mass;
        return island;
    }
    
    private ArrayList<double[]> islandDFS(int frame, int x, int y, boolean[][] visited, ArrayList<double[]> points){
        double[] here = {x,y};
        points.add(here);
        //ArrayList<double[]> directions = new ArrayList<double[]>();
        visited[x][y] = true;
        int xx, yy;

        //N
        xx = x;
        yy = y+1;
        if(validCoords(xx,yy)){
            if ( !(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true){
                visited[xx][yy]=true;
                points = islandDFS(frame,xx,yy,visited,points);
            }
        }


        //E
        xx = x+1;
        yy = y;
        if(validCoords(xx,yy)){
            if ( !(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true){
                visited[xx][yy]=true;
                points = islandDFS(frame,xx,yy,visited,points);
            }
        }

        //S
        xx = x;
        yy = y-1;
        if(validCoords(xx,yy)){
            if ( !(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true){
                visited[xx][yy]=true;
                points = islandDFS(frame,xx,yy,visited,points);
            }
        }

        //W
        xx = x-1;
        yy = 0;
        if(validCoords(xx,yy)){
            if ( !(visited[xx][yy]) && larvaLoc[frame][xx][yy] == true){
                visited[xx][yy]=true;
                points = islandDFS(frame,xx,yy,visited,points);
            }
        }

        return points;
    }

    private boolean validCoords(int x, int y){
        return x>= 0 && x<larvaLoc[0].length && y >=0 && y<larvaLoc[0][0].length;
    }
    
    
    
    
    
	
	public ArrayList<Larva> getLarva() {
		return larvae;
	}
	
	public void addLarva(Larva l) {
		System.out.println("added larva: ");
		Double[] a = l.getPosition(0);
		for (int i = 0; i < 2; i++) {
			System.out.println("\t" + Double.toString(a[i]));
		}
		larvae.add(l);
	}

    public String getPathToFrame(int index){
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
}
