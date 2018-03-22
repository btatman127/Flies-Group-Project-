import java.util.ArrayList;

public class Larva {
	private ArrayList<Double[]> positions;

	public Larva(Double x_initial, Double y_initial){
		positions = new ArrayList<Double[]>();
		Double[] coordinates = {x_initial, y_initial};
		positions.add(coordinates);
	}

    public Double[] getPosition(int frame) {
        return positions.get(frame);
    }

	public ArrayList<Double[]> getCoordinates() {
		return positions;
	}
	
    public void setNewPosition(Double x, Double y) {
		Double[] coordinates = {x, y};
		positions.add(coordinates);
    }


}
