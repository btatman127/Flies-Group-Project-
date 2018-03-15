import java.util.ArrayList;

public class Larva {
	private ArrayList<Double[]> positions;

	public Larva(Double y_initial, Double x_initial){
		positions = new ArrayList<Double[]>();
		Double[] coordinates = {y_initial, x_initial};
		positions.add(coordinates);
	}

    public Double[] getPosition(int frame) {
        return positions.get(frame);
    }
	
    public void setNewPosition(Double y, Double x) {
		Double[] coordinates = {y, x};
		positions.add(coordinates);
    }


}
