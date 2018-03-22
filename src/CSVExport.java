import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;


public class CSVExport {
	private String result;
	private int frames;


	public CSVExport(ArrayList<Larva> larvae, int frames) {
		result = "";
		this.frames = frames;
		for (int i = 0; i < larvae.size(); i++) {
			String larvaName = "larva" + Integer.toString(i+1);
			result = result + larvaName;
			if (i != larvae.size()-1) {
				result = result + ",,";
			}
		}
		result = result + "\n";
		for (int row = 0; row < frames; row++) {
			System.out.println("row:" + row + "frames:" + frames);
			for (int coord = 0; coord < larvae.size(); coord++) {
				String x = Double.toString(larvae.get(coord).getPosition(row)[0]);
				String y = Double.toString(larvae.get(coord).getPosition(row)[1]);
				result = result + x + "," + y;
				if (coord != larvae.size()-1) {
					result = result + ",";
				}
			}
			result = result + "\n";
		}
	}
	public void export() {
		try {
			PrintWriter out = new PrintWriter("flies.csv");
			out.println(result);
		}
		catch(IOException e) {
		}
	}
}