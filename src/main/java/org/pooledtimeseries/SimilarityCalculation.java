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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.pooledtimeseries.cartesian.CartesianInputFormat;
import org.pooledtimeseries.util.HadoopFileUtil;
import org.pooledtimeseries.util.PoTSerialiser;
import org.pooledtimeseries.util.ReadSeqFileUtil;

public class SimilarityCalculation {

	private static final Logger LOG = Logger.getLogger(SimilarityCalculation.class.getName());

	static int videos = 0;

	public static class Map extends MapReduceBase implements Mapper<Text, BytesWritable, Text, Text> {

		double[] meanDists = null;

		@Override
		public void configure(JobConf conf) {
			super.configure(conf);
			String meanDistsPath = conf.get("meanDistsFilePath");
			List<Double> meanDistsList = new ArrayList<Double>();
			InputStream in = null;
			try {		
				in = HadoopFileUtil.getInputStreamFromHDFS(meanDistsPath);
				Scanner scin = new Scanner(in) ;
				while (scin.hasNextDouble()) {
					meanDistsList.add(scin.nextDouble());
				}
				scin.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(in !=null){
					try {
						in.close();
					} catch (IOException e) {}
				}
			}

			this.meanDists = ArrayUtils.toPrimitive(meanDistsList.toArray(new Double[0]));
			LOG.info("Loaded meanDist of length - " + meanDists.length);
		}

		@Override
		public void map(Text key, BytesWritable value, OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			videos++;
			LOG.info("Processing pair - " + key);
			long startTime = System.currentTimeMillis();
			
			String[] videoPaths = ReadSeqFileUtil.getFileNames(key);

			List<FeatureVector> fvList = (List<FeatureVector>) PoTSerialiser.getObject(value.getBytes()) ;
			LOG.info("Loaded Time Series for pair in - " + (System.currentTimeMillis() - startTime));
			
			double similarity = PoT.kernelDistance(fvList.get(0), fvList.get(1), meanDists);

			File p1 = new File(videoPaths[0]);
			File p2 = new File(videoPaths[1]);
			output.collect(new Text(p1.getName() + ',' + p2.getName()), new Text(String.valueOf(similarity)));
			
			LOG.info("Completed processing pair - " + key);
            LOG.info("Time taken to complete job - " + (System.currentTimeMillis() - startTime));
		}
	}

	public static void main(String[] args) throws Exception {

		JobConf conf = new JobConf();
		System.out.println("Before Map:" + conf.getNumMapTasks());
		conf.setNumMapTasks(196);
		System.out.println("After Map:" + conf.getNumMapTasks());
		conf.setJobName("similarity_calc");

		conf.set("meanDistsFilePath", args[2]);

		System.out.println("Job Name: " + conf.getJobName());
		conf.setJarByClass(SimilarityCalculation.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setInputFormat(CartesianInputFormat.class);
		CartesianInputFormat.setLeftInputInfo(conf, SequenceFileInputFormat.class,
				args[0]);
		CartesianInputFormat.setRightInputInfo(conf, SequenceFileInputFormat.class,
				args[0]);
		
		conf.setOutputFormat(TextOutputFormat.class);

		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		conf.setMapperClass(Map.class);

		JobClient.runJob(conf);
	}
}
