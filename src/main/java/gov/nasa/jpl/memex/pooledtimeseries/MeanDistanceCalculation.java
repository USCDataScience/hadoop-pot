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

public class MeanDistanceCalculation {
    public static class Map extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException, NumberFormatException {
            System.out.println(value.toString());
            Configuration conf = context.getConfiguration();
            String videoListPath = conf.get("videoListPath"); //video pairs
            String hofList = conf.get("hofFileList");
            String hogList = conf.get("hogFileList");

            String[] videoPaths = value.toString().split(",");
            String[] hofPaths = value.toString().split("\\n");
            String[] hogPaths = value.toString().split("\\n");
            ArrayList<double[]> tws = PoT.getTemporalWindows(4);
            ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();
            
            int count=0;
            for (String video: videoPaths) {
                ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();

                String ofCachePath = hofPaths[count];
                String hogCachePath = hogPaths[count];

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
            
            double[] mean_dists = new double[fvList.get(0).numDim()];
            for (int i = 0; i < fvList.get(0).numDim(); i++)
                mean_dists[i] = PoT.meanChiSquareDistances(fvList, i);
        
        for(int i=0;i<mean_dists.length;i++){
            context.write(new Text(String.valueOf(mean_dists[i])), new Text(""));
    }
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Configuration baseConf = new Configuration();
        baseConf.set("videoListPath", args[2]);
        baseConf.set("hofFileList", args[3]);
        baseConf.set("hogFileList", args[4]);
        
        Job job = Job.getInstance(baseConf);
        job.setJarByClass(MeanDistanceCalculation.class);

        job.setJobName("mean_distance_calc");

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
}


