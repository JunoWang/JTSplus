package org.datasyslab.jts.utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.io.FileReader;

import com.vividsolutions.jts.io.WKTReader;
import org.datasyslab.jts.utils.DistanceComparator;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

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
     
    public static void main(String[] args) throws IOException, CsvException {
        String fileName = "/Users/cwang/Desktop/glin_dataset/TIGER_2015_AREALM.csv";
        List<Geometry> testPolys = new ArrayList<Geometry>();
        testPolys = readRecord(fileName);
        assert !testPolys.isEmpty();
        System.out.println("the size of converted polygon is " + testPolys.size());
    }
}
