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
        progressBar.setVisible(true);
        for (int i = 1; i <= frames; i++) {
            try {
                final BufferedImage image = ImageIO.read(directory.resolve(String.format("img%04d.png", i)).toFile());
                BufferedImage subimage = PreProcessor.cropImage(image, point1, point2);
                ImageIO.write(subimage, "png", directory.resolve(String.format("img%04d.png", i)).toFile());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            progressBar.setValue(i);
        }

        progressBar.setVisible(false);
    }
}