import java.util.ArrayList;

public class CSVExport {
	private String result = "";

	public CSV(ArrayList<Larva> larvae) {
		for (int i = 0; i < larvae.size(); i++) {
			String larvaName = "larva" + Integer.toString(i+1);
			result = result + larvaName;
			if (i != larvae.size()-1) {
				result = result + ",,";
			}
		}
		result = result + "\n";
		for (int row = 0; row < larvae.get(0).size(); row++) {
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
			out.println(result;)
		}
		catch {
			IOException e;
		}
	}
}