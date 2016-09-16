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

package org.pooledtimeseries.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.io.Text;
import org.pooledtimeseries.FeatureVector;
import org.pooledtimeseries.MeanChiSquareDistanceCalculation;
import org.pooledtimeseries.PoT;

public class ReadSeqFileUtil {
	private static final Logger LOG = Logger.getLogger(MeanChiSquareDistanceCalculation.class.getName());

	/**
	 * Takes HDFS path to time series and convert them to {@link FeatureVector}
	 * @param files - path to of.txt and hog.txt
	 * @return List of {@link FeatureVector}
	 */
	public static List<FeatureVector> computeFeatureFromSeries(String[] files) {

		ArrayList<double[]> tws = PoT.getTemporalWindows(4);
		ArrayList<FeatureVector> fvList = new ArrayList<FeatureVector>();

		ArrayList<double[][]> multiSeries = new ArrayList<double[][]>();

		long startIoTime = System.currentTimeMillis();

		try {
			multiSeries.add(PoT.loadTimeSeries(HadoopFileUtil.getInputStreamFromHDFS(files[0])) );
			multiSeries.add(PoT.loadTimeSeries(HadoopFileUtil.getInputStreamFromHDFS(files[1])) );
		} catch (IOException e) {
			LOG.log(Level.SEVERE,"Unable to read series from filesysytem ",e);
			throw new RuntimeException("Unable to read series from filesysytem",e);
		}

		LOG.info("Read both series in - " + (System.currentTimeMillis() - startIoTime));

		FeatureVector fv = new FeatureVector();
		for (int i = 0; i < multiSeries.size(); i++) {
			fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 1));
			fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 2));
			fv.feature.add(PoT.computeFeaturesFromSeries(multiSeries.get(i), tws, 5));
		}
		fvList.add(fv);

		return fvList;

	}

	public static String[] getFileNames(Text key) {

		return key.toString().split(PoTConstants.FILE_SEPERATOR);
	}

}
