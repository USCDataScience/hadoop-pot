package gov.nasa.jpl.memex.pooledtimeseries;

import java.io.IOException;
import java.util.*;
import java.io.*;
//import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.mapred.lib.*;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class SimilarityCalculation {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            //output.collect(value, new Text(ofVector));
            System.out.println(value.toString());

            /*
             * Split the value string to get both video paths
             * For first video
             *      loadTimeSeries() for both cached files
             *      Generate feature vectors for this video
             *
             * Pass computed feature vectors to similarity function
             * Capture similarity value from matrix as output value. Key could
             * be comma-separated hash of file paths
             *
             * Post Hadoop run we could combine the outputs together in another
             * script to generate the complete matrix
             */

            String[] videoPaths = value.toString().split(",");
            ArrayList<double[]> tws = PoT.getTemporalWindows(4);
            ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();
            

            for (String video: videoPaths) {
                ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();

                String ofCachePath = video + ".of.txt";
                String hogCachePath = video + ".hog.txt";

                multiSeries.add(PoT.loadTimeSeries(new File(ofCachePath).toPath()));
                multiSeries.add(PoT.loadTimeSeries(new File(hogCachePath).toPath()));

                FeatureVector fv = new FeatureVector();
                for (int i = 0; i < multiSeries.size(); i++) {
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 1));
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 2));
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 5));
                }
                fvList.add(fv);
            }

            double[][] similarities = PoT.calculateSimilarities(fvList);
            String newValue = DigestUtils.sha256Hex(videoPaths[0]).toUpperCase() + ',' + DigestUtils.sha256Hex(videoPaths[1]).toUpperCase();
            output.collect(new Text(newValue), new Text(String.valueOf(similarities[0][1])));
        }
    }


    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Configuration baseConf = new Configuration();
        baseConf.set("mapred.reduce.tasks", "0");
        JobConf conf = new JobConf(baseConf, SimilarityCalculation.class);

        conf.setJobName("similarity_calc");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Map.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}


