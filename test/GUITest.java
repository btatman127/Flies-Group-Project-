import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

public class GUITest {
    @Test
    void deleteDirectoryWorks(){
        String dirName = "testDir";
        File dir = new File(dirName);
        dir.mkdir();
        for (int i = 0; i < 10; i++) {
            try {
                new File(dirName + "/" + i + ".txt").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GUI g =  new GUI();
        g.deleteDirectory(dir);

        assertFalse(dir.isDirectory());
    }
}
