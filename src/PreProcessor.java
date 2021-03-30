import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.lang.Math;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Scanner;


public class PreProcessor {
    /**
     *
     * @param file The file to scale
     * @param width The desired width
     * @param height The desired height
     * @return An image scaled to the specified width and height. The image remains in aspect, and is scaled to the largest it can be
     * without becoming stretched.
     */
    public static Image scale(Path file, int width, int height) {
        double displayAngle = Math.atan2(height, width);
        if (displayAngle < 0) {
            displayAngle += (2 * Math.PI);
        }
        try {
            BufferedImage image = ImageIO.read(file.toFile());
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
     * @return An integer array {x,y} of the top left point, given any two points.
     */
    static int[] findTopLeftCropCorner(int[] point1, int[] point2) {
        return new int[] {
            Math.min(point1[0], point2[0]),
            Math.min(point1[1], point2[1])
        };
    }

    /**
     * Crops a given image.
     *
     * @param image The image to crop.
     * @return A cropped image.
     */
    static BufferedImage cropImage(BufferedImage image, int[] point1, int[] point2) {
        int[] topLeft = findTopLeftCropCorner(point1, point2);
        return image.getSubimage(topLeft[0], topLeft[1],
                                 Math.abs(point1[0] - point2[0]), Math.abs(point1[1] -point2[1]));
    }


    /**
     * Carries out a color correction algorithm on the frames in a given directory.
     * @param frames The number of frames to modify.
     */
    static void colorCorrectFrames(int frames, Path directory) {
        for (int i = 1; i <= frames; i++) {
            try {
                BufferedImage image = ImageIO.read(directory.resolve(String.format("img%04d.png", i)).toFile());
                BufferedImage colorImage = colorCorrect(image);
                ImageIO.write(colorImage, "png", directory.resolve(String.format("cc%04d.png", i)).toFile());
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
	* @param inputPath the video from which to extract frames
	* @param outputPath a format string for output image paths
	* @param fps a value of 1 will extract 1 frame for each second of video
	*/

    public static void extractFrames(String inputPath, String outputPath, int fps) throws java.io.IOException, java.lang.InterruptedException {
        String[] command = new String[]{
                "ffmpeg", "-i", inputPath,
                "-vf", "fps=" + fps, outputPath};
        Runtime.getRuntime().exec(command).waitFor();
	}


    /**
     * Takes a start time and end time and tells ffmpeg to trim video before images are extracted
     **/
    public static void cropVideo(int startTime, int endTime, String inputPathLong, String outputPathLong) throws java.io.IOException, java.lang.InterruptedException {
        int duration = endTime - startTime;
        String[] command = new String[]{
                "ffmpeg", "-ss", String.valueOf(startTime),
                "-i", inputPathLong,
                "-c", "copy",
                "-t", String.valueOf(duration),
                outputPathLong};
        Runtime.getRuntime().exec(command).waitFor();
    }

    /** Gets video duration in seconds from ffmpeg. **/
    public static int getVideoDuration(File movie) throws IOException {
        String[] command2 = new String[]{"ffprobe", "-v", "quiet", "-print_format",
                                         "compact=print_section=0:nokey=1:escape=csv", "-show_entries",
                                         "format=duration", movie.getAbsolutePath()};

        java.lang.Runtime rt2 = java.lang.Runtime.getRuntime();
        java.lang.Process p2 = rt2.exec(command2);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));
        Scanner scanner = new Scanner(stdInput);
        return (int) scanner.nextDouble();
    }
}


class LimitedQueue extends LinkedList<Integer> {
    private final int limit;

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
        for (int n : this) {
            ans += n;
        }
        return ans / this.size();
    }
}