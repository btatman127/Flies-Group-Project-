import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public void crop_crops_correctly(){
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        BufferedImage croppedImage = PreProcessor.cropImage(image, point1, point2);
        assertEquals( croppedImage.getHeight(),100 );
        assertEquals( croppedImage.getWidth(), 50);
    }

    @Test
    public void topLeft_returns_top_left_point(){
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        int[] point3 = {100,10};
        assertArrayEquals(PreProcessor.topLeft(point1, point2), new int[] {50,50});
        assertArrayEquals(PreProcessor.topLeft(point2, point1), new int[] {50,50});
        assertArrayEquals(PreProcessor.topLeft(point1, point3), new int[] {50,10});
    }


	 @Test
	 public void ffmpegExtractsImages() throws java.io.IOException, java.lang.InterruptedException {

	 	//create a directory to place images into
	 	java.lang.Runtime rt = java.lang.Runtime.getRuntime();
	 	long l = System.currentTimeMillis() / 1000L;
	 	String dirName = "vidID" + l;
	 	String[] command = new String[]{"mkdir", dirName};

         new File(dirName).mkdir();

	 	String inputPath = System.getProperty("user.dir") + "/test.MOV";
	 	System.out.println(inputPath);
	 	String outputPath = System.getProperty("user.dir") + "/" + dirName + "/img%04d.png";
	 	int fps = 1;

	 	//call ffmpeg extractor
	 	PreProcessor.extractFrames(inputPath, outputPath, fps);

	 }

}
