import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.util.LinkedList;


public class PreProcessor {

    public static void main(String[] args) {
        scale(90);
    }

    public static void scale(int frames){
        for (int i = 0; i < frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File("assets/img" + String.format("%03d", i + 1) + ".png"));
                Image scaleImage = image.getScaledInstance((int) (Toolkit.getDefaultToolkit().getScreenSize().width * .8),-1,Image.SCALE_DEFAULT);

                BufferedImage subimage = new BufferedImage(scaleImage.getWidth(null), scaleImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
                subimage.getGraphics().drawImage(scaleImage, 0, 0, null);

                ImageIO.write(subimage, "png", new File("assets/img" + String.format("%03d", i + 1) + ".png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    /**
     * Crops all the images
     *
     * @param point1 An integer array {x,y} of one coordinate to crop.
     * @param point2 An integer array {x,y} of one coordinate to crop.
     * @param frames The total number of frames in the video to crop.
     */
    public static void crop(int[] point1, int[] point2, int frames) {
        for (int i = 0; i < frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File("assets/img" + String.format("%03d", i + 1) + ".png"));
                BufferedImage subimage = cropImage(image, point1, point2);
                ImageIO.write(subimage, "png", new File("assets/img" + String.format("%03d", i + 1) + ".png"));
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

    /**
     * @param frames
     */
    static void colorCorrectFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            try {
                BufferedImage image = ImageIO.read(new File("assets/img" + String.format("%03d", i) + ".png"));
                BufferedImage colorImage = colorCorrect(image);
                ImageIO.write(colorImage, "png", new File("assets/img" + String.format("%03d", i) + ".png"));
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