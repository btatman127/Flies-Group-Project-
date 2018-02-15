import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;

public class PreProcessor {

    public static void main(String[] args) {

    }

    /**
     * Crops all the images
     * @param point1 An integer array {x,y} of one coordinate to crop.
     * @param point2 An integer array {x,y} of one coordinate to crop.
     * @param frames The total number of frames in the video to crop.
     */
    public static void crop(int[] point1, int[] point2, int frames) {
        for (int i = 0; i < frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File("assets/" + i + ".png"));
                BufferedImage subimage = cropImage(image,point1,point2);
                ImageIO.write(subimage, "png", new File("assets/" + i + ".png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * @return An integer array {x,y} of the top left point, given any two points.
     */
    static int[] topLeft(int[] point1, int[]point2) {
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
     * @param image The image to crop.
     * @return A cropped image.
     */
    static BufferedImage cropImage(BufferedImage image, int[] point1, int[]point2) {
        int[] topLeft = topLeft(point1, point2);
        return image.getSubimage(topLeft[0], topLeft[1], Math.abs(point1[0] - point2[0]), Math.abs(point1[1] - point2[1]));
    }


}
