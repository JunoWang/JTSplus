package org.datasyslab.jts.utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sun.tools.doclint.Env;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.WKTWriter;

public class gen_query_window {


    public static void readRecord(String fileName, int interval, double selectivity) throws IOException, CsvException {
        STRtree strtree = new STRtree();
        try {
            // Create an object of file reader class with CSV file as a parameter.
            FileReader filereader = new FileReader(fileName);
            // create csvParser object with
            // custom separator tab
            CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
            // create csvReader object with parameter
            // filereader and parser
            CSVReader csvReader = new CSVReaderBuilder(filereader)
                    .withCSVParser(parser)
                    .build();
            String[] nextRecord;
            int listIndex = 0;
            List<Geometry> points = new ArrayList<Geometry>();
            if(fileName.contains("wkt") || fileName.contains("csv")){
                while ((nextRecord = csvReader.readNext()) != null) {
//                System.out.println(nextRecord);
                    assert nextRecord[0] != "";
//                nextRecord[1].replace("\"", "" );
                    WKTReader wkt = new WKTReader();
                    Geometry geom = wkt.read(nextRecord[0]);
                    if (geom != null) {
                        strtree.insert(geom.getEnvelopeInternal(), geom);
                        //get center point instead of counting col
                        //coordinate needs consistency
                        //in polygon they are -80, xxx, 32 xxx, then the query center also should be -xxx, xxx
                        if (listIndex % interval == 0) {
                            points.add(geom.getCentroid());
                        }
                        listIndex++;
//                    System.out.println(listIndex);
                    } else {
                        listIndex++;
                        continue;
                    }
                }
            }else{
                while ((nextRecord = csvReader.readNext()) != null) {
//                System.out.println(nextRecord);
                    assert nextRecord[1] != "";
//                nextRecord[1].replace("\"", "" );
                    WKTReader wkt = new WKTReader();
                    Geometry geom = wkt.read(nextRecord[1]);
                    if (geom != null) {
                        strtree.insert(geom.getEnvelopeInternal(), geom);
                        //get center point instead of counting col
                        //coordinate needs consistency
                        //in polygon they are -80, xxx, 32 xxx, then the query center also should be -xxx, xxx
                        if (listIndex % interval == 0) {
                            points.add(geom.getCentroid());
                        }
                        listIndex++;
//                    System.out.println(listIndex);
                    } else {
                        listIndex++;
                        continue;
                    }
                }
            }

            // we are going to read data line by line


            //1% of selectivity
            int totalRecords_op = (int) (listIndex * selectivity);

            writeFile(strtree, fileName, totalRecords_op, Double.toString(selectivity), points);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
    create one file that contains 1%,0.1%,0.01%,0.001% selectivity query window
    use top 100 neighbor
     */

    public static Envelope genSelecTopK(STRtree strtree, Geometry queryCenter, int topK) {
        Object[] testTopK_op = strtree.kNearestNeighbour(queryCenter.getEnvelopeInternal(), queryCenter, new GeometryItemDistance(), topK);
        List topKList_op = Arrays.asList(testTopK_op);
        assert !topKList_op.isEmpty();
        assert topKList_op.size() == topK;
        //  System.out.println("first one in topK list "+topKList_op.get(0));
        Envelope topKMbr = new Envelope();
        for (Object o : topKList_op) {
            Geometry geometry = (Geometry) o;
            topKMbr.expandToInclude(geometry.getEnvelopeInternal());
        }
        return topKMbr;
    }

    public static void writeFile(STRtree strtree, String fileName, int selecTopK, String selectivity, List<Geometry> points) {
        try {
            String outputFileName = fileName + "_" + selectivity + ".txt";
            FileWriter myWriter = new FileWriter(outputFileName, false);
            for (int i = 0; i < points.size(); i++) {
                //create STRtree
                GeometryFactory geometryFactory = new GeometryFactory();
                Envelope topKMbr_op = genSelecTopK(strtree, points.get(i), selecTopK);
                Geometry mbr = geometryFactory.toGeometry(topKMbr_op);
                String mbrWkt = new WKTWriter().writeFormatted(mbr);
                myWriter.write(mbrWkt + "\n");
            }
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException, CsvException {
//        String fileName = "src/main/java/org/datasyslab/jts/utils/TIGER_2015_AREALM_10000.csv";
        //"/Users/cwang/Desktop/glin_dataset/uniform_2gb.wkt";
        String fileName = args[0];
        int interval = Integer.parseInt(args[1]);

        /*
        number of records for each data
        2292766     TIGER_2015_AREAWATER.csv
        19592693    TIGER_2015_ROADS.csv
        180958      TIGER_2015_RAILS.csv
        5838339     TIGER_2015_LINEARWATER.csv
        129179      TIGER_2015_AREALM.csv
        9961896     parks
        40000000    diagonal_8gb
        10000000    uniform_2gb.wkt
        40000000    uniform_8gb.wkt
        10000000    diagonal_2gb.wkt

         */

        /*
        this function write 100 query window with certain selectivity
        the interval decides row intervals when get a query center
        selectivity is 1%: 0.01, 0.1% = 0.001, 0.01% = 0.0001, 0.001% = 0.00001
         */

        readRecord(fileName, interval, 0.01);
        readRecord(fileName, interval, 0.001);
        readRecord(fileName, interval, 0.0001);
        readRecord(fileName, interval, 0.00001);
    }
}
