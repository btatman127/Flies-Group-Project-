import java.util.ArrayList;

public class Larva {
	private ArrayList<Double[]> positions;

	public Larva(Double xInitial, Double yInitial){
		positions = new ArrayList<Double[]>();
		Double[] coordinates = {xInitial, yInitial};
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

    public double getDisplacement(int frame){
		double x = positions.get(frame)[0] - positions.get(0)[0];
		double y = positions.get(frame)[1] - positions.get(0)[1];
		return Math.sqrt(x*x+y*y);
	}

	/**
	 * Removes all frames from given frame to the end.
	 */

	public void trimPositions(int frame){
		while(positions.size() > frame){
			positions.remove(positions.size()-1);
		}
	}
}
