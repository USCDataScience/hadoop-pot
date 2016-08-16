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

package org.pooledtimeseries;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.pooledtimeseries.cartesian.CartesianInputFormat;
import org.pooledtimeseries.util.ReadSeqFileUtil;

public class MeanChiSquareDistanceCalculation {
	private static final Logger LOG = Logger.getLogger(MeanChiSquareDistanceCalculation.class.getName());
	static int videos=0;
    public static class Map extends MapReduceBase implements Mapper<Text, Text, IntWritable, DoubleWritable> {
    	
    	@Override
    	public void map(Text key, Text value, OutputCollector<IntWritable, DoubleWritable> output, Reporter reporter) throws IOException {
        	videos++;    
        	System.out.println(videos);
        	LOG.info("Processing pair - " + key);
        	long startTime = System.currentTimeMillis();
        	
        	String[] videoFiles = ReadSeqFileUtil.getFileNames(key);
            
        	// If we're looking at a pair of videos where the videos are the same
            // we don't include them in the meanChiSquareDistance calculation.
            if (videoFiles[0].equals(videoFiles[1]))
                return;
            
            List<FeatureVector> fvList = ReadSeqFileUtil.computeFeatureFromSeries(value);
            
            LOG.info("Loaded Time Series for pair in - " + (System.currentTimeMillis() - startTime));

            for (int i = 0; i < fvList.get(0).numDim(); i++) {
            	
            	output.collect(new IntWritable(i), new DoubleWritable(
                    PoT.chiSquareDistance(
                        fvList.get(0).feature.get(i),
                        fvList.get(1).feature.get(i)
                    )
                ));
            }
            
            LOG.info("Completed processing pair - " + key);
            LOG.info("Time taken to complete job - " + (System.currentTimeMillis() - startTime));
        }
    }
    
    public static class Reduce extends MapReduceBase implements Reducer<IntWritable, DoubleWritable, NullWritable, DoubleWritable>{
    
    	public void reduce(IntWritable key, Iterator<DoubleWritable> values,
				OutputCollector<NullWritable, DoubleWritable> output, Reporter reporter) throws IOException {
            double sum = 0;
            int count = 0;
            
            while (values.hasNext()){
                sum += values.next().get();
                count++;
            }

            output.collect(null, new DoubleWritable(sum / (double) count));
        }

		

    }

    public static void main(String[] args) throws Exception {

        Configuration baseConf = new Configuration();
        baseConf.set("mapreduce.job.maps", "96");
        baseConf.set("mapred.tasktracker.map.tasks.maximum", "96");
        
        JobConf conf = new JobConf(baseConf, MeanChiSquareDistanceCalculation.class);
        System.out.println("Before Map:"+ conf.getNumMapTasks());
        conf.setNumMapTasks(96);
        System.out.println("After Map:"+ conf.getNumMapTasks());

	
        conf.setJobName("mean_chi_square_calculation");
	
        System.out.println("Track:" + baseConf.get("mapred.job.tracker"));
        System.out.println("Job Name- "+conf.getJobName());
        System.out.println(baseConf.get("mapreduce.job.maps"));

        conf.setMapOutputKeyClass(IntWritable.class);
        conf.setMapOutputValueClass(DoubleWritable.class);
        conf.setOutputKeyClass(IntWritable.class);
        conf.setOutputValueClass(DoubleWritable.class);

        conf.setOutputFormat(TextOutputFormat.class);
        
        conf.setInputFormat(CartesianInputFormat.class);
		CartesianInputFormat.setLeftInputInfo(conf, SequenceFileInputFormat.class,
				args[0]);
		CartesianInputFormat.setRightInputInfo(conf, SequenceFileInputFormat.class,
				args[0]);


        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        conf.setMapperClass(Map.class);
        conf.setReducerClass(Reduce.class);

        JobClient.runJob(conf);
    }
}


