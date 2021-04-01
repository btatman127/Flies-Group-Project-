import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CSVExportTest {
    @Test
    void getsCorrectZonesFileName() {
        File file = new File("/root/data.csv");
        File zonesFile = new File("/root/data-zones.csv");
        assertEquals(zonesFile.getAbsolutePath(), CSVExport.getZonesFileName(file).getAbsolutePath());
    }

    @Test
    void getsUnusualZoneFile() {
        File file = new File("/root/file");
        File zonesFile = new File("/root/file-zones.csv");
        assertEquals(zonesFile.getAbsolutePath(), CSVExport.getZonesFileName(file).getAbsolutePath());
    }

}