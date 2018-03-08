import java.io.IOException;

public class Video {
    private String movieDir;
    private String movieName;
    private String imgDir;


    /**
     * Constructor for a Video object
     * @param movieDir   the movieDir file where the movie is located
     * @param movieName  the name of the movie
     */
    public Video(String movieDir, String movieName) throws IOException, InterruptedException {
        this.movieDir = movieDir;
        this.movieName = movieName;

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


        // System.getProperty("user.dir") --> "working directory"
        // create the imgDir --> involves finding the working dir of the java application
        // call the preprocessor function


        //call the preprocessor function to make the images from the movie
        //use the folder name that is given to the video class as the outputPath
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
}
