package com.leidos.glidepath.gps.simulated;

import com.leidos.glidepath.appcommon.DataElementHolder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CsvSimulatedUiMessageProducer
 *
 * Maintains a list of DataElementHolders that contains GPS data
 * Each getGpsData call retrieves the next lat/long data
 *
 * Loads CSV data from provided file (testdata/uiMessages.csv
 *
 */
public class SimulatedGpsProducer {

    private static Logger logger = LoggerFactory.getLogger(SimulatedGpsProducer.class);

    private String gpsFile;

    private List<DataElementHolder> gpsData = new ArrayList<DataElementHolder>();

    private int currentIndex = 0;
    private int maxIndex = 0;

    public SimulatedGpsProducer(String gpsFile)   {
        this.gpsFile = gpsFile;
    }

    public DataElementHolder getGpsData()   {

        DataElementHolder holder = new DataElementHolder();

        if (!gpsData.isEmpty())  {
            holder = gpsData.get(currentIndex);
            currentIndex += 1;
            if (currentIndex >= maxIndex)  {
                currentIndex = 0;
            }
        }

        return holder;
    }

    public void load()   {
        try  {
            File csvData = new File(gpsFile);
            CSVParser parser = CSVParser.parse(csvData, Charset.forName("UTF-8"), CSVFormat.RFC4180);
            for (CSVRecord csvRecord : parser) {
                double latitude = Double.parseDouble(csvRecord.get(0).trim());
                double longitude = Double.parseDouble(csvRecord.get(1).trim());

                DataElementHolder holder = new DataElementHolder();
                holder.setLatitude(latitude);
                holder.setLongitude(longitude);

                gpsData.add(holder);
            }
        }
        catch(Exception e)   {
            logger.error("Error loading CSV DviUIMessage data. ", e);
        }

        maxIndex = gpsData.size();
    }

    public int size()   {
        return maxIndex;
    }

    private int getMaxIndex()   {
        return maxIndex;
    }

}
