import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

public class PreProcessorTest {

    private BufferedImage image;

    @BeforeEach
    public void setUp() throws Exception{
        URL url = new URL("https://confluence.jetbrains.com/download/attachments/10818/IDEADEV?version=6&modificationDate=1449747979000&api=v2");
        //Gets IntelliJ logo from internet to test image methods on.
        image = ImageIO.read(url);
    }

    @Test
    public void cropCropsCorrectly(){
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        BufferedImage croppedImage = PreProcessor.cropImage(image, point1, point2);
        assertEquals( croppedImage.getHeight(),100 );
        assertEquals( croppedImage.getWidth(), 50);
    }

    @Test
    public void topLeftReturnsTopLeftPoint(){
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        int[] point3 = {100,10};
        assertArrayEquals(PreProcessor.findTopLeftCropCorner(point1, point2), new int[] {50,50});
        assertArrayEquals(PreProcessor.findTopLeftCropCorner(point2, point1), new int[] {50,50});
        assertArrayEquals(PreProcessor.findTopLeftCropCorner(point1, point3), new int[] {50,10});
    }


	 @Test
	 public void ffmpegExtractsImages() throws java.io.IOException, java.lang.InterruptedException {
	 	long l = System.currentTimeMillis() / 1000L;
	 	String dirName = "vidID" + l;
	 	new File(dirName).mkdir();

	 	String inputPath = Paths.get(System.getProperty("user.dir")).resolve("test.MOV").toString();
	 	System.out.println(inputPath);
	 	String outputPath = Paths.get(System.getProperty("user.dir")).resolve(dirName).resolve("img%04d.png").toString();
	 	int fps = 1;

	 	//call ffmpeg extractor
	 	PreProcessor.extractFrames(inputPath, outputPath, fps);
	 }

}
