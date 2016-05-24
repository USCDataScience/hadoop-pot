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



import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat;

import org.opencv.core.Core;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class OpticalTimeSeries {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

            try {
                double[][] series1 = PoT.getOpticalTimeSeries(new File(value.toString()).toPath(), 5, 5, 8);
                String ofVector = saveVectors(series1);
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
            String fName =fileName + ".of.txt";
            File file = new File(fName);
            if(file.exists())
            	file.delete();
            return fName;
        }

        protected Text generateActualKey(Text key, Text value) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Configuration baseConf = new Configuration();
        baseConf.set("mapred.reduce.tasks", "0");
        JobConf conf = new JobConf(baseConf, OpticalTimeSeries.class);

        conf.setJobName("optical_time_series");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Map.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(MultiFileOutput.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}
