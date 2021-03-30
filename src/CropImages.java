import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JProgressBar;

class CropImages {

    private JProgressBar progressBar;
    int[] point1;
    int[] point2;
    final int frames;
    Path directory;

    public CropImages(int[] point1, int[] point2, int frames, Path directory, JProgressBar progressBar) {
        this.point1 = point1.clone();
        this.point2 = point2.clone();
        this.frames = frames;
        this.directory = directory;
        this.progressBar = progressBar;
    }

    public void run() {
        int i = 1;
        progressBar.setVisible(true);
        while (i <= frames) {
            try {
                final BufferedImage image = ImageIO.read(
                        new File(directory.resolve(String.format("img%04d.png", i)).toString()));
                BufferedImage subimage = PreProcessor.cropImage(image, point1, point2);
                ImageIO.write(subimage, "png",
                        new File(directory.resolve(String.format("img%04d.png", i)).toString()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            progressBar.setValue(i++);
        }

        progressBar.setVisible(false);
    }
}