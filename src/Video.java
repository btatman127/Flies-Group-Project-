import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Video {
    private String movieDir;
    private String movieName;
    private String imgDir;
    private int numImages;
	// length and/or width of each grid square in mm
	private final int gridSquareSize = 13; 
	private ArrayList<Larva> larvae;
	
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

		/*
        //get total num frames
        command = new String[]{"ls", "-l", System.getProperty("user.dir") + "/" + imgDir, "|", "egrep", "-c", "\'^-\'"};
        java.lang.Process pnew = rt.exec(command);
        pnew.waitFor();

        InputStreamReader isr = new InputStreamReader(System.in);
        System.out.println("got here first\n");
        BufferedReader br = new BufferedReader(isr);
        System.out.println("got here\n");
        String str = br.readLine();
        System.out.println("get here last\n");
        try{
            numImages = Integer.valueOf(str).intValue();
        } catch (NumberFormatException nfe){
            System.out.println("Incorrect format!");
        }

        System.out.println(numImages);
		*/
    }
	
	public ArrayList<Larva> getLarva() {
		return larvae;
	}
	
	public void addLarva(Larva l) {
		larvae.add(l	);
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
}
