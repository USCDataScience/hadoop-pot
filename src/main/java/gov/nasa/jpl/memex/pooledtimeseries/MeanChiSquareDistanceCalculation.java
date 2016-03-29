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
import java.util.*;
import java.io.*;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
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

public class MeanChiSquareDistanceCalculation {
    public static class Map extends Mapper<LongWritable, Text, IntWritable, DoubleWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException, NumberFormatException {
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

            for (int i = 0; i < fvList.get(0).numDim(); i++) {
                context.write(new IntWritable(i), new DoubleWritable(
                    PoT.chiSquareDistance(
                        fvList.get(0).feature.get(i),
                        fvList.get(1).feature.get(i)
                    )
                ));
            }
        }
    }

    public static class Reduce extends Reducer<IntWritable, DoubleWritable, NullWritable, DoubleWritable> {
        public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
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

        Job job = Job.getInstance(baseConf);
        job.setJarByClass(MeanChiSquareDistanceCalculation.class);

        job.setJobName("mean_chi_square_calculation");

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


