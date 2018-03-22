import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Tracker {

    public Region[][][] regions; //Region[frame number][x-coordinate][y-coordinate]
    public boolean[][][] larvaLoc;
    public int threshold = 255 - (int) (255 * .2);
    public int regionDim = 8;

    public Tracker(Video vid) {
        createFrames(vid.getNumImages(), vid.getImgDir());
    }

    public Tracker() {
        createFrames(90, "assets");
    }

    public void createFrames(int frames, String directory) {
        try {
            BufferedImage im = ImageIO.read(new File(directory + "/img" + String.format("%03d", 1) + ".png"));
            regions = new Region[frames][im.getWidth() / regionDim][im.getHeight() / regionDim];
            larvaLoc = new boolean[frames][im.getWidth() / regionDim][im.getHeight() / regionDim];
            for (int f = 0; f < frames; f++) {
                BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%03d", f+1) + ".png"));
                BufferedImage averages = createRegions(f, image);
                BufferedImage locations = fillLarvaLoc(f);
                ImageIO.write(averages, "png", new File("assets/avg" + String.format("%03d", f+1) + ".png"));
                ImageIO.write(locations, "png", new File("assets/bool" + String.format("%03d", f+1) + ".png"));

            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public BufferedImage createRegions(int frame, BufferedImage image) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        BufferedImage averages = new BufferedImage(imgWidth / regionDim, imgHeight / regionDim, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < regions[0].length; i++) {
            for (int j = 0; j < regions[0][0].length; j++){
                Region region = new Region(image.getSubimage(i*regionDim, j*regionDim, regionDim, regionDim));
                regions[frame][i][j] = region;
                averages.setRGB(i, j, new Color(regions[frame][i][j].getAvgValue(), regions[frame][i][j].getAvgValue(), regions[frame][i][j].getAvgValue()).getRGB());
            }
        }
        return averages;
    }

    public int getSample(int frame, int x, int y) {
        int average = 0;
        int count = 0;
        int kernelSize = 3;
        for (int i = x - (kernelSize / 2); i <= x + (kernelSize / 2); i++) {
            if (i >= 0 && i < regions[0].length) {
                for (int j = y - (kernelSize / 2); j <= y + (kernelSize / 2); j++) {
                    if (j >= 0 && j < regions[0][0].length) {
                        count++;
                        average += regions[frame][i][j].getAvgValue();
                        //System.out.println("frame " + frame + " i,j " + i +"," +j);
                    }
                }
            }
        }
        return average /= count;
    }

    public BufferedImage fillLarvaLoc(int frame) {
        BufferedImage array = new BufferedImage(regions[0].length, regions[0][0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < larvaLoc[0].length; i++) {
            for (int j = 0; j < larvaLoc[0][0].length; j++) {
                int avg = getSample(frame, i, j);
                larvaLoc[frame][i][j] = avg < threshold;
                //System.out.println((avg < threshold) + "  avg < thresh " + avg + " " + threshold );
                int b = 255;
                if(larvaLoc[frame][i][j]){ b= 0;}
                array.setRGB(i,j,new Color(b,b,b).getRGB());
            }
        }
        return array;
    }


}
