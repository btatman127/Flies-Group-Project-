import java.lang.Thread;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import javax.swing.JProgressBar;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

class CropThread implements Runnable{
	private Thread t;
	private String threadName;
	//private int progress;
	private JProgressBar progressBar;
	
	int[] point1;
	int[] point2;
	final int frames;
	String directory;
	
	public CropThread(String name, int[] point1, int[] point2, int frames, String directory, JProgressBar progressBar) {
		threadName  = name;
		this.point1 = point1.clone();
		this.point2 = point2.clone();
		this.frames = frames;
		this.directory = directory;
		this.progressBar = progressBar;
	}
	
	public void run() {
		try {
			int i = 1;
			while (i <= frames) {
				final int j = i;
				try {
					EventQueue.invokeAndWait(() -> {
		                try {
							final BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%04d", j) + ".png"));
		                	BufferedImage subimage = PreProcessor.cropImage(image, point1, point2);
		                	ImageIO.write(subimage, "png", new File(directory + "/img" + String.format("%04d", j) + ".png"));
						}
						catch(IOException ioe) {
							ioe.printStackTrace();
						}
						progressBar.setValue(j);
					});
				}
				catch(InvocationTargetException ine) {
					ine.printStackTrace();
				}
				
				//Thread.sleep(1);
				i++;
			}
		}
		catch(InterruptedException ine) {
			ine.printStackTrace();
		}
			//
	//     for (int i = 1; i <= frames; i++) {
	//             try {
	//                 BufferedImage image = ImageIO.read(new File(directory + "/img" + String.format("%04d", i) + ".png"));
	//                 BufferedImage subimage = PreProcessor.cropImage(image, point1, point2);
	//                 ImageIO.write(subimage, "png", new File(directory + "/img" + String.format("%04d", i) + ".png"));
	//             }
	// 		catch (IOException ioe) {
	//                 ioe.printStackTrace();
	//             }
	// 		progress = i;
	//         }
	}
	
	public void start() {
		if (t == null) {
			t = new Thread(this, threadName);
			t.start();
		}
	}
	
	// public int getProgress() {
	// 	System.out.println(progress);
	// 	return progress;
	// }
}