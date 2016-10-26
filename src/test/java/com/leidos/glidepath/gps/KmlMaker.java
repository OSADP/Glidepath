package com.leidos.glidepath.gps;

import com.leidos.glidepath.appcommon.DataElementHolder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to make a kml file from a CSV file containing lat, lon
 */
public class KmlMaker {

    private static final String NL = "\n";


    @Test
    public void makeKmlFromCsvLatLon() throws Exception   {

        List<DataElementHolder> holders = loadCsv("testdata/gpsLiveSaxton20150128.csv");

        StringBuffer sb = new StringBuffer();

        kmlHeader(sb);

        for (DataElementHolder holder : holders)   {
            kmlCoords(sb, holder.getLongitude(), holder.getLatitude());
        }

        kmlFooter(sb);

        writeToTextFile("testdata/kml/gpsLiveSaxton20150128.kml", sb.toString());

    }


    public List<DataElementHolder> loadCsv(String gpsFile)   {
        List<DataElementHolder> gpsData = new ArrayList<DataElementHolder>();

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
            System.out.println("Error loading CSV DviUIMessage data. " + e.getMessage());
        }

        return gpsData;
    }

    private StringBuffer kmlHeader(StringBuffer sb)   {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL);

        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">" + NL);
        sb.append("<Document>" + NL);

        return sb;
    }

    private StringBuffer kmlCoords(StringBuffer sb, double lon, double lat)   {

        sb.append("<Placemark>" + NL);
        sb.append("  <Point>" + NL);
        sb.append("    <coordinates>" + lon + "," + lat + ",0</coordinates>" + NL);
        sb.append("  </Point>" + NL);
        sb.append("</Placemark>" + NL);

        return sb;
    }

    private StringBuffer kmlFooter(StringBuffer sb)   {
        sb.append("</Document>" + NL);
        sb.append("</kml>");

        return sb;
    }

    private void writeToTextFile(String fileName, String content) throws IOException {
        Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
    }

}
