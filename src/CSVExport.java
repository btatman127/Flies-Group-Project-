import java.io.File;
import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;


public class CSVExport {
    private final int frames;
    private double scaleX;
    private double scaleY;
    private final Video movie;
    private final List<Larva> larvae;
    private final double zoneRadius;

    public CSVExport(Video movie, int frames, double zoneRadius) {
        this.frames = frames;
        this.movie = movie;
        this.zoneRadius = zoneRadius;
        larvae = movie.getLarva();

        setConversionScale();
    }

    /**
     * Finds number of larvae spends certain portion of time in a zone.
     * @param larvaInZone 2D array with what zone each larvae is in at each frame
     */
    private String getTimeInZoneString(List<Larva> larvae, int[][] larvaInZone) {
        double[][] timeInZone = new double[larvae.size()][];
        for (int i = 0; i < larvaInZone.length; i++) {
            timeInZone[i] = portionOfTimeInZone(larvaInZone[i]);
        }
        int[] anyTimeInZone = numberOfLarvaeAboveTimeThreshold(timeInZone, 0.001);
        int[] halfTimeInZone = numberOfLarvaeAboveTimeThreshold(timeInZone, 0.5);
        int[] allTimeInZone = numberOfLarvaeAboveTimeThreshold(timeInZone, 1.0);

        return buildTimeInZoneTable(larvae, anyTimeInZone, halfTimeInZone, allTimeInZone);
    }

    /**
     * Finds the number of larvae that spent a greater portion of time in a zone than the threshold.
     * @param timeInZone portion of time each larvae spent in each zone
     */
    private int[] numberOfLarvaeAboveTimeThreshold(double[][] timeInZone, double threshold){
        int[] portionOfTimeInZone = new int[12];
        for (int i = 0; i < 12; i++) {
            for(double[] portionOfTime : timeInZone) {
                if (portionOfTime[i] >= threshold) {
                    portionOfTimeInZone[i]++;
                }
            }
        }
        return portionOfTimeInZone;
    }

