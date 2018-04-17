import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CSVExport {
    private static String result;
    private int frames;

    public CSVExport(ArrayList<Larva> larvae, int frames) {
        result = "";
        this.frames = frames;

        //add column labels for each larva
        for (int i = 0; i < larvae.size(); i++) {
            String larvaName = "larva" + Integer.toString(i + 1);
            result = result + larvaName;
            if (i != larvae.size() - 1) {
                result = result + ",,";
            }
        }

        //add column labels for x and y
        result += "\n";
        for (int i = 0; i < larvae.size(); i++) {
            result += "x,y";
            if (i < larvae.size()) {
                result += ",";
            }
        }

        //add data
        result = result + "\n";
        for (int row = 0; row < frames; row++) {
            System.out.println("row:" + row + "frames:" + frames);
            for (int coord = 0; coord < larvae.size(); coord++) {
                String x = "";
                String y = "";
                if (row < larvae.get(coord).getSize()) {
                    x = String.format("%.2f", (larvae.get(coord).getPosition(row)[0]));
                    y = String.format("%.2f", (larvae.get(coord).getPosition(row)[1]));
                    ;
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

        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String fileName = "flies" + timestamp + ".csv";
        try {
            PrintWriter out = new PrintWriter(fileName);
            out.write(result);
            out.close();
        } catch (IOException e) {
            System.out.println("Wasn't able to save csv");
        }
    }

}