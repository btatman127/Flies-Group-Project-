import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import javax.swing.JProgressBar;

class CropImages {

	private JProgressBar progressBar;
	int[] point1;
	int[] point2;
	final int frames;
	String directory;
	
	public CropImages(int[] point1, int[] point2, int frames, String directory, JProgressBar progressBar) {
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
				final BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%04d", i) + ".png"));
				BufferedImage subimage = PreProcessor.cropImage(image, point1, point2);
				ImageIO.write(subimage, "png", new File(directory + "/img" + String.format("%04d", i) + ".png"));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			progressBar.setValue(i++);
		}

		progressBar.setVisible(false);
	}
}