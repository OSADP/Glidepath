package com.leidos.glidepath.ead;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.Lane;
import com.leidos.glidepath.asd.Location;
import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.gps.simulated.SimulatedGpsProducer;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class LaneTester {

    @Autowired
    ApplicationContext applicationContext;

    SimulatedGpsProducer gps = new SimulatedGpsProducer("testdata/gpsLiveSaxton20150128.csv");

    private static ILogger logger = LoggerManager.getLogger(LaneTester.class);
    private boolean perfCheck;

    private static final byte[] MAP_MESSAGE = {
            (byte) 0x87,0x01, 0x01, (byte) 0x9d, 0x01, 0x01, 0x0c, 0x02,
            0x04, 0x00, 0x00, 0x07, 0x6d, 0x03, 0x08, 0x17, 0x38, 0x0e, (byte) 0x98, (byte) 0xd2, 0x03, (byte) 0xf1, (byte) 0xa2, 0x04,
            0x01, 0x01, 0x05, 0x02, 0x0c, 0x01, 0x06, 0x02, 0x00, 0x2e, 0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x58,
            0x02, 0x75, (byte) 0xdd, (byte) 0xfa, 0x21, (byte) 0xcf, (byte) 0xac, 0x2f, 0x7f, (byte) 0xdb, 0x34, 0x00, 0x16, 0x70, (byte) 0xf0, 0x52,
            0x6f, 0x70, 0x3b, 0x70, 0x40, 0x69, 0x71, 0x30, (byte) 0xbe, 0x6d, (byte) 0x91, 0x22, 0x60, 0x01, 0x34, 0x65,
            (byte) 0xe1, 0x46, 0x37, 0x40, (byte) 0xb9, 0x39, (byte) 0xb0, (byte) 0xce, 0x3b, 0x21, 0x0c, 0x33, 0x41, (byte) 0xea, 0x32, 0x11,
            (byte) 0xf1, 0x27, 0x52, 0x22, 0x1e, (byte) 0xa2, 0x5f, 0x16, 0x22, (byte) 0xbf, 0x11, (byte) 0x92, (byte) 0xf0, 0x0f, (byte) 0xb3, 0x18,
            0x1e, (byte) 0xe6, (byte) 0x88, 0x22, (byte) 0x86, (byte) 0xf9, 0x24, (byte) 0x97, 0x4f, 0x23, (byte) 0xd7, 0x64, 0x23, (byte) 0xb7, (byte) 0xaa, 0x23,
            0x07, (byte) 0x98, 0x1f, (byte) 0xe7, 0x00, 0x1a, (byte) 0x85, 0x45, 0x05, 0x02, 0x16, 0x01, 0x06, 0x02, 0x00, 0x22,
            0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x43, 0x02, (byte) 0xe7, (byte) 0xc1, (byte) 0x98, (byte) 0xf2, 0x10, 0x55, (byte) 0xef, 0x40, 0x6b,
            (byte) 0xe7, 0x10, (byte) 0xa9, (byte) 0xdf, 0x00, (byte) 0xcb, (byte) 0xd7, (byte) 0xa1, 0x08, (byte) 0xcf, 0x11, 0x4f, (byte) 0xca, 0x51, (byte) 0x8d,(byte) 0xc6,
            (byte) 0xc1, (byte) 0xeb, (byte) 0xc6, (byte) 0xc2, (byte) 0x83, (byte) 0xcc, (byte) 0xc2, (byte) 0xc9, (byte) 0xd0, 0x62, (byte) 0xf9, (byte) 0xd4, 0x33, 0x2b, (byte) 0xd7, (byte) 0x83,
            0x64, (byte) 0xda, (byte) 0x83, 0x5a, (byte) 0xd8, (byte) 0xb2, (byte) 0xfe, (byte) 0xd2, (byte) 0x82, (byte) 0x94, (byte) 0xce, (byte) 0x91, (byte) 0xf4, 0x34, 0x31, 0x51,
            (byte) 0xcb, 0x00, (byte) 0xaf, (byte) 0xce, 0x10, 0x44, (byte) 0xd7, 0x20, 0x3c, 0x05, 0x02, 0x0d, 0x01, 0x06, 0x02, 0x00,
            0x2a, 0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x22, 0x02, (byte) 0xf0, (byte) 0xfc, 0x45, (byte) 0xfb, (byte) 0xdf, (byte) 0x9a, (byte) 0xf9, 0x1f,
            0x37, (byte) 0xec, (byte) 0xed, (byte) 0xfc, (byte) 0xe6, 0x2d, 0x2f, (byte) 0xec, 0x4d, 0x33, (byte) 0xff, 0x5d, 0x36, 0x14, 0x1d, 0x5c,
            0x28, 0x4e, 0x01, 0x37, 0x7e, (byte) 0xa6, 0x40, (byte) 0xde, (byte) 0xf5, 0x05, 0x02, 0x0e, 0x01, 0x06, 0x02, 0x00,
            0x24, 0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x13, 0x02, (byte) 0xe2, (byte) 0xcc, (byte) 0xba, (byte) 0xf9, (byte) 0xaf, 0x3d, (byte) 0xf6, 0x7e,
            (byte) 0xdc, (byte) 0xf2, 0x4e, 0x32, (byte) 0xed, 0x3d, (byte) 0xa8, (byte) 0xf0, (byte) 0x9d, 0x1d, 0x05, 0x02, 0x0f, 0x01, 0x06, 0x02,
            0x00, 0x2e, 0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x49, 0x02, (byte) 0x9d, 0x71, (byte) 0xdd, (byte) 0xfb, (byte) 0xd0, 0x17, (byte) 0xf3,
            0x70, 0x6f, (byte) 0xe9, (byte) 0xe0, (byte) 0x9a, (byte) 0xd9, 0x11, 0x03, (byte) 0xd5, (byte) 0x81, 0x33, (byte) 0xd3, (byte) 0x91, 0x5c, (byte) 0xd1, (byte) 0xb1,
            (byte) 0x97, (byte) 0xd0, 0x61, (byte) 0xcb, (byte) 0xd6, 0x42, 0x0f, (byte) 0xcf, (byte) 0x92, (byte) 0x83, (byte) 0xd1, 0x22, (byte) 0xf0, (byte) 0xd5, (byte) 0x83, 0x06,
            (byte) 0xd8, (byte) 0x83, 0x3b, (byte) 0xda, 0x53, 0x54, (byte) 0xda, (byte) 0xd3, 0x18, (byte) 0xd4, 0x42, (byte) 0x85, (byte) 0xcc, 0x61, (byte) 0xe6, (byte) 0xca,
            0x41, 0x36, (byte) 0xca, (byte) 0xe0, 0x77, (byte) 0xcd, (byte) 0x8f, (byte) 0xe2, (byte) 0xd1, 0x6f, (byte) 0x9e, (byte) 0xda, (byte) 0xbf, (byte) 0xb3, (byte) 0xe2, 0x50,
            0x79, 0x05, 0x02, 0x0b, 0x01, 0x06, 0x02, 0x00, 0x2e, 0x08, 0x02, 0x01, (byte) 0x90, 0x09, 0x19, 0x02,
            0x22, (byte) 0xf6, (byte) 0x8c, 0x06, 0x60, (byte) 0xbb, 0x0b, 0x11, 0x53, 0x10, (byte) 0xf2, 0x19, 0x12, 0x42, (byte) 0x99, 0x11,
            0x42, (byte) 0xba, 0x0f, 0x32, (byte) 0xa6, 0x0c, 0x41, (byte) 0xfd, (byte) 0xff
    };

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		perfCheck = Boolean.valueOf(config.getProperty("performancechecks"));
    }

    @After
    public void shutdown() {
        try {
            LoggerManager.setOutputFile("logs/LaneTester.txt");
            LoggerManager.setRecordData(true);
            LoggerManager.writeToDisk();
        }catch (Exception e) {
            //do nothing for now
        }
    }


    @Test
    public void testMapMessageParse() throws Exception   {
        MapMessage mapMessage = new MapMessage();

        mapMessage.parse(MAP_MESSAGE);

        Intersection intersection = new Intersection(perfCheck);
        intersection.initialize(mapMessage);

        //writeNodeCsv(mapMessage);

        gps.load();

        for (int i=0; i<gps.size() - 1; i++)   {
            DataElementHolder gpsHolder = gps.getGpsData();

            boolean result = intersection.computeGeometry(gpsHolder.getLatitude(), gpsHolder.getLongitude());

            logger.debug("", "Loop " + i + ": " + result);
        }

        logger.debug("", "Done.");
    }


    /**
     * Just used once to write out the node locs for Lane 12
     * @param mapMessage
     */
    private void writeNodeCsv(MapMessage mapMessage)    {
        String NEW_LINE_SEPARATOR = "\n";

        Lane lane12 = mapMessage.getLane(0);

        Location[] nodes = lane12.getNodes();

        FileWriter fileWriter = null;
        CSVPrinter csvFilePrinter = null;
        //Create the CSVFormat object with "\n" as a record delimiter
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
        try {
            //initialize FileWriter object
            fileWriter = new FileWriter("testdata/gpsLane12Nodes.csv");

            //initialize CSVPrinter object
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

            for (Location loc : nodes)   {
                List record = new ArrayList();

                record.add(loc.lat());
                record.add(loc.lon());

                csvFilePrinter.printRecord(record);
            }

        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                csvFilePrinter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
                e.printStackTrace();
            }
        }
    }


}
