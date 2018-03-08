public class Video {
    private String dir;
    private String fileName;
    private String folderName;


    /**
     * Constructor for a Video object
     * @param dir   the directory where the movie is located
     * @param fileName  the name of the movie
     * @param folderName    the name of the folder the generated png images will be put in
     */
    public Video(String dir, String fileName, String folderName){
        this.dir = dir;
        this.fileName = fileName;
        this.folderName = folderName;

        //call the preprocessor function to make the images from the movie
        //use the folder name that is given to the video class as the outputPath
    }

    public String getPathToFrame(int index){
        String path;

        path = folderName + "/img" + String.format("%03d", index) + ".png";

        return path;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDir() {
        return dir;
    }

    public String getFileName() {
        return fileName;
    }
}
