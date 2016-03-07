package gov.nasa.jpl.memex.pooledtimeseries;

import java.io.IOException;
import java.util.*;
import java.io.*;
//import java.nio.file.Path;

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

public class GradientTimeSeries {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            //output.collect(value, value);

            try {
                double[][] series1 = PoT.getGradientTimeSeries(new File(value.toString()).toPath(), 5, 5, 8);
                String ofVector = saveVectors(series1);
                //System.out.println(ofVector);
                output.collect(value, new Text(ofVector));
            } catch (Exception e) {}
        }

        private static String saveVectors(double[][] vectors) {
            int d = vectors[0].length;

            ArrayList<double[][][]> temp_hists = new ArrayList<double[][][]>();

            for (int i = 0; i < vectors.length; i++) {
              double[][][] temp_hist = new double[1][1][d];
              temp_hist[0][0] = vectors[i];

              temp_hists.add(temp_hist);
            }

            return getSaveHistogramsOutput(temp_hists);
        }

        private static String getSaveHistogramsOutput(ArrayList<double[][][]> hists) {
            int w_d = hists.get(0).length;
            int h_d = hists.get(0)[0].length;
            int o_d = hists.get(0)[0][0].length;

            int i, j, k, l;

            StringWriter writer = new StringWriter();
            String head = String.format("%d %d", hists.size(), w_d * h_d * o_d);
            writer.write(head);
            writer.write("\n");

            for (l = 0; l < (int) hists.size(); l++) {
                double[][][] hist = hists.get(l);

                for (i = 0; i < hist.length; i++) {
                    for (j = 0; j < hist[0].length; j++) {
                        for (k = 0; k < hist[0][0].length; k++) { // optical_bins+1
                            writer.write(String.format("%f ", hist[i][j][k]));
                        }
                    }
                }

                writer.write("\n");
            }

            return writer.toString();
        }
    }

    public static class MultiFileOutput extends MultipleTextOutputFormat<Text, Text> {
        protected String generateFileNameForKeyValue(Text key, Text value, String name) {
            String[] splitPath = key.toString().split("/");
            String fileName = splitPath[splitPath.length - 1];
            return fileName + ".hog.txt";
        }

        protected Text generateActualKey(Text key, Text value) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Configuration baseConf = new Configuration();
        baseConf.set("mapred.reduce.tasks", "0");
        JobConf conf = new JobConf(baseConf, GradientTimeSeries.class);

        conf.setJobName("gradient_time_series");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Map.class);
        //conf.setCombinerClass(Reduce.class);
        //conf.setReducerClass(Reduce.class);

        conf.setInputFormat(TextInputFormat.class);
        //conf.setOutputFormat(TextOutputFormat.class);
        conf.setOutputFormat(MultiFileOutput.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}

