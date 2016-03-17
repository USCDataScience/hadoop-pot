package gov.nasa.jpl.memex.pooledtimeseries;

import java.io.IOException;
import java.util.*;
import java.io.*;
//import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
//import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.mapred.lib.*;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class SimilarityCalculation {
    //public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
    public static class Map extends Mapper<LongWritable, Text, Text, Text> {
        //
        //public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException, NumberFormatException {
            //output.collect(value, new Text(ofVector));
            System.out.println(value.toString());
            Configuration conf = context.getConfiguration();
            String meanDistsPath = conf.get("meanDistsFilePath");

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

            double[] meanDists = new double[fvList.get(0).numDim()];
            BufferedReader inFile = new BufferedReader(new FileReader(meanDistsPath));
            String line;
            int counter = 0;
            while ((line = inFile.readLine()) != null) {
                meanDists[counter] = Double.parseDouble(line);
                System.out.println(line);
                counter++;
            }

            //double[][] similarities = PoT.calculateSimilarities(fvList);
            //System.out.println("------------");
            //System.out.println(similarities[0][0] + " " + similarities[0][1]);
            //System.out.println(similarities[1][0] + " " + similarities[1][1]);
            //System.out.println("------------");

            double similarity = PoT.kernelDistance(fvList.get(0), fvList.get(1), meanDists);

            // The below is for dumping the file paths as SHA256 hex strings. For now this is being defaulted
            // to the full file paths because the other results are rather difficult for a human to read. =)
            //String newValue = DigestUtils.sha256Hex(videoPaths[0]).toUpperCase() + ',' + DigestUtils.sha256Hex(videoPaths[1]).toUpperCase();
            //output.collect(new Text(newValue), new Text(String.valueOf(similarities[0][1])));
            //output.collect(new Text(newValue), new Text(String.valueOf(similarities[0][1])));
            
            File p1 = new File(videoPaths[0]);
            File p2 = new File(videoPaths[1]);
            //output.collect(new Text(p1.getName() + ',' + p2.getName()), new Text(String.valueOf(similarities[0][1])));
            context.write(new Text(p1.getName() + ',' + p2.getName()), new Text(String.valueOf(similarity)));

            //output.collect(value, new Text(String.valueOf(similarities[0][1])));
        }
    }


    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Configuration baseConf = new Configuration();
        baseConf.set("mapred.reduce.tasks", "0");
        baseConf.set("meanDistsFilePath", args[2]);
        //JobConf conf = new JobConf(baseConf, SimilarityCalculation.class);

        //conf.setJobName("similarity_calc");

        //conf.setOutputKeyClass(Text.class);
        //conf.setOutputValueClass(Text.class);

        //conf.setMapperClass(Map.class);

        //conf.setInputFormat(TextInputFormat.class);
        //conf.setOutputFormat(TextOutputFormat.class);

        //FileInputFormat.setInputPaths(conf, new Path(args[0]));
        //FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        //conf.set("meanDistsFilePath", args[2]);

        //JobClient.runJob(conf);

        // Create a new Job
        Job job = Job.getInstance(baseConf);
        job.setJarByClass(SimilarityCalculation.class);

        // Specify various job-specific parameters     
        job.setJobName("similarity_calc");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        //job.setInputPath(new Path(args[0]));
        //job.setOutputPath(new Path(args[1]));
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setMapperClass(Map.class);

        // Submit the job, then poll for progress until the job is complete
        job.waitForCompletion(true);

         //Job job = Job.getInstance(new Configuration());

    //job.setOutputKeyClass(Text.class);
    //job.setOutputValueClass(IntWritable.class);

    //job.setMapperClass(Map.class);
    //job.setReducerClass(Reduce.class);

    //job.setInputFormatClass(TextInputFormat.class);
    //job.setOutputFormatClass(TextOutputFormat.class);

    //FileInputFormat.setInputPaths(job, new Path(args[0]));
    //FileOutputFormat.setOutputPath(job, new Path(args[1]));

    //job.setJarByClass(WordCount.class);

    //job.submit();
    }
}


