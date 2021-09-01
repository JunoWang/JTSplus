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


    public static List<Geometry> readRecord(String fileName)throws IOException, CsvException {
        List<Geometry> testPolys = new ArrayList<Geometry>();
        try {
            // Array to put all polygon

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
            // we are going to read data line by line
            while ((nextRecord = csvReader.readNext()) != null) {
                assert nextRecord[0] != " ";
//                System.out.println("arrays[0] is " + nextRecord[0]);
                WKTReader wkt = new WKTReader();
                Geometry geom = wkt.read(nextRecord[0]);
                testPolys.add(geom);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return testPolys;
    }
    /*
    create one file that contains 1%,0.1%,0.01%,0.001% selectivity query window
    use top 100 neighbor
     */
    public static void genQueryWindow(List<Geometry> testPolys, String fileName){

        //1% of selectivity
        int totalRecords_op = (int) (testPolys.size() * 0.01);
        //0.1%
        int totalRecords_zop = (int) (testPolys.size() * 0.001);
        //0.01%
        int totalRecords_zzop =(int) (testPolys.size() * 0.0001);
        //0.001%
        int totalRecords_zzzop = (int) (testPolys.size() * 0.00001);

        writeFile(testPolys,fileName,totalRecords_op,"1%");


    }
    public static Envelope genSelecTopK(int seleStart, int seleEnd, List<Geometry> testPolys, Point queryCenter, int topK){
        STRtree strtree = new STRtree();
        for(int i = seleStart ; i < seleEnd; i++){
            strtree.insert(testPolys.get(i).getEnvelopeInternal(), testPolys.get(i));
        }
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
    public static void writeFile(List<Geometry> testPolys, String fileName, int selecTopK, String selectivity){
        try {
            String outputFileName = fileName + "_" + selectivity+".txt";
            FileWriter myWriter = new FileWriter(outputFileName,true);
            for (int i = 0; i < 100; i++) {
                //create STRtree
                int valueRange = 10000;
                Random random = new Random();
                // random a query center
                GeometryFactory geometryFactory = new GeometryFactory();
                Coordinate coordinate = new Coordinate(-1000+random.nextInt(valueRange)*7.1,random.nextInt(valueRange)*(-50.1));
                Point queryCenter = geometryFactory.createPoint(coordinate);
                Envelope topKMbr_op = genSelecTopK(0, testPolys.size(), testPolys,queryCenter,selecTopK);
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
        List<Geometry> testPolys = new ArrayList<Geometry>();
        testPolys = readRecord(fileName);
        assert !testPolys.isEmpty();
        System.out.println("the size of converted polygon is " + testPolys.size());
        genQueryWindow(testPolys,fileName);
    }
}
