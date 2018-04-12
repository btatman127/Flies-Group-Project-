import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;


public class CSVExport {
    private static String result;
    private int frames;

    public CSVExport(ArrayList<Larva> larvae, int frames) {
        result = "";
        this.frames = frames;
        for (int i = 0; i < larvae.size(); i++) {
            String larvaName = "larva" + Integer.toString(i + 1);
            result = result + larvaName;
            if (i != larvae.size() - 1) {
                result = result + ",,";
            }
        }
        result = result + "\n";
        for (int row = 0; row < frames; row++) {
            System.out.println("row:" + row + "frames:" + frames);
            for (int coord = 0; coord < larvae.size(); coord++) {
                String x = "";
                String y = "";
                if (row < larvae.get(coord).getPositionsSize()) {
                    x = String.format ("%.2f", (larvae.get(coord).getPosition(row)[0]));
                    y = String.format ("%.2f", (larvae.get(coord).getPosition(row)[1]));;
                }
                result = result + x + "," + y;
                if (coord != larvae.size() - 1) {
                    result = result + ",";
                }


            }
            result = result + "\n";
        }
    }

    public void export() {
        System.out.println("csv export: \n" + result);
        try {
            PrintWriter out = new PrintWriter("flies.csv");
            out.write(result);
            out.close();
        } catch (IOException e) {
            System.out.println("Wasn't able to save csv");
        }
    }
}