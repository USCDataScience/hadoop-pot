/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nasa.jpl.memex.pooledtimeseries;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.opencv.core.Core;

public class MeanChiSquareDistanceCalculation {
	private static final Logger LOG = Logger.getLogger(MeanChiSquareDistanceCalculation.class.getName());
	static int videos=0;
    public static class Map extends Mapper<LongWritable, Text, IntWritable, DoubleWritable> {
    	
    	private InputStream getInputStreamFromHDFS(String pathToHDFS) throws IOException{
    		Path videoPath = new Path(pathToHDFS.toString());
    		return videoPath.getFileSystem(new Configuration()).open(videoPath);
    	}
    	
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException, NumberFormatException {
        	videos++;    
        	System.out.println(videos);
        	LOG.info("Processing pair - " + value);
        	long startTime = System.currentTimeMillis(); 
            String[] videoPaths = value.toString().split(",");
            ArrayList<double[]> tws = PoT.getTemporalWindows(4);
            ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();

            // If we're looking at a pair of videos where the videos are the same
            // we don't include them in the meanChiSquareDistance calculation.
            if (videoPaths[0].equals(videoPaths[1]))
                return;

            for (String video: videoPaths) {
                ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();
                
                multiSeries.add(PoT.loadTimeSeries(getInputStreamFromHDFS(video + ".of.txt")));
                multiSeries.add(PoT.loadTimeSeries(getInputStreamFromHDFS(video + ".hog.txt")));
                
                FeatureVector fv = new FeatureVector();
                for (int i = 0; i < multiSeries.size(); i++) {
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 1));
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 2));
                    fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 5));
                }
                fvList.add(fv);
            }
            
            LOG.info("Loaded Time Series for pair - " + value);

            for (int i = 0; i < fvList.get(0).numDim(); i++) {
                context.write(new IntWritable(i), new DoubleWritable(
                    PoT.chiSquareDistance(
                        fvList.get(0).feature.get(i),
                        fvList.get(1).feature.get(i)
                    )
                ));
            }
            
            LOG.info("Complated processing pair - " + value);
            LOG.info("Time taken to complete job - " + (System.currentTimeMillis() - startTime));
        }
    }

    public static class Reduce extends Reducer<IntWritable, DoubleWritable, NullWritable, DoubleWritable> {
        public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            double sum = 0;
            int count = 0;
            for (DoubleWritable value : values) {
                sum += value.get();
                count++;
            }

            context.write(null, new DoubleWritable(sum / (double) count));
        }

    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Configuration baseConf = new Configuration();
	baseConf.set("mapreduce.job.maps", "96");
	baseConf.set("mapred.tasktracker.map.tasks.maximum", "96");
        
	JobConf conf = new JobConf();
        System.out.println("Before Map:"+ conf.getNumMapTasks());
        conf.setNumMapTasks(96);
        System.out.println("After Map:"+ conf.getNumMapTasks());

	Job job = Job.getInstance(baseConf);
        job.setJarByClass(MeanChiSquareDistanceCalculation.class);
	
        job.setJobName("mean_chi_square_calculation");
	System.out.println("Job ID" + job.getJobID());
	System.out.println("Track:" + baseConf.get("mapred.job.tracker"));
        System.out.println("Job Name"+job.getJobName());
        System.out.println(baseConf.get("mapreduce.job.maps"));

        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(DoubleWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.waitForCompletion(true);
    }
}


