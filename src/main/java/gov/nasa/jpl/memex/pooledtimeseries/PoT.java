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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

/**
 * 
 * Pooled Time Series Similarity Metric.
 * 
 */
@SuppressWarnings({ "static-access", "deprecation" })
public class PoT {

  public static int frame_width = 320;
  public static int frame_height = 240;

  private static String outputFile = "similarity.txt";

  private static enum OUTPUT_FORMATS {TXT, JSON}
  private static OUTPUT_FORMATS outputFormat = OUTPUT_FORMATS.TXT;

  private static final Logger LOG = Logger.getLogger(PoT.class.getName());

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    Option dirOpt = OptionBuilder.withArgName("directory").hasArg()
        .withLongOpt("dir")
        .withDescription("A directory with image files in it").create('d');

    Option helpOpt = OptionBuilder.withLongOpt("help")
        .withDescription("Print this message.").create('h');

    Option pathFileOpt = OptionBuilder
        .withArgName("path file")
        .hasArg()
        .withLongOpt("pathfile")
        .withDescription(
            "A file containing full absolute paths to videos. Previous default was memex-index_temp.txt")
        .create('p');

    Option outputFileOpt = OptionBuilder
        .withArgName("output file")
        .withLongOpt("outputfile")
        .hasArg()
        .withDescription("File containing similarity results. Defaults to ./similarity.txt")
        .create('o');

    Option jsonOutputFlag = OptionBuilder
        .withArgName("json output")
        .withLongOpt("json")
        .withDescription("Set similarity output format to JSON. Defaults to .txt")
        .create('j');

    Options options = new Options();
    options.addOption(dirOpt);
    options.addOption(pathFileOpt);
    options.addOption(helpOpt);
    options.addOption(outputFileOpt);
    options.addOption(jsonOutputFlag);

    // create the parser
    CommandLineParser parser = new DefaultParser();

    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      String directoryPath = null;
      String pathFile = null;
      ArrayList<Path> videoFiles = null;

      if (line.hasOption("dir")) {
        directoryPath = line.getOptionValue("dir");
      }

      if (line.hasOption("pathfile")) {
        pathFile = line.getOptionValue("pathfile");
      }

      if (line.hasOption("outputfile")) {
          outputFile = line.getOptionValue("outputfile");
      }

      if (line.hasOption("json")) {
          outputFormat = OUTPUT_FORMATS.JSON;
      }

