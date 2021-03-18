import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.lang.Math;
import java.util.LinkedList;
import java.nio.file.Paths;



public class PreProcessor {

    private static String durationSeconds;

    /**
     *
     * @param filename The file to scale
     * @param width The desired width
     * @param height The desired height
     * @return An image scaled to the specified width and height. The image remains in aspect, and is scaled to the largest it can be
     * without becoming stretched.
     */
    public static Image scale(String filename, int width, int height) {
        double displayAngle = Math.atan2(height, width);
        if (displayAngle < 0) {
            displayAngle += (2 * Math.PI);
        }
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            double imageAngle = Math.atan2(image.getHeight(), image.getWidth());
            if (imageAngle < 0) {
                imageAngle += (2 * Math.PI);
            }
            Image scaleImage;

            if (displayAngle >= imageAngle) {
                //Giving -1 keeps the Image's original aspect ratio.
                scaleImage = image.getScaledInstance(width, -1, Image.SCALE_DEFAULT);
            } else {
                scaleImage = image.getScaledInstance(-1, height, Image.SCALE_DEFAULT);
            }
            return scaleImage;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * Crops all the images
     *
     * @param point1 An integer array {x,y} of one coordinate to crop.
     * @param point2 An integer array {x,y} of one coordinate to crop.
     * @param frames The total number of frames in the video to crop.
     */
    public static void crop(int[] point1, int[] point2, int frames, String directory) {
        for (int i = 1; i <= frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File(Paths.get(directory).resolve("img" +
                                                                                         String.format("%04d", i) +
                                                                                         ".png").toString()));
                BufferedImage subimage = cropImage(image, point1, point2);
                ImageIO.write(subimage, "png", new File(Paths.get(directory).resolve("img" +
                                                                        String.format("%04d", i) + ".png").toString()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * @return An integer array {x,y} of the top left point, given any two points.
     */
    static int[] topLeft(int[] point1, int[] point2) {
        int point[] = new int[2];
        if (point1[0] <= point2[0]) {
            point[0] = point1[0];
        } else {
            point[0] = point2[0];
        }
        if (point1[1] <= point2[1]) {
            point[1] = point1[1];
        } else {
            point[1] = point2[1];
        }
        return point;
    }

    /**
     * Crops a given image.
     *
     * @param image The image to crop.
     * @return A cropped image.
     */
    static BufferedImage cropImage(BufferedImage image, int[] point1, int[] point2) {
        int[] topLeft = topLeft(point1, point2);
        return image.getSubimage(topLeft[0], topLeft[1],
                                 Math.abs(point1[0] - point2[0]), Math.abs(point1[1] -point2[1]));
    }


    /**
     * Carries out a color correction algorithm on the frames in a given directory.
     * @param frames The number of frames to modify.
     */
    static void colorCorrectFrames(int frames, String directory) {
        for (int i = 1; i <= frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File(Paths.get(directory).resolve("img" +
                                                                        String.format("%04d", i) + ".png").toString()));
                BufferedImage colorImage = colorCorrect(image);
                ImageIO.write(colorImage, "png", new File(Paths.get(directory).resolve("cc" +
                                                                        String.format("%04d", i) + ".png").toString()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * For a specific image, color correct that image.
     * @param image The image to color correct.
     * @return A black and white image which attempts to correct for bad video quality to provide a clearer idea of
     * what is actually going on.
     */
    static BufferedImage colorCorrect(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        LimitedQueue queue = new LimitedQueue(height/4);

        double threshold = 0;
        for (int y = 0; y < height; y++) {
            int average = 0;
            for (int x = 0; x < width; x++) {
                int colorValue = image.getRGB(x, y);
                //get average RGB value
                int red = (colorValue >> 16) & 0xFF;
                int green = (colorValue >> 8) & 0xFF;
                int blue = (colorValue) & 0xFF;
                int colorAvg = (red + green + blue) / 3;
                average += colorAvg;
                if (colorAvg > threshold) {
                    image.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    image.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
            average = average / width;
            queue.add(average);
            threshold = (queue.sum() * .8);
        }
        return image;
    }


	/**
	* Sends ffmpeg command to the shell to extract frames of a video as .png files in given directory
	* @param inputPath
	* @param outputPath   this String should end with a / character
	* @param fps    a value of 1 will extract 1 frame for each second of video
	*/

    public static void extractFrames(String inputPath, String outputPath, int fps) throws java.io.IOException, java.lang.InterruptedException {

        // Get runtime
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();


        String[] command = new String[]{"ffmpeg", "-i", inputPath, "-vf", "fps=" + fps, outputPath};
        java.lang.Process p = rt.exec(command);
        // You can or maybe should wait for the process to complete
		p.waitFor();

        //CODE TO COLLECT RESULTANT INPUT STREAM:
        java.io.InputStream is = p.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
        
		// And print each line
        String s = null;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
        }
        is.close();
	}


    /**
     * Takes a start time and end time and tells ffmpeg to trim video before images are extracted
     **/
    public static void cropVideo(int startTime, int endTime, String inputPathLong, String outputPathLong) throws java.io.IOException, java.lang.InterruptedException {


        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        //TODO look at end time
        int duration = endTime - startTime;
        String[] command = new String[]{"ffmpeg", "-ss", String.valueOf(startTime), "-i", inputPathLong, "-c", "copy", "-t", String.valueOf(duration), outputPathLong};
        java.lang.Process p = rt.exec(command);
        // You can or maybe should wait for the process to complete
        p.waitFor();

        //CODE TO COLLECT RESULTANT INPUT STREAM:
        java.io.InputStream is = p.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
        // And print each line
        String s = null;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
        }
        is.close();

    }
	
    /** Gets duration in seconds from ffmpeg as a String**/
    public static String getDuration(String movieDir, String movieNameLong) throws IOException, InterruptedException {
        String[] command2 = new String[]{"ffprobe", "-v", "quiet", "-print_format",
                                         "compact=print_section=0:nokey=1:escape=csv", "-show_entries",
                                         "format=duration", Paths.get(movieDir).resolve(movieNameLong).toString()};

        java.lang.Runtime rt2 = java.lang.Runtime.getRuntime();
        java.lang.Process p2 = rt2.exec(command2);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            durationSeconds = s;
        }
        //wait for command to finish
        p2.waitFor();
        return durationSeconds;
    }
    /** Gets duration of movie in seconds from getDuration() and converts to int, contains a try catch to handle exceptions from getDuration()**/
    public static String getDurationSeconds(String movieDir, String movieNameLong) {
        try {
            getDuration(movieDir, movieNameLong);
        } catch (Exception e) {
        }
        double duration = Double.parseDouble(durationSeconds);
        int durationInt = (int) duration;
        return Integer.toString(durationInt);

    }
}


    class LimitedQueue extends LinkedList<Integer> {
        private int limit;

        public LimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(Integer o) {
            super.add(o);
            while (size() > limit) {
                super.remove();
            }
            return true;
        }

        public int sum() {
            int ans = 0;
            for (int i = 0; i < this.size(); i++) {
                ans += this.get(i);
            }
            return ans / this.size();
        }
}