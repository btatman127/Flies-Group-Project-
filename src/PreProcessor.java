import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.lang.Math;
import java.util.LinkedList;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PreProcessor {

    public static Image scale(String filename, int width, int height) {
        double displayAngle = Math.atan2(height, width);
        if(displayAngle < 0){ displayAngle += (2*Math.PI);}
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            double imageAngle = Math.atan2(image.getHeight(),image.getWidth());
            if(imageAngle < 0){ imageAngle += (2*Math.PI);}
            Image scaleImage;

            if(displayAngle >= imageAngle){
                //Giving -1 keeps the Image's original aspect ratio.
                scaleImage = image.getScaledInstance(width, -1, Image.SCALE_DEFAULT);
            }else{
                scaleImage = image.getScaledInstance(-1, height, Image.SCALE_DEFAULT);
            }
            return scaleImage;
            //BufferedImage subimage = new BufferedImage(scaleImage.getWidth(null), scaleImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
            //subimage.getGraphics().drawImage(scaleImage, 0, 0, null);
            //ImageIO.write(subimage, "png", new File("assets/img" + String.format("%03d", frame + 1) + ".png"));
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
                BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%04d", i) + ".png"));
                BufferedImage subimage = cropImage(image, point1, point2);
                ImageIO.write(subimage, "png", new File(directory + "/img" + String.format("%04d", i) + ".png"));
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
        return image.getSubimage(topLeft[0], topLeft[1], Math.abs(point1[0] - point2[0]), Math.abs(point1[1] - point2[1]));
    }

    static double setScaleFactor(int[]point1, int[]point2) {
        int width = Math.abs(point1[0] - point2[0]);
        return (12.7 * 5 / width);
    }

    /**
     * @param frames
     */
    static void colorCorrectFrames(int frames, String directory) {
        for (int i = 1; i <= frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%04d", i) + ".png"));
                BufferedImage colorImage = colorCorrect(image);
                ImageIO.write(colorImage, "png", new File(directory + "/img" + String.format("%04d", i) + ".png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    static BufferedImage colorCorrect(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        LimitedQueue queue = new LimitedQueue(200);

        double threshold = 0;
        for (int y = 0; y < height; y++) {
            int average = 0;
            for (int x = 0; x < width; x++) {
                int colorValue = image.getRGB(x, y);
                //System.out.println("x: " +x+ " y: " + y + "Color: " + colorValue);
                //get average RGB value
                int red = (colorValue >> 16) & 0xFF;
                int green = (colorValue >> 8) & 0xFF;
                int blue = (colorValue) & 0xFF;
                //System.out.println("Red " + red + " blue: " + blue + " green " + green);
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
            threshold = (queue.sum() * .75);
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
		
		//Path outPath = Paths.get(outputDir);
		//outPath = outPath.resolve("img%04d.png"); 
		
		String[] command = new String[]{"ffmpeg", "-i", inputPath, "-vf", "fps="+fps, outputPath}; 
		for (int i = 0; i < command.length; i++) {
			System.out.println(command[i]);
		}
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
	/** Takes a start time and end time and tells ffmpeg to trim video before images are extracted **/
	public static void cropVideo ( int startTime, int endTime, String inputPathLong, String outputPathLong)  throws java.io.IOException, java.lang.InterruptedException {

        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
	    String[] command = new String[]{"ffmpeg", "-ss", String.valueOf(startTime), "-i", inputPathLong, "-c", "copy", "-t", String.valueOf(endTime), outputPathLong};
        for (int i = 0; i < command.length; i++) {
            System.out.println(command[i]);
        }
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