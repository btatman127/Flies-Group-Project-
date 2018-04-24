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
	
    public void setNewPosition(Double[] coords) {
		positions.add(coords);
    }

    public int getPositionsSize(){ return positions.size(); }

	/**
	 * useful for overwriting larva positions
	 * delete all indeces including and after the index of value frame
	 * @param frame
	 */

	public void trimPositions(int frame){
		ArrayList<Double[]> tempPositions = new ArrayList<Double[]>();
		for(int f = 0; f < frame; f++){
			tempPositions.add(positions.get(f));
		}
		positions = tempPositions;
	}


}
