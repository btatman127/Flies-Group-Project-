public class Video {
    private String movieDir;
    private String movieName;
    private String imgDir;


    /**
     * Constructor for a Video object
     * @param movieDir   the movieDir file where the movie is located
     * @param movieName  the name of the movie
     * @param imgDir    the name of the folder the generated png images will be put in
     */
    public Video(String movieDir, String movieName, String imgDir){
        this.movieDir = movieDir;
        this.movieName = movieName;
        this.imgDir = imgDir;


        //call the preprocessor function to make the images from the movie
        //use the folder name that is given to the video class as the outputPath
    }

    public String getPathToFrame(int index){
        String path;

        path = imgDir + "/img" + String.format("%03d", index) + ".png";

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
