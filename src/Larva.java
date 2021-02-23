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
		if(coords == null) {
			positions.add(null);
		}
		else {
			positions.add(new Double[]{coords[0], coords[1]});
		}
    }

    public int getPositionsSize(){ return positions.size(); }

	/**
	 * Removes all frames from given frame to the end.
	 */

	public void trimPositions(int frame){
		while(positions.size() > frame){
			positions.remove(positions.size()-1);
		}
	}
}
