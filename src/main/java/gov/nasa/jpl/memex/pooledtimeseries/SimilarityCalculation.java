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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.opencv.core.Core;


public class SimilarityCalculation {
	static int videos=0;
    public static class Map extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException, NumberFormatException {
            videos++;
        System.out.println(videos);

            Configuration conf = context.getConfiguration();
            String meanDistsPath = conf.get("meanDistsFilePath");

            String[] videoPaths = value.toString().split(",");
            ArrayList<double[]> tws = PoT.getTemporalWindows(4);
            ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();
            

            for (String video: videoPaths) {
                ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();

                String ofCachePath = video + ".of.txt";
                String hogCachePath = video + ".hog.txt";

                double[][] series1 = PoT.loadTimeSeries(new File(ofCachePath).toPath());
                double[][] series2 = PoT.loadTimeSeries(new File(hogCachePath).toPath());
                multiSeries.add(series1);
                multiSeries.add(series2);

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
                counter++;
            }

            double similarity = PoT.kernelDistance(fvList.get(0), fvList.get(1), meanDists);
            
            File p1 = new File(videoPaths[0]);
            File p2 = new File(videoPaths[1]);
            context.write(new Text(p1.getName() + ',' + p2.getName()), new Text(String.valueOf(similarity)));
        }
    }


    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Configuration baseConf = new Configuration();
	baseConf.set("mapreduce.job.maps", "96");
        baseConf.set("mapreduce.job.reduces", "0");
	baseConf.set("mapred.tasktracker.map.tasks.maximum", "96");
        baseConf.set("meanDistsFilePath", args[2]);
	
	JobConf conf = new JobConf();
        System.out.println("Before Map:"+ conf.getNumMapTasks());
        conf.setNumMapTasks(196);
        System.out.println("After Map:"+ conf.getNumMapTasks());

        Job job = Job.getInstance(baseConf);
        System.out.println("Track: "+baseConf.get("mapred.job.tracker"));
	System.out.println("Job ID" + job.getJobID());
        System.out.println("Job Name"+job.getJobName());
        System.out.println(baseConf.get("mapreduce.job.maps"));
	job.setJarByClass(SimilarityCalculation.class);

        job.setJobName("similarity_calc");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setMapperClass(Map.class);

        job.waitForCompletion(true);
    }
}


