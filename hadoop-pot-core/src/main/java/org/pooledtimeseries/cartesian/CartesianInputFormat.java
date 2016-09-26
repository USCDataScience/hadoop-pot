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

package org.pooledtimeseries.cartesian;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.join.CompositeInputSplit;
import org.apache.hadoop.util.ReflectionUtils;

public class CartesianInputFormat extends FileInputFormat {

	public static final Log LOG = LogFactory.getLog(CartesianInputFormat.class);

	public static final String LEFT_INPUT_FORMAT = "cart.left.inputformat";
	public static final String LEFT_INPUT_PATH = "cart.left.path";
	public static final String RIGHT_INPUT_FORMAT = "cart.right.inputformat";
	public static final String RIGHT_INPUT_PATH = "cart.right.path";

	public static void setLeftInputInfo(JobConf conf, Class<? extends FileInputFormat> inputFormat, String inputPath) {
		conf.set(LEFT_INPUT_FORMAT, inputFormat.getCanonicalName());
		conf.set(LEFT_INPUT_PATH, inputPath);
	}

	public static void setRightInputInfo(JobConf job, Class<? extends FileInputFormat> inputFormat, String inputPath) {
		job.set(RIGHT_INPUT_FORMAT, inputFormat.getCanonicalName());
		job.set(RIGHT_INPUT_PATH, inputPath);
	}

	@Override
	public InputSplit[] getSplits(JobConf conf, int numSplits) throws IOException {

		try {
			// Get the input splits from both the left and right data sets
			InputSplit[] leftSplits = getInputSplits(conf, conf.get(LEFT_INPUT_FORMAT), conf.get(LEFT_INPUT_PATH),
					numSplits);
			InputSplit[] rightSplits = getInputSplits(conf, conf.get(RIGHT_INPUT_FORMAT), conf.get(RIGHT_INPUT_PATH),
					numSplits);

			// Create our CartesianInputSplits, size equal to left.length *
			// right.length
			CompositeInputSplit[] returnSplits = new CompositeInputSplit[((leftSplits.length * (rightSplits.length - 1))
					/ 2) + leftSplits.length];

			int i = 0;
			// For each of the left input splits
			for (int leftLoop = 0; leftLoop < leftSplits.length; leftLoop++) {
				InputSplit left = leftSplits[leftLoop];
				// For each of the right input splits

				for (int rightLoop = leftLoop; rightLoop < rightSplits.length; rightLoop++) {
					InputSplit right = rightSplits[rightLoop];
					// Create a new composite input split composing of the two

					returnSplits[i] = new CompositeInputSplit(2);
					returnSplits[i].add(left);
					returnSplits[i].add(right);
					++i;
				}
			}

			// Return the composite splits
			LOG.info("Total splits to process: " + returnSplits.length);
			return returnSplits;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	@Override
	public RecordReader getRecordReader(InputSplit split, JobConf conf, Reporter reporter) throws IOException {
		// create a new instance of the Cartesian record reader
		return new CartesianRecordReader((CompositeInputSplit) split, conf, reporter);
	}

	private InputSplit[] getInputSplits(JobConf conf, String inputFormatClass, String inputPath, int numSplits)
			throws ClassNotFoundException, IOException {
		// Create a new instance of the input format
		FileInputFormat inputFormat = (FileInputFormat) ReflectionUtils.newInstance(Class.forName(inputFormatClass),
				conf);

		// Set the input path for the left data set
		inputFormat.setInputPaths(conf, inputPath);

		// Get the left input splits
		return inputFormat.getSplits(conf, numSplits);
	}
}
