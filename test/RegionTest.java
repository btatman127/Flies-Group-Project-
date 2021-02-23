import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {
    private BufferedImage image;

    @Test
    public void solidColor() throws IOException {
        File red = new File("test_resources/red.png");
        image = ImageIO.read(red);
        Region region = new Region(image);
        assertEquals((255)/3, region.getAvgValue());

    }

    @Test
    public void multiColor() throws IOException {
        File redGreen = new File("test_resources/red_green.png");
        image = ImageIO.read(redGreen);
        Region region = new Region(image);
        assertEquals((255)/3, region.getAvgValue());
        File blackWhite = new File("test_resources/black_white.png");
        image = ImageIO.read(blackWhite);
        Region region2 = new Region(image);
        assertEquals((255)/2, region2.getAvgValue());
    }
}