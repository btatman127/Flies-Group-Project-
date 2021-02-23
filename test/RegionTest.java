import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {
    @Test
    public void findsBrightnessOfSolidColor() throws IOException {
        File red = new File("test_resources/red.png");
        BufferedImage image = ImageIO.read(red);
        Region region = new Region(image);
        assertEquals(255 / 3, region.getAvgValue());

    }

    @Test
    public void findsBrightnessOfMultipleColors() throws IOException {
        File redGreen = new File("test_resources/red_green.png");
        BufferedImage image = ImageIO.read(redGreen);
        Region region = new Region(image);
        assertEquals(255 / 3, region.getAvgValue());

        File blackWhite = new File("test_resources/black_white.png");
        BufferedImage image2 = ImageIO.read(blackWhite);
        Region region2 = new Region(image2);
        assertEquals(255 / 2, region2.getAvgValue());
    }
}