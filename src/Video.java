import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	// length and/or width of each grid square in mm
	private ArrayList<Larva> larvae;
	
    //factor that converts pixels to mm
    private double scaleFactor;

    /**
     * Constructor for a Video object
     * @param movieDir   the movieDir file where the movie is located
     * @param movieNameLong  the name of the movie
     */
    public Video(String movieDir, String movieNameLong, int startTime, int endTime) throws IOException, InterruptedException {
        this.movieDir = movieDir;
        this.movieNameLong = movieNameLong;
		
		//create a list of larva for this video
		larvae = new ArrayList<Larva>();

        String[] command = new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", movieNameLong, ">", "movieDuration.txt"};


		//create input and output paths for the whole video
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        this.movieName = this.movieNameLong.substring(0, this.movieNameLong.length()-4) + "SHORTER" + timestamp + ".mov";
        System.out.print(this.movieName);
        String outputPathLong = movieDir + "/" + this.movieName;
        String inputPathLong = movieDir + "/" + this.movieNameLong;

        //call ffmpeg crop method

        PreProcessor.cropVideo(startTime, endTime, inputPathLong, outputPathLong);
		
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        Long l = new Long(System.currentTimeMillis()/1000L);
        this.imgDir = "vidID" + l.toString();

        String[] command = new String[]{"mkdir", imgDir};
        java.lang.Process p = rt.exec(command);
        p.waitFor();

        String inputPath = this.movieDir + "/" + this.movieName;
        System.out.println(inputPath);
        String outputPath = System.getProperty("user.dir") + "/" + imgDir + "/img%04d.png";
        int fps = 1;

		System.out.println("before preprocessor");
        //call ffmpeg extractor
        PreProcessor.extractFrames(inputPath, outputPath, fps);
		System.out.println("after preprocessor");
        numImages = new File(System.getProperty("user.dir") + "/" + imgDir).listFiles().length;


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
