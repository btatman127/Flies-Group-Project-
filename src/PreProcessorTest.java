import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreProcessorTest {

    private BufferedImage image;

    @org.junit.Before
    public void setUp() throws Exception{
        URL url = new URL("https://confluence.jetbrains.com/download/attachments/10818/IDEADEV?version=6&modificationDate=1449747979000&api=v2");
        //Gets IntelliJ logo from internet to test image methods on.
        image = ImageIO.read(url);
    }

    @org.junit.Test
    public void crop_crops_correctly() throws Exception{
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        BufferedImage croppedImage = PreProcessor.cropImage(image, point1, point2);
        assertTrue( croppedImage.getHeight() == 100 );
        assertTrue( croppedImage.getWidth() == 50);
    }

    @org.junit.Test
    public void topLeft_returns_top_left_point(){
        int[] point1 = {50,50};
        int[] point2 = {100, 150};
        int[] point3 = {100,10};
        assertArrayEquals(PreProcessor.topLeft(point1, point2), new int[] {50,50});
        assertArrayEquals(PreProcessor.topLeft(point2, point1), new int[] {50,50});
        assertArrayEquals(PreProcessor.topLeft(point1, point3), new int[] {50,10});
    }
	
	// @org.junit.Test
	// public void ffmpegExtractsImages() throws java.io.IOException, java.lang.InterruptedException {
	//
	// 	//create a directory to place images into
	// 	java.lang.Runtime rt = java.lang.Runtime.getRuntime();
	// 	Long l = new Long(System.currentTimeMillis()/1000L);
	// 	String dirName = "vidID" + l.toString();
	// 	String[] command = new String[]{"mkdir", dirName};
	// 	java.lang.Process p = rt.exec(command);
	// 	p.waitFor();
	//
	// 	String inputPath = System.getProperty("user.dir") + "/test.MOV";
	// 	System.out.println(inputPath);
	// 	String outputPath = System.getProperty("user.dir") + "/" + dirName + "/img%04d.png";
	// 	int fps = 1;
	//
	// 	//call ffmpeg extractor
	// 	PreProcessor.extractFrames(inputPath, outputPath, fps);
	//
	// }

}
