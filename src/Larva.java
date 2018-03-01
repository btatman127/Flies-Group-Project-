public class Larva {
    private double[][] position;
    private int time;

    public Larva( double x_initial, double y_initial){

        position = new double[2][1000];
        position[0][0] = x_initial;
        position[1][0] = y_initial;
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