    private String buildTimeInZoneTable(List<Larva> larvae, int[] anyTimeInZone, int[] halfTimeInZone, int[] allTimeInZone) {
        StringBuilder sb = new StringBuilder();
        sb.append("How many larva spent time in each zone?\n");
        sb.append(", Any amount,,At least half their time,,All their time,\n");
        sb.append(",count, proportion, count, proportion, count proportion\n");
        for (int i = 0; i < 12; i++) {
            sb.append("Zone ");
            sb.append(i + 1);
            sb.append(",");
            sb.append(anyTimeInZone[i]);
            sb.append(",");
            sb.append(String.format("%.2f", 1.0* anyTimeInZone[i]/ larvae.size()));
            sb.append(",");
            sb.append(halfTimeInZone[i]);
            sb.append(",");
            sb.append(String.format("%.2f", 1.0* halfTimeInZone[i]/ larvae.size()));
            sb.append(",");
            sb.append(allTimeInZone[i]);
            sb.append(",");
            sb.append(String.format("%.2f", 1.0* allTimeInZone[i]/ larvae.size()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getFurthestZoneString(List<Larva> larvae, int[] furthestZone) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total number of Zones Occupied, Raw # of Larva, Proportion of Larva");
        sb.append("\n");

        int[] numLarvaFurthestZone = new int[12];
        for (int i = 0; i < larvae.size(); i++) {
            numLarvaFurthestZone[furthestZone[i]]++;
        }
        for (int i = 0; i < 12; i++) {
            sb.append(i + 1);
            sb.append(",");
            sb.append(numLarvaFurthestZone[i]);
            sb.append(",");
            sb.append(String.format("%.2f", 1.0*numLarvaFurthestZone[i]/ larvae.size()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getAverageVelocityString(List<Larva> larvae) {
        StringBuilder sb = new StringBuilder();
        //add average velocity
        for (Larva larva : larvae) {
            sb.append("Average Velocity (mm/sec): ,");
            sb.append(String.format("%.2f", getAverageVelocityString(larva)));
            sb.append(",");
        }
        return sb.toString();
    }

    private String getTotalDistanceString(List<Larva> larvae) {
        StringBuilder sb = new StringBuilder();
        //add total distance
        for (Larva larva : larvae) {
            sb.append("Total Distance (mm): ,");
            sb.append(String.format("%.2f", getTotalDistance(larva)));
            sb.append(",");
        }
        return sb.toString();
    }

    /**
     * Returns the furthest zone travelled for each larva.
     * @param larvaInZone 2D array with what zone each larvae is in at each frame
     */
    private int[] getFurthestZone(int frames, List<Larva> larvae, int[][] larvaInZone) {
        int[] furthestZone = new int[larvae.size()];
        for (int i = 0; i < larvae.size(); i++) {
            furthestZone[i] = 0;
            for (int j = 0; j < frames; j++) {
                if(larvaInZone[i][j] > furthestZone[i]){
                    furthestZone[i] = larvaInZone[i][j];
                }
            }
        }
        return furthestZone;
    }

    /**
     * Makes a string that contains which x & y coordinate and zone each larva is for each frame.
     * @param zoneRadius radius in mm for each zone
     * @param larvaInZone 2D array with what zone each larvae is in at each frame
     */
    private String getFrameDataString(double zoneRadius, List<Larva> larvae, int[][] larvaInZone) {
        StringBuilder sb = new StringBuilder();
        sb.append(addColumnLabels(larvae));

        //add data
        sb.append("\n");
        for (int row = 0; row < frames; row++) {
            for (int coord = 0; coord < larvae.size(); coord++) {
                String x = "";
                String y = "";
                String zoneString = "";
                int zone;

                Double[] startPosition = larvae.get(coord).getPosition(0);
                if (row < larvae.get(coord).getPositionsSize()) {
                    //Get pixel x and y positions and convert them into mm. Convert (0,0) from top left to bottom left.
                    Double[] currentPosition = larvae.get(coord).getPosition(row);
                    if(currentPosition != null) {
                        x = String.format("%.2f", (currentPosition[0] * scaleX));
                        y = String.format("%.2f", ((movie.getDimensions()[1] - currentPosition[1]) * scaleY));

                        zone = findZone(startPosition, currentPosition, zoneRadius);
                        zoneString = String.valueOf(zone + 1);
                        larvaInZone[coord][row] = zone;

                    }
                    else{
                        larvaInZone[coord][row] = -1;
                    }
                }
                String frameData = x + "," + y + ",";
                sb.append(frameData);
                sb.append(zoneString);
                if (coord != larvae.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private StringBuilder addColumnLabels(List<Larva> larvae) {
        StringBuilder sb = new StringBuilder();
        //add column labels for each larva
        for (int i = 0; i < larvae.size(); i++) {
            String larvaName = "larva" + (i + 1);
            sb.append(larvaName);
            if (i != larvae.size() - 1) {
                sb.append(",,,");
            }
        }

        //add column labels for x and y
        sb.append("\n");
        for (int i = 0; i < larvae.size(); i++) {
            sb.append("x (mm),y (mm), zone,");
        }
        return sb;
    }

    private int findZone(Double[] startingPosition, Double[] currentPosition, double zoneRadius){
        double distance = distance(startingPosition, currentPosition);
        return (int) (distance/zoneRadius);
    }

    private double[] portionOfTimeInZone(int[] zoneAtFrame){
        double[] timeInZone = new double[12];
        int framesTracked = 0;
        for (int zone : zoneAtFrame) {
            if(zone != -1){
                framesTracked++;
                timeInZone[zone]++;
            }
        }
        for (int i = 0; i < 12; i++) {
            timeInZone[i] /= framesTracked;
        }
        return timeInZone;
    }

    /**
     * Exports and saves a CSV file containing position, distance, and velocity data for each larva.
     */
    public void export(File file) {
        StringBuilder sb = new StringBuilder();
        int[][] larvaInZone = new int [larvae.size()][frames];
        sb.append(getFrameDataString(zoneRadius, larvae, larvaInZone));

        int[] furthestZone = getFurthestZone(frames, larvae, larvaInZone);

        sb.append("\n");
        sb.append(getTotalDistanceString(larvae));

        sb.append("\n");
        sb.append(getAverageVelocityString(larvae));

        sb.append("\n");
        sb.append("\n");

        sb.append(getFurthestZoneString(larvae, furthestZone));

        sb.append("\n");
        sb.append(getTimeInZoneString(larvae, larvaInZone));

        String result = sb.toString();
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
    private double getAverageVelocityString(Larva larva){
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