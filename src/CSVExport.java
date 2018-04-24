import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CSVExport {
    private static String result;
    private int frames;
    private double scaleX;
    private double scaleY;
    private Video movie;

    public CSVExport(Video movie, int frames) {
        result = "";
        this.frames = frames;
        this.movie = movie;
        ArrayList<Larva> larvae = movie.getLarva();

        setConversionScale();

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
            for (int coord = 0; coord < larvae.size(); coord++) {
                String x = "";
                String y = "";

                if (row < larvae.get(coord).getPositionsSize()) {
                    x = String.format("%.2f", (larvae.get(coord).getPosition(row)[0] * scaleX));
                    y = String.format("%.2f", ((movie.getDimensions()[1] - larvae.get(coord).getPosition(row)[1]) * scaleY));
                }
                result = result + x + "," + y;
                if (coord != larvae.size() - 1) {
                    result = result + ",";
                }


            }
            result = result + "\n";
        }

        //add total distance
        result += "\n";
        for (int i = 0; i < larvae.size(); i++) {
            result += "Total Distance: ," + String.format("%.2f", getTotalDistance(larvae.get(i))) + ",";
        }


        //add average velocity
        result += "\n";
        for (int i = 0; i < larvae.size(); i++) {
            result += "Average Velocity: ," + String.format("%.2f", getAverageVelocity(larvae.get(i))) + ",";
//            if (i != larvae.size() - 1) {
//                result = result + ",,";
//            }
        }

        result += "\n";
    }


    public void export() {

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

    public void setConversionScale(){
        double mm = 76.0; //The grid is 3" by 3", which translates into about 76 mm.
        scaleX = mm/movie.getDimensions()[0];
        scaleY = mm/movie.getDimensions()[1];
    }

    private double distance(Double[] a, Double[] b) {
        return Math.sqrt(Math.pow(((a[0] - b[0]) * scaleX), 2) + Math.pow(((a[1] - b[1]) * scaleY), 2));
    }

    private double getTotalDistance(Larva larva){
        double sum = 0;
        for(int i = 0; i<larva.getPositionsSize()-1; i++){
            sum += distance(larva.getPosition(i), larva.getPosition(i+1));
        }
        return sum;
    }

    private double getAverageVelocity(Larva larva){
        return getTotalDistance(larva)/(larva.getPositionsSize() -1);
    }

}