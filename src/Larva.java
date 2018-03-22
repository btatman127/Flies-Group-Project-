import java.util.ArrayList;

public class Larva {
	private ArrayList<Double[]> positions;

	public Larva(Double x_initial, Double y_initial){
		positions = new ArrayList<Double[]>();
		Double[] coordinates = {x_initial, y_initial};
		positions.add(coordinates);

		coordinates[0] += 200;
		coordinates[1] += 20;
		setNewPosition(coordinates[0], coordinates[1]);
		coordinates[0] += 90;
		coordinates[1] += 0;
		setNewPosition(coordinates[0], coordinates[1]);
		coordinates[0] += -120;
		coordinates[1] += 20;
		setNewPosition(coordinates[0], coordinates[1]);
	}

    public Double[] getPosition(int frame) {
        return positions.get(frame);
    }
	
    public void setNewPosition(Double x, Double y) {
		Double[] coordinates = {x, y};
		positions.add(coordinates);
    }


}
