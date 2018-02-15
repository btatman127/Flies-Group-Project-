import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.scale.*;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PreProcessor {

    public static void main(String args[]) {
        int frameNumber = 1;
        Picture picture = AWTFrameGrab.getFrameFromFile(new File("sample.mp4"), frameNumber);

//for JDK (jcodec-javase)
        BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
        ImageIO.write(bufferedImage, "png", new File("frame1.png"));
    }


    public static void crop() {

    }



}
