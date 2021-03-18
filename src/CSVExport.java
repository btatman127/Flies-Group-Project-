import java.io.File;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CSVExport {
    private static String result;
    private double scaleX;
    private double scaleY;
    private Video movie;

    public CSVExport(Video movie, int frames) {
        result = "";
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
            result += "x (mm),y (mm)";
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
                    //Get pixel x and y positions and convert them into mm. Convert (0,0) from top left to bottom left.
                    Double[] position = larvae.get(coord).getPosition(row);
                    if(position != null) {
                        x = String.format("%.2f", (position[0] * scaleX));
                        y = String.format("%.2f", ((movie.getDimensions()[1] - position[1]) * scaleY));
                    }
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
            result += "Total Distance (mm): ," + String.format("%.2f", getTotalDistance(larvae.get(i))) + ",";
        }


        //add average velocity
        result += "\n";
        for (int i = 0; i < larvae.size(); i++) {
            result += "Average Velocity (mm/sec): ," + String.format("%.2f", getAverageVelocity(larvae.get(i))) + ",";
        }

        result += "\n";
    }

    /**
     * Exports and saves a CSV file containing position, distance, and velocity data for each larva.
     */
    public void export(File file) {
        try {
            PrintWriter out = new PrintWriter(file);
            out.write(result);
            out.close();
        } catch (IOException e) {
            System.out.println("Wasn't able to save csv");
        }
    }

    /**
     * Finds and sets the proper millimeter to pixel scale.
     */
    public void setConversionScale(){
        double mm = 76.2; //The grid is 3" by 3", which translates into about 76 mm.
        scaleX = mm/movie.getDimensions()[0];
        scaleY = mm/movie.getDimensions()[1];
    }

    /**
     * @return The distance between points a and b.
     */
    private double distance(Double[] a, Double[] b) {
        return Math.sqrt(Math.pow(((a[0] - b[0]) * scaleX), 2) + Math.pow(((a[1] - b[1]) * scaleY), 2));
    }

    /**
     * @return The total distance a given larva traveled during the video.
     */
    private double getTotalDistance(Larva larva){
        double sum = 0;
        for(int i = 0; i<larva.getPositionsSize()-1; i++){
            if(larva.getPosition(i) != null){
                for (int j = i + 1; j < larva.getPositionsSize(); j++) {
                    if(larva.getPosition(j) != null){
                        sum += distance(larva.getPosition(i), larva.getPosition(j));
                        break;
                    }
                }
            }
        }
        return sum;
    }

    /**
     * @return The average velocity a given larva traveled during the video.
     */
    private double getAverageVelocity(Larva larva){
        int frames = 0;
        for (int i = larva.getPositionsSize() - 1; i > 0 ; i--) {
            if(larva.getPosition(i) != null){
                frames = i;
                break;
            }
        }
        return getTotalDistance(larva) / frames;
    }

}