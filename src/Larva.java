public class Larva {
    private double[][] position;
    private int time;

    public Larva( double x_initial, double y_initial){

        position = new double[1000][2];
        position[0][0] = x_initial;
        position[0][1] = y_initial;
        time = 0;
       
    }




    public double[][] getPosition() {
        return position;
    }


    public void setNewPosition(double x, double y) {
        position[0][time] = x;
        position[1][time] = y;
        time++;
    }


}