      if (line.hasOption("help")
          || (line.getOptions() == null || (line.getOptions() != null && line
              .getOptions().length == 0))
          || (directoryPath != null && pathFile != null
              && !directoryPath.equals("") && !pathFile.equals(""))) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("pooled_time_series", options);
        System.exit(1);
      }

      if (directoryPath != null) {
        File dir = new File(directoryPath);
        List<File> files = (List<File>) FileUtils.listFiles(dir,
            TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        videoFiles = new ArrayList<Path>(files.size());

        for (File file : files) {
          String filePath = file.toString();

          // When given a directory to load videos from we need to ensure that we
          // don't try to load the of.txt and hog.txt intermediate result files
          // that results from previous processing runs.
          if (!filePath.contains(".txt")) {
            videoFiles.add(file.toPath());
          }
        }

        LOG.info("Added " + videoFiles.size() + " video files from "
            + directoryPath);

      }

      if (pathFile != null) {
        Path list_file = Paths.get(pathFile);
        videoFiles = loadFiles(list_file);
        LOG.info("Loaded " + videoFiles.size() + " video files from "
            + pathFile);
      }

      evaluateSimilarity(videoFiles, 0);
      LOG.info("done.");

    } catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
    }

  }

  public static void evaluateSimilarity(ArrayList<Path> files, int save_mode) {
    // PoT level set
    ArrayList<double[]> tws = getTemporalWindows(4);

    // computing feature vectors
    ArrayList<FeatureVector> fv_list = new ArrayList<FeatureVector>();

    for (int k = 0; k < files.size(); k++) {
      try {
        LOG.fine(files.get(k).toString());

        ArrayList<double[][]> multi_series = new ArrayList<double[][]>();
        Path file = files.get(k);

        // optical flow descriptors
        String series_name1 = file.toString() + ".of.txt";
        Path series_path1 = Paths.get(series_name1);
        double[][] series1;

        if (save_mode == 0) {
          series1 = getOpticalTimeSeries(file, 5, 5, 8);
          saveVectors(series1, series_path1);

        } else {
          series1 = loadTimeSeries(series_path1);
        }

        multi_series.add(series1);

        // gradients descriptors
        String series_name2 = file.toString() + ".hog.txt";
        Path series_path2 = Paths.get(series_name2);
        double[][] series2;

        if (save_mode == 0) {
          series2 = getGradientTimeSeries(file, 5, 5, 8);
          saveVectors(series2, series_path2);
        } else {
          series2 = loadTimeSeries(series_path2);
        }

        multi_series.add(series2);

        // computing features from series of descriptors
        FeatureVector fv = new FeatureVector();

        for (int i = 0; i < multi_series.size(); i++) {
          fv.feature.add(computeFeaturesFromSeries(multi_series.get(i), tws, 1));
          fv.feature.add(computeFeaturesFromSeries(multi_series.get(i), tws, 2));
          fv.feature.add(computeFeaturesFromSeries(multi_series.get(i), tws, 5));
        }
        System.out.println("");
        fv_list.add(fv);

      } catch (PoTException e) {
        LOG.severe("PoTException occurred: " + e.message + ": Skipping file " + files.get(k));
        continue;
      }
    }
    double[][] similarities = calculateSimilarities(fv_list);
    writeSimilarityOutput(files, similarities);
  }

  private static double[][] calculateSimilarities(ArrayList<FeatureVector> fv_list) {
    // feature vector similarity measure
    if (fv_list.size() < 1) {
      LOG.info("Feature Vector list is empty. Nothing to calculate. Exiting...");
      System.exit(1);
    }
    double[] mean_dists = new double[fv_list.get(0).numDim()];
    for (int i = 0; i < fv_list.get(0).numDim(); i++)
      mean_dists[i] = meanChiSquareDistances(fv_list, i);

    for (int i = 0; i < fv_list.get(0).numDim(); i++)
      System.out.format("%f ", mean_dists[i]);
    System.out.println("");

    double[][] sims = new double[fv_list.size()][fv_list.size()];
    for (int i = 0; i < fv_list.size(); i++) {
      for (int j = 0; j < fv_list.size(); j++) {
        sims[i][j] = kernelDistance(fv_list.get(i), fv_list.get(j), mean_dists);
      }
    }

    return sims;
  }

  private static void writeSimilarityOutput(ArrayList<Path> files, double[][] similarities) {
    if (outputFormat == OUTPUT_FORMATS.TXT) {
      writeSimilarityToTextFile(similarities);
    } else if (outputFormat == OUTPUT_FORMATS.JSON) {
      writeSimilarityToJSONFile(files, similarities);
    } else {
      LOG.severe("Invalid output format. Skipping similarity dump.");
    }
  }

  private static void writeSimilarityToTextFile(double[][] similarities) {
    try {
      FileOutputStream fos = new FileOutputStream(outputFile);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

      for (int i = 0; i < similarities.length; i++) {
        for (int j = 0; j < similarities[0].length; j++) {
          writer.write(String.format("%f,", similarities[i][j]));
        }
        writer.newLine();
      }

      writer.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeSimilarityToJSONFile(ArrayList<Path> files, double[][] similarities) {
    JSONObject root_json_obj = new JSONObject();

    for (int i = 0; i < similarities.length; i++) {
      JSONObject fileJsonObj = new JSONObject();

      for (int j = 0; j < similarities[0].length; j++) {
        fileJsonObj.put(files.get(j).getFileName(), similarities[i][j]);
      }

      root_json_obj.put(files.get(i).getFileName(), fileJsonObj);
    }

    try {
      outputFile = outputFile.substring(0, outputFile.lastIndexOf('.')) + ".json";
      FileWriter file = new FileWriter(outputFile);
      file.write(root_json_obj.toJSONString());
      file.flush();
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<Path> loadFiles(Path list_file) {
    ArrayList<Path> filenames = new ArrayList<Path>();

    try (InputStream in = Files.newInputStream(list_file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        filenames.add(Paths.get(line));
      }
    } catch (IOException x) {
      System.err.println(x);
    }

    return filenames;
  }

  public static double[][] getOpticalTimeSeries(Path filename, int w_d,
      int h_d, int o_d) throws PoTException {
    ArrayList<double[][][]> hists = getOpticalHistograms(filename, w_d, h_d,
        o_d);
    double[][] vectors = new double[hists.size()][];

    for (int i = 0; i < hists.size(); i++) {
      vectors[i] = histogramToVector(hists.get(i));
    }

    return vectors;
  }

  static double[] histogramToVector(double[][][] hist) {
    int d1 = hist.length;
    int d2 = hist[0].length;
    int d3 = hist[0][0].length;
    double[] vector = new double[d1 * d2 * d3];

    for (int i = 0; i < d1; i++) {
      for (int j = 0; j < d2; j++) {
        for (int k = 0; k < d3; k++) {
          vector[d3 * d2 * i + d3 * j + k] = hist[i][j][k];
        }
      }
    }

    return vector;
  }

  static ArrayList<double[][][]> getOpticalHistograms(Path filename, int w_d,
      int h_d, int o_d) throws PoTException{
    ArrayList<double[][][]> histograms = new ArrayList<double[][][]>();

    VideoCapture capture = new VideoCapture(filename.toString());

    if (!capture.isOpened()) {
      LOG.warning("video file not opened.");
      
      double[][][] hist = new double[w_d][h_d][o_d];
      histograms.add(hist);
    }
    else {
	    // variables for processing images
	    Mat original_frame = new Mat();
	
	    Mat frame = new Mat();
	    Mat frame_gray = new Mat();
	    Mat prev_frame_gray = new Mat();
	    MatOfPoint2f flow = new MatOfPoint2f();
	
	    // computing a list of histogram of optical flows (i.e. a list of 5*5*8
	    // arrays)
	    for (int frame_index = 0;; frame_index++) {
	      // capturing the video images
	      capture.read(original_frame);
	
	      if (original_frame.empty()) {
            if (original_frame.empty()) {
              if (frame_index == 0) {
                throw new PoTException("Could not read the video file");
              }
              else
                break;
            }
	      } else {
	        // resizing the captured frame and converting it to the gray scale
	        // image.
	        Imgproc.resize(original_frame, frame, new Size(frame_width,
	            frame_height));
	        Imgproc.cvtColor(frame, frame_gray, Imgproc.COLOR_BGR2GRAY);
	
	        double[][][] hist = new double[w_d][h_d][o_d];
	        histograms.add(hist);
	
	        // from frame #2
	        if (frame_index > 0) {
	          // calculate optical flows
	          Video.calcOpticalFlowFarneback(prev_frame_gray, frame_gray, flow,
	              0.5, 1, 10, 2, 7, 1.5, 0); // 0.5, 1, 15, 2, 7, 1.5, 0
	
	          // update histogram of optical flows
	          updateOpticalHistogram(histograms.get(frame_index), flow);
	        }
	
	        Mat temp_frame = prev_frame_gray;
	        prev_frame_gray = frame_gray;
	        frame_gray = temp_frame;
	      }
	    }
	
	    capture.release();
    }

    return histograms;
  }

  static void updateOpticalHistogram(double[][][] hist, Mat flow) {
    int d1 = hist.length;
    int d2 = hist[0].length;
    int d3 = hist[0][0].length;

    int step = 4; // 5;

    for (int x = 0; x < frame_width; x += step) {
      int x_type = (int) (x * d1 / frame_width);

      for (int y = 0; y < frame_height; y += step) {
        int y_type = (int) (y * d2 / frame_height);

        Point fxy = new Point(flow.get(y, x));

        double size = (fxy.x + fxy.y) * (fxy.x + fxy.y);

        if (size < 9) {
          continue; // 25
        } else {
          int f_type = opticalFlowType(fxy, d3);

          hist[x_type][y_type][f_type]++;
        }
      }
    }
  }

  static int opticalFlowType(Point fxy, int dim) {
    double degree = Math.atan2(fxy.y, fxy.x);
    int type = 7;

    for (int i = 0; i < dim; i++) {
      double boundary = (i + 1) * 2 * Math.PI / dim - Math.PI;

      if (degree < boundary) {
        type = i;
        break;
      }
    }

    return type;
  }

  public static void saveVectors(double[][] vectors, Path outfile) {
    int d = vectors[0].length;

    ArrayList<double[][][]> temp_hists = new ArrayList<double[][][]>();

    for (int i = 0; i < vectors.length; i++) {
      double[][][] temp_hist = new double[1][1][d];
      temp_hist[0][0] = vectors[i];

      temp_hists.add(temp_hist);
    }

    saveHistograms(temp_hists, outfile);
  }

  static void saveHistograms(ArrayList<double[][][]> hists, Path outfile) {
    int w_d = hists.get(0).length;
    int h_d = hists.get(0)[0].length;
    int o_d = hists.get(0)[0][0].length;

    int i, j, k, l;

    try (FileOutputStream fos = new FileOutputStream(outfile.toFile());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
      String head = String.format("%d %d", hists.size(), w_d * h_d * o_d);
      writer.write(head);
      writer.newLine();

      for (l = 0; l < (int) hists.size(); l++) {
        double[][][] hist = hists.get(l);

        for (i = 0; i < hist.length; i++) {
          for (j = 0; j < hist[0].length; j++) {
            for (k = 0; k < hist[0][0].length; k++) { // optical_bins+1
              writer.write(String.format("%f ", hist[i][j][k]));
            }
          }
        }

        writer.newLine();
      }

    } catch (IOException x) {
      System.err.println(x);
    }
  }

  public static double[][] loadTimeSeries(Path filename) {
    double[][] series = new double[1][1];

    try (InputStream in = Files.newInputStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      Scanner scin = new Scanner(in);

      int num_frames = scin.nextInt();
      int dim = scin.nextInt();

      series = new double[num_frames][dim];

      for (int i = 0; i < num_frames; i++) {
        for (int j = 0; j < dim; j++) {
          series[i][j] = scin.nextDouble();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return series;
  }

  public static double[][] getGradientTimeSeries(Path filename, int w_d,
      int h_d, int o_d) throws PoTException {
    ArrayList<double[][][]> hists = getGradientHistograms(filename, w_d, h_d,
        o_d);
    double[][] vectors = new double[hists.size()][];

    for (int i = 0; i < hists.size(); i++) {
      vectors[i] = histogramToVector(hists.get(i));
    }

    return vectors;
  }

  static ArrayList<double[][][]> getGradientHistograms(Path filename, int w_d,
      int h_d, int o_d) throws PoTException{
    ArrayList<double[][][]> histograms = new ArrayList<double[][][]>();

    VideoCapture capture = new VideoCapture(filename.toString());

    if (!capture.isOpened()) {
      LOG.warning("video file not opened.");
      
      double[][][] hist = new double[w_d][h_d][o_d];
      histograms.add(hist);
    }
    else {
	    // variables for processing images
	    Mat original_frame = new Mat();
	    Mat resized = new Mat();
	    Mat resized_gray = new Mat();
	
	    // initializing a list of histogram of gradients (i.e. a list of s*s*9
	    // arrays)
	    for (int i = 0;; i++) {
	      // capturing the video images
	      capture.read(original_frame);
	      if (original_frame.empty()) {
            if (original_frame.empty()) {
              if (i == 0) {
                throw new PoTException("Could not read the video file");
              }
              else
                break;
            }
	      }
	
	      double[][][] hist = new double[w_d][h_d][o_d];
	
	      Imgproc.resize(original_frame, resized, new Size(frame_width,
	          frame_height));
	      Imgproc.cvtColor(resized, resized_gray, Imgproc.COLOR_BGR2GRAY);
	
	      ArrayList<double[][]> gradients = computeGradients(resized_gray, o_d);
	      updateGradientHistogram(hist, gradients);
	
	      histograms.add(hist);
	    }
	
	    capture.release();
    }

    return histograms;
  }

  static ArrayList<double[][]> computeGradients(Mat frame, int dim) {
    byte frame_array[] = new byte[(int) frame.total()];
    frame.get(0, 0, frame_array);

    ArrayList<double[][]> gradients = new ArrayList<double[][]>();

    for (int k = 0; k < dim; k++) {
      double angle = Math.PI * (double) k / (double) dim;

      double dx = Math.cos(angle) * 0.9999999;
      double dy = Math.sin(angle) * 0.9999999;

      double[][] grad = new double[frame.width()][frame.height()];

      for (int i = 0; i < frame.cols(); i++) {
        for (int j = 0; j < frame.rows(); j++) {
          if (i <= 1 || j <= 1 || i >= frame.cols() - 2
              || j >= frame.rows() - 2) {
            grad[i][j] = 0;
          } else {
            double f1 = interpolatePixel(frame_array, frame.cols(), (double) i
                + dx, (double) j + dy);
            double f2 = interpolatePixel(frame_array, frame.cols(), (double) i
                - dx, (double) j - dy);

            double diff = f1 - f2;
            if (diff < 0)
              diff = diff * -1;
            if (diff >= 256)
              diff = 255;

            grad[i][j] = diff;
          }
        }
      }

      gradients.add(grad);
    }

    return gradients;
  }

  static double interpolatePixel(byte[] image, int w, double x, double y) {
    double x1 = (double) ((int) x);
    double x2 = (double) ((int) x + 1);
    double y1 = (double) ((int) y);
    double y2 = (double) ((int) y + 1);

    double f11 = (double) (image[(int) y * w + (int) x] & 0xFF);
    double f21 = (double) (image[(int) y * w + (int) x + 1] & 0xFF);
    double f12 = (double) (image[(int) (y + 1) * w + (int) x] & 0xFF);
    double f22 = (double) (image[(int) (y + 1) * w + (int) x + 1] & 0xFF);

    double f = f11 * (x2 - x) * (y2 - y) + f21 * (x - x1) * (y2 - y) + f12
        * (x2 - x) * (y - y1) + f22 * (x - x1) * (y - y1);

    return f;
  }

  static void updateGradientHistogram(double[][][] hist,
      ArrayList<double[][]> gradients) {
    int d1 = hist.length;
    int d2 = hist[0].length;
    int d3 = hist[0][0].length;

    int width = gradients.get(0).length;
    int height = gradients.get(0)[0].length;

    for (int i = 0; i < width; i++) {
      int s1_index = i * d1 / width;

      for (int j = 0; j < height; j++) {
        int s2_index = j * d2 / height;

        for (int k = 0; k < d3; k++) {
          double val = gradients.get(k)[i][j] / 100;
          hist[s1_index][s2_index][k] += val;
        }
      }
    }
  }

  public static ArrayList<double[]> getTemporalWindows(int level) {
    ArrayList<double[]> fws = new ArrayList<double[]>();

    for (int l = 0; l < level; l++) {
      int cascade_steps = (int) Math.pow((double) 2, (double) l);// 2;
      double step_size = (double) 1 / (double) cascade_steps;

      for (int k = 0; k < cascade_steps; k++) {
        double start = step_size * (double) k + 0.000001;
        double end = step_size * (double) (k + 1) + 0.000001;

        double[] wind = new double[2];
        wind[0] = start;
        wind[1] = end;

        fws.add(wind);
      }
    }

    return fws;
  }

  public static ArrayList<Double> computeFeaturesFromSeries(double[][] series,
      ArrayList<double[]> time_windows_list, int feature_mode) {
    int start = 0;
    int end = series.length - 1;

    ArrayList<Double> feature = new ArrayList<Double>();

    for (int j = 0; j < time_windows_list.size(); j++) {
      int duration = end - start;

      for (int i = 0; i < series[0].length; i++) {
        if (duration < 0) {
          if (feature_mode == 2 || feature_mode == 4) {
            feature.add(0.0);
            feature.add(0.0);
          } else
            feature.add(0.0);

          continue;
        }

        int window_start = start
            + (int) (duration * time_windows_list.get(j)[0] + 0.5);
        int window_end = start
            + (int) (duration * time_windows_list.get(j)[1] + 0.5);

        if (feature_mode == 1) { // Sum pooling
          double sum = 0;

          for (int t = window_start; t <= window_end; t++) {
            if (t < 0)
              continue;

            sum += series[t][i];
          }

          feature.add(sum);
        } else if (feature_mode == 2) { // Gradient pooling1
          double positive_gradients = 0;
          double negative_gradients = 0;

          for (int t = window_start; t <= window_end; t++) {
            int look = 2;

            if (t - look < 0)
              continue;
            else {
              double dif = series[t][i] - series[t - look][i];

              if (dif > 0.01) { // 0.01 for optical
                positive_gradients++;
              } else if (dif < -0.01) { // if (dif<-10)
                negative_gradients++;
              }
            }
          }

          feature.add(positive_gradients);
          feature.add(negative_gradients);
        } else if (feature_mode == 4) { // Gradient pooling2
          double positive_gradients = 0;
          double negative_gradients = 0;

          for (int t = window_start; t <= window_end; t++) {
            int look = 2;

            if (t - look < 0)
              continue;
            else {
              double dif = series[t][i] - series[t - look][i];

              if (dif > 0) {
                positive_gradients += dif;
              } else {
                negative_gradients += -dif;
              }
            }
          }

          feature.add(positive_gradients);
          feature.add(negative_gradients);
        } else if (feature_mode == 5) { // Max pooling
          double max = -1000000;

          for (int t = window_start; t <= window_end; t++) {
            if (t < 0)
              continue;

            if (series[t][i] > max)
              max = series[t][i];
          }

          feature.add(max);
        }
      }
    }

    return feature;
  }

  public static void normalizeFeatureL1(ArrayList<Double> sample) {
    int sum = 0;

    for (int i = 0; i < sample.size(); i++) {
      double val = sample.get(i);
      if (val < 0)
        val = -1 * val;

      sum += val;
    }

    for (int i = 0; i < sample.size(); i++) {
      double v;
      if (sum == 0)
        v = 0;
      else
        v = sample.get(i) / sum;// *100;

      sample.set(i, v);
    }
  }

  static double chiSquareDistance(ArrayList<Double> feature1,
      ArrayList<Double> feature2) {
    if (feature1.size() != feature2.size())
      LOG.warning("feature vector dimension mismatch.");

    double score = 0;

    for (int i = 0; i < feature1.size(); i++) {
      double h1 = feature1.get(i);
      double h2 = feature2.get(i);

      if (h1 < 0 || h2 < 0) {
        LOG.warning("A negative feature value. The chi square kernel "
            + "does not work with negative values. Please try shifting "
            + "the vector to make all its elements positive.");
      }

      if (h1 == h2)
        continue;
      else
        score += (h1 - h2) * (h1 - h2) / (h1 + h2);
    }

    return 0.5 * score;
  }

  static double meanChiSquareDistances(ArrayList<FeatureVector> samples, int d) {
    double mean_dist = 0;

    double sum = 0;
    int count = 0;

    for (int i = 0; i < samples.size(); i++) {
      for (int j = i + 1; j < samples.size(); j++) {
        count++;

        sum += chiSquareDistance(samples.get(i).feature.get(d),
            samples.get(j).feature.get(d));
      }
    }

    mean_dist = sum / (double) count;

    return mean_dist;
  }

  static double kernelDistance(FeatureVector sample1, FeatureVector sample2,
      double[] mean_dists) {
    double distance = 0;

    for (int d = 0; d < sample1.numDim(); d++) {
      double weight = 1;

      double val = chiSquareDistance(sample1.feature.get(d),
          sample2.feature.get(d))
          / mean_dists[d] * weight;
      if (mean_dists[d] == 0)
        val = chiSquareDistance(sample1.feature.get(d), sample2.feature.get(d)) / 1000000.0;

      distance = distance + val;
    }

    double final_score = Math.exp(-1 * distance / 10); // 10000 10

    return final_score;
  }
}
