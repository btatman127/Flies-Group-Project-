import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Region {
    private BufferedImage image;
    private int avgValue;
    //private int depth;
    private int width;
    private int height;

    public Region(BufferedImage image) {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
        findAvgValue();
    }

    /**
     * Finds the average brightness of this region.
     */
    private void findAvgValue() {
        int avg = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //getRGB method returns a color in format #RRGGBB in Hex. We use bit shifting operations to extract the correct values
                int colorValue = image.getRGB(i, j);
                int r = (colorValue >> 16) & 0xFF;
                int g = (colorValue >> 8) & 0xFF;
                int b = (colorValue) & 0xFF;
                avg += (r + g + b) / 3;
            }
        }
        avgValue = avg / (width * height);
    }

    /**
     * @return The average brightness of the region.
     */
    public int getAvgValue() {
        return avgValue;
    }

}



