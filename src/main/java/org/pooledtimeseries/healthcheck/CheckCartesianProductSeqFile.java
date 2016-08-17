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

package org.pooledtimeseries.healthcheck;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.pooledtimeseries.cartesian.CartesianInputFormat;


public class CheckCartesianProductSeqFile {

	public static class CartesianMapper extends MapReduceBase implements Mapper<Text, Text, Text, IntWritable> {

		private Text simkey = new Text("simkey");
		private Text diskey = new Text("diskey");
		private static final IntWritable one = new IntWritable(1);

		public void map(Text key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			// System.out.println(value);
			System.out.println(key);
			System.out.println("");
			String[] values = value.toString().split("~");
			String leftVal[] = values[0].split("\\|");
			String rightVal = values[1];
			
			System.out.println("All- "+ values.length);
			System.out.println("Left -" + leftVal.length);
			System.out.println("Left[0] -" + leftVal[0].length());
			System.out.println("Left[1] -" + leftVal[1].length());
			System.out.println("Right -" + rightVal.length());
			
			System.out.println();
			// If the two values are equal add one to output
			if (leftVal.equals(rightVal)) {
				output.collect(simkey, one);
			} else {
				output.collect(diskey, one);
			}
		}
	}

	public static class CartesianReducer extends MapReduceBase implements Reducer<Text, IntWritable, Text, Text> {
		private Text outputVal = new Text();

		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, Text> output,
				Reporter reporter) throws IOException {
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			outputVal.set("" + sum);
			output.collect(key, outputVal);
		}

	}

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

		long start = System.currentTimeMillis();
		JobConf conf = new JobConf("Cartesian Product");
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: CartesianProduct <comment data> <out>");
			System.exit(1);
		}

		// Configure the join type
		conf.setJarByClass(CheckCartesianProductSeqFile.class);

		conf.setMapperClass(CartesianMapper.class);
		conf.setReducerClass(CartesianReducer.class);

		conf.setInputFormat(CartesianInputFormat.class);
		CartesianInputFormat.setLeftInputInfo(conf, SequenceFileInputFormat.class, otherArgs[0]);
		CartesianInputFormat.setRightInputInfo(conf, SequenceFileInputFormat.class, otherArgs[0]);

		TextOutputFormat.setOutputPath(conf, new Path(otherArgs[1]));

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		RunningJob job = JobClient.runJob(conf);
		while (!job.isComplete()) {
			Thread.sleep(1000);
		}

		long finish = System.currentTimeMillis();

		System.out.println("Time in ms: " + (finish - start));

		System.exit(job.isSuccessful() ? 0 : 2);
	}

}
