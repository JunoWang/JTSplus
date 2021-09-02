package org.datasyslab.jts.utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.*;
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


    public static void readRecord(String fileName, int interval, double selectivity)throws IOException, CsvException {
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
            List<Point> points = new ArrayList<Point>();
            // we are going to read data line by line
            while ((nextRecord = csvReader.readNext()) != null) {
                assert nextRecord[0] != " ";
              //  System.out.println("arrays[7] is " + nextRecord[8]);
                WKTReader wkt = new WKTReader();
                Geometry geom = wkt.read(nextRecord[0]);
                strtree.insert(geom.getEnvelopeInternal(), geom);
                if(listIndex%interval == 0 && points.size()<100){
                    GeometryFactory geometryFactory = new GeometryFactory();
                    Coordinate coordinate = new Coordinate(Double.valueOf(nextRecord[8]),Double.valueOf(nextRecord[9]));
                    Point queryCenter = geometryFactory.createPoint(coordinate);
                    points.add(queryCenter);
                }
                listIndex++;
            }

            //1% of selectivity
            int totalRecords_op = (int) (listIndex * selectivity);
//            //0.1%
//            int totalRecords_zop = (int) (listIndex * 0.001);
//            //0.01%
//            int totalRecords_zzop = (int) (listIndex * 0.0001);
//            //0.001%
//            int totalRecords_zzzop = (int) (listIndex * 0.00001);



            writeFile(strtree,fileName,totalRecords_op,Double.toString(selectivity), points);

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
    create one file that contains 1%,0.1%,0.01%,0.001% selectivity query window
    use top 100 neighbor
     */

    public static Envelope genSelecTopK( STRtree strtree, Point queryCenter, int topK){
        Object[] testTopK_op = (Object[])strtree.kNearestNeighbour(queryCenter.getEnvelopeInternal(), queryCenter, new GeometryItemDistance(), topK);
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
    public static void writeFile(STRtree strtree, String fileName, int selecTopK, String selectivity, List<Point> points){
        try {
            String outputFileName = fileName + "_" + selectivity+".txt";
            FileWriter myWriter = new FileWriter(outputFileName,true);
            for (int i = 0; i < 100; i++) {
                //create STRtree
                GeometryFactory geometryFactory = new GeometryFactory();
                Envelope topKMbr_op = genSelecTopK(strtree,points.get(i),selecTopK);
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
        String fileName = "/Users/cwang/Desktop/glin_dataset/TIGER_2015_AREALM.csv";
        /*
        this function write 100 query window with certain selectivity
        the interval decides row intervals when get a query center
        selectivity is 1%: 0.01, 0.1% = 0.001, 0.01% = 0.0001, 0.001% = 0.00001
         */
        readRecord(fileName,1230,0.001);


    }
}
