import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PreProcessor {

    public static void main( String[] args){
        crop();
    }

    public static void crop(){
        try{
            File f = new File("../assets/cafe.png");
            BufferedImage image = ImageIO.read(f);
            int height = image.getHeight();
            int width = image.getWidth();
            System.out.println("Height = " + height + "; Width = " + width);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }



}
