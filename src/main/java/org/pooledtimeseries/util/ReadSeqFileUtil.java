package org.pooledtimeseries.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.hadoop.io.Text;
import org.pooledtimeseries.FeatureVector;
import org.pooledtimeseries.MeanChiSquareDistanceCalculation;
import org.pooledtimeseries.PoT;

public class ReadSeqFileUtil {
	private static final Logger LOG = Logger.getLogger(MeanChiSquareDistanceCalculation.class.getName());

	public static List<FeatureVector> computeFeatureFromSeries(Text value) {
		
		String[] videoVectors = value.toString().split(PoTConstants.FILE_SEPERATOR);
		ArrayList<double[]> tws = PoT.getTemporalWindows(4);
		ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();

		for (String video : videoVectors) {
			ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();

			long startIoTime = System.currentTimeMillis();
			String[] vectors = video.split(PoTConstants.VECTOR_SEPERATOR_REGEX);

			multiSeries.add(PoT.loadTimeSeries(new Scanner(vectors[0])));
			multiSeries.add(PoT.loadTimeSeries(new Scanner(vectors[1])));

			LOG.info("Read both series in - " + (System.currentTimeMillis() - startIoTime));

			FeatureVector fv = new FeatureVector();
			for (int i = 0; i < multiSeries.size(); i++) {
				fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 1));
				fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 2));
				fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 5));
			}
			fvList.add(fv);
		}

		return fvList;

	}
	
	public static String[] getFileNames(Text key){
		
		 return key.toString().split(PoTConstants.FILE_SEPERATOR);
	}


}
