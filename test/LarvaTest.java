import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class LarvaTest {
    @Test
    public void initialLarva(){
        Larva larva = new Larva(1.0, 2.0);
        assertArrayEquals(new Double[]{1.0, 2.0}, larva.getPosition(0));
    }

    @Test
    public void multiplePositions() {
        Larva larva = new Larva(1.0, 2.0);
        larva.setNewPosition(new Double[]{3.0, 5.0});
        larva.setNewPosition(new Double[]{4.0, 3.0});
        ArrayList<Double[]> expectedArray = new ArrayList<Double[]>();
        expectedArray.add(new Double[]{1.0, 2.0});
        expectedArray.add(new Double[]{3.0, 5.0});
        expectedArray.add(new Double[]{4.0, 3.0});
        assertEquals(3, larva.getPositionsSize());
        for (int i = 0; i < larva.getPositionsSize(); i++) {
            assertArrayEquals(expectedArray.get(i), larva.getCoordinates().get(i));
        }
    }

    @Test
    public void removesPosition(){
        Larva larva = new Larva(1.0, 2.0);
        larva.setNewPosition(new Double[]{3.0, 5.0});
        larva.setNewPosition(new Double[]{4.0, 3.0});
        larva.setNewPosition(new Double[]{6.0, 4.0});
        larva.setNewPosition(new Double[]{4.0, 3.0});
        larva.trimPositions(1);
        ArrayList<Double[]> expectedArray = new ArrayList<Double[]>();
        expectedArray.add(new Double[]{1.0, 2.0});
        assertEquals(1, larva.getPositionsSize());
        assertArrayEquals(expectedArray.get(0), larva.getCoordinates().get(0));


    }

}