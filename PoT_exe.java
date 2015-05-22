package pooled_time_series;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;


public class PoT_exe {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Path series_file = Paths.get(args[0]);
		int pyramid_level = Integer.parseInt(args[1]);
		int pooling_mode = Integer.parseInt(args[2]);
		
		ArrayList<double[]> tws = getTemporalWindows(pyramid_level);
		double[][] series = loadTimeSeries(series_file);
		
		FeatureVector fv = new FeatureVector();
		fv.feature.add(computeFeaturesFromSeries(series, tws, pooling_mode));
		
		Path out_file = Paths.get(args[0] + ".pot.txt");
		saveFeatureVector(fv, out_file);
		
		System.out.println("done.");
	}

	public static ArrayList<Path> loadFiles(Path list_file) {
		ArrayList<Path> filenames = new ArrayList<Path>();
		
		try (InputStream in = Files.newInputStream(list_file);BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line = null;
			while((line = reader.readLine())!=null) {
				filenames.add(Paths.get(line));
			}
		} catch (IOException x) {
			System.err.println(x);
		}

		return filenames;
	}
	
	static double[] histogramToVector(double[][][] hist)
	{
		int d1 = hist.length;
		int d2 = hist[0].length;
		int d3 = hist[0][0].length;
		double[] vector = new double[d1 * d2 * d3];
		
		for(int i=0;i<d1;i++) {
			for(int j=0;j<d2;j++) { 
				for(int k=0;k<d3;k++) {
					vector[d3*d2*i + d3*j + k] = hist[i][j][k];
				}
			}
		}
		
		return vector;
	}
	
	public static void saveVectors(double[][] vectors, Path outfile)
	{
		int d = vectors[0].length;
		
		ArrayList<double[][][]> temp_hists = new ArrayList<double[][][]>();
		
		for(int i=0;i<vectors.length;i++) {
			double[][][] temp_hist = new double[1][1][d];
			temp_hist[0][0] = vectors[i];
			
			temp_hists.add(temp_hist);
		}
		
		saveHistograms(temp_hists, outfile);
	}
	
	static void saveHistograms(ArrayList<double[][][]> hists, Path outfile)
	{
		int w_d = hists.get(0).length;
		int h_d = hists.get(0)[0].length;
		int o_d = hists.get(0)[0][0].length;
		
		int i, j, k, l;
		
		try (FileOutputStream  fos = new FileOutputStream(outfile.toFile());BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
			String head = String.format("%d %d", hists.size(), w_d*h_d*o_d);
			writer.write(head);
			writer.newLine();

			for(l=0;l<(int)hists.size();l++) {
				double[][][] hist = hists.get(l);

				for(i=0;i<hist.length;i++) {
					for(j=0;j<hist[0].length;j++) {
						for(k=0;k<hist[0][0].length;k++) { // optical_bins+1
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
	
	static void saveFeatureVector(FeatureVector fv, Path outfile)
	{
		try (FileOutputStream  fos = new FileOutputStream(outfile.toFile());BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
			for(int i=0;i<fv.feature.size();i++) writer.write(String.format("%d ", fv.feature.get(i).size()));
			writer.newLine();
			
			for(int i=0;i<fv.feature.size();i++) {
				for(int j=0;j<fv.feature.get(i).size();j++) {
					writer.write(String.format("%f ", fv.feature.get(i).get(j)));
				}
			}
		} catch (IOException x) {
			System.err.println(x);
		}
	}
	
	/*public static double[][] concatenateTimeSeries(ArrayList<double[][]> multi_series)
	{
		int dim = 0;
		for(int i=0;i<multi_series.size();i++) dim += multi_series.get(i)[0].length;
		
		double[][] series = new double[multi_series.get(0).length][dim];
		
		for(int t=0;t<series.length;t++) {
			int index = 0;
			for(int i=0;i<multi_series.size();i++) {
				System.arraycopy(multi_series.get(i)[t], 0, series[t], index, multi_series.get(i)[t].length);
				index += multi_series.get(i)[t].length;
			}
		}
		
		return series;
	}
	
	public static int[] dimsTimeSeries(ArrayList<double[][]> multi_series)
	{
		int[] dims = new int[multi_series.size()];
		
		for(int i=0;i<multi_series.size();i++) dims[i] = multi_series.get(i)[0].length;
		
		return dims;
	}*/
	
	public static double[][] loadTimeSeries(Path filename)
	{
		double[][] series = new double[1][1];
		
		try (InputStream in = Files.newInputStream(filename);BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			Scanner scin = new Scanner(in); 
			
			int num_frames = scin.nextInt();
			int dim = scin.nextInt();
			
			series = new double[num_frames][dim];
			
			for(int i=0;i<num_frames;i++) {
				for(int j=0;j<dim;j++) {
					series[i][j] = scin.nextDouble();
				}
			}
		} catch (IOException x) {
			System.err.println(x);
		}

		return series;
	}
	
	
	static void updateGradientHistogram(double[][][] hist, ArrayList<double[][]> gradients)
	{
		int d1 = hist.length;
		int d2 = hist[0].length;
		int d3 = hist[0][0].length;

		int width = gradients.get(0).length;
		int height = gradients.get(0)[0].length;

		for(int i=0;i<width;i++) {
			int s1_index = i * d1 / width;
			
			for(int j=0;j<height;j++) {
				int s2_index = j * d2 / height;

				for(int k=0;k<d3;k++) {
					double val = gradients.get(k)[i][j] / 100;
					hist[s1_index][s2_index][k] += val;
				}
			}
		}
	}


	public static ArrayList<double[]> getTemporalWindows(int level)
	{
		ArrayList<double[]> fws = new ArrayList<double[]>();

		for (int l = 0; l < level; l++) {
			int cascade_steps = (int)Math.pow((double)2, (double)l);//2;
			double step_size = (double)1 / (double)cascade_steps;

			for (int k = 0; k < cascade_steps; k++) {
				double start = step_size * (double)k + 0.000001;
				double end = step_size * (double)(k + 1) + 0.000001;

				double[] wind = new double[2];
				wind[0] = start;
				wind[1] = end;
				
				fws.add(wind);
			}
		}

		return fws;
	}
	
	public static ArrayList<Double> computeFeaturesFromSeries(double[][] series, ArrayList<double[]> time_windows_list, int feature_mode)
	{
		int start = 0;
		int end = series.length - 1;
		
		ArrayList<Double> feature = new ArrayList<Double>();

		for (int j = 0; j < time_windows_list.size(); j++) {
			int duration = end - start + 1;
			
			for(int i=0;i<series[0].length;i++) {
				if (duration <= 0) {
					if (feature_mode == 2 || feature_mode == 4) {
						feature.add(0.0);
						feature.add(0.0);
					}
					else feature.add(0.0);
	
					continue;
				}
	
				int window_start = start + (int)(duration * time_windows_list.get(j)[0] + 0.5);
				int window_end = start + (int)(duration * time_windows_list.get(j)[1] + 0.5) - 1;
	
				if (feature_mode == 1) { // Sum pooling
					double sum = 0;
	
					for (int t = window_start; t <= window_end; t++) {
						if (t < 0) continue;
	
						sum += series[t][i];
					}
	
					feature.add(sum);
				}
				else if (feature_mode == 2) { // Gradient pooling1
					double positive_gradients = 0;
					double negative_gradients = 0;
	
					for (int t = window_start; t <= window_end; t++) {
						int look = 2;
	
						if (t - look < 0) continue;
						else {
							double dif = series[t][i] - series[t - look][i];
	
							if (dif>0.01) { // 0.01 for optical
								positive_gradients++;
							}
							else if (dif < -0.01) { // if (dif<-10) 
								negative_gradients++;
							}
						}
					}
	
					feature.add(positive_gradients);
					feature.add(negative_gradients);
				}
				else if (feature_mode == 4) { // Gradient pooling2
					double positive_gradients = 0;
					double negative_gradients = 0;

					for (int t = window_start; t <= window_end; t++) {
						int look = 2;
	
						if (t - look < 0) continue;
						else {
							double dif = series[t][i] - series[t - look][i];
	
							if (dif > 0) {
								positive_gradients += dif;
							}
							else {
								negative_gradients += -dif;
							}
						}
					}
	
					feature.add(positive_gradients);
					feature.add(negative_gradients);
				}
				else if (feature_mode == 5) { // Max pooling
					double max = -1000000;

					for (int t = window_start; t <= window_end; t++) {
						if (t < 0) continue;

						if (series[t][i] > max) max = series[t][i];
					}

					feature.add(max);
				}
			}
		}

		return feature;
	}
	
	
	public static void normalizeFeatureL1(ArrayList<Double> sample)
	{
		int sum = 0;
		
		for (int i = 0; i<sample.size(); i++) {
			double val = sample.get(i);
			if (val < 0) val = -1 * val;

			sum += val;
		}

		for (int i = 0; i<sample.size(); i++) {
			double v;
			if (sum == 0) v = 0;
			else v = sample.get(i) / sum;// *100;

			sample.set(i, v);
		}
	}

	static double chiSquareDistance(ArrayList<Double> feature1, ArrayList<Double> feature2)
	{
		if(feature1.size() != feature2.size()) System.err.println("feature vector dimension mismatch.");

		double score = 0;

		for (int i = 0; i < feature1.size(); i++) {
			double h1 = feature1.get(i);
			double h2 = feature2.get(i);

			if (h1 < 0 || h2 < 0) {
				System.err.println("A negative feature value. The chi square kernel does not work with negative values. Please try shifting the vector to make all its elements positive.");
			}

			if (h1 == h2) continue;
			else score += (h1 - h2)*(h1 - h2) / (h1 + h2);
		}

		return 0.5 * score;
	}
	
	static double meanChiSquareDistances(ArrayList<FeatureVector> samples, int d)
	{
		double mean_dist = 0;

		double sum = 0;
		int count = 0;

		for (int i = 0; i < samples.size(); i++) {
			for (int j = i + 1; j < samples.size(); j++) {
				count++;

				sum += chiSquareDistance(samples.get(i).feature.get(d), samples.get(j).feature.get(d));
			}
		}

		mean_dist = sum / (double)count;

		return mean_dist;
	}

	static double kernelDistance(FeatureVector sample1, FeatureVector sample2, double[] mean_dists)
	{
		double distance = 0;

		for (int d = 0; d < sample1.numDim(); d++) {
			double weight = 1;

			double val = chiSquareDistance(sample1.feature.get(d), sample2.feature.get(d)) / mean_dists[d] * weight;
			if (mean_dists[d] == 0) val = chiSquareDistance(sample1.feature.get(d), sample2.feature.get(d)) / 1000000.0;

			distance = distance + val;
		}

		double final_score = Math.exp(-1 * distance / 10); // 10000 10
		
		return final_score;
	}
}

