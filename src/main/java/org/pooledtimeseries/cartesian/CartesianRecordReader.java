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

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.join.CompositeInputSplit;
import org.apache.hadoop.util.ReflectionUtils;

public class CartesianRecordReader<K1, V1, K2, V2> implements RecordReader<Text, Text> {

	// Record readers to get key value pairs
	private RecordReader leftRR = null, rightRR = null;

	// Store configuration to re-create the right record reader
	private FileInputFormat rightFIF;
	private JobConf rightConf;
	private InputSplit rightIS;
	private Reporter rightReporter;
	// if left and right are same splits this flag is set
	// It's used to avoid repeated pairs
	// for l=1,2 r =1,2 pair=11,12,22
	private boolean pairWithItself;

	// Helper variables
	private K1 lkey;
	private V1 lvalue;
	private K2 rkey;
	private V2 rvalue;
	private boolean goToNextLeft = true, alldone = false;
	private int rightShiftCount = 1;

	/**
	 * Creates a new instance of the CartesianRecordReader
	 * 
	 * @param split
	 * @param conf
	 * @param reporter
	 * @throws IOException
	 */
	public CartesianRecordReader(CompositeInputSplit split, JobConf conf, Reporter reporter) throws IOException {
		this.rightConf = conf;
		this.rightIS = split.get(1);
		this.rightReporter = reporter;

		try {
			// Create left record reader
			FileInputFormat leftFIF = (FileInputFormat) ReflectionUtils
					.newInstance(Class.forName(conf.get(CartesianInputFormat.LEFT_INPUT_FORMAT)), conf);

			leftRR = leftFIF.getRecordReader(split.get(0), conf, reporter);

			// Create right record reader
			rightFIF = (FileInputFormat) ReflectionUtils
					.newInstance(Class.forName(conf.get(CartesianInputFormat.RIGHT_INPUT_FORMAT)), conf);

			rightRR = rightFIF.getRecordReader(rightIS, rightConf, rightReporter);
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			throw new IOException(e);
		}

		// Create key value pairs for parsing
		lkey = (K1) this.leftRR.createKey();
		lvalue = (V1) this.leftRR.createValue();

		rkey = (K2) this.rightRR.createKey();
		rvalue = (V2) this.rightRR.createValue();
	}

	public Text createKey() {
		return new Text();
	}

	public Text createValue() {
		return new Text();
	}

	public long getPos() throws IOException {
		return leftRR.getPos();
	}

	public boolean next(Text key, Text value) throws IOException {
		
		do {
			// If we are to go to the next left key/value pair
			if (goToNextLeft) {
				// Read the next key value pair, false means no more pairs
				if (!leftRR.next(lkey, lvalue)) {
					// If no more, then this task is nearly finished
					alldone = true;
					break;
				} else {
					// If we aren't done, set the value to the key and set
					// our flags
					goToNextLeft = alldone = false;

					// Reset the right record reader
					this.rightRR = this.rightFIF.getRecordReader(this.rightIS, this.rightConf, this.rightReporter);
				}

				if (this.pairWithItself) {
					// shifting right data set to avoid repeated pairs
					// we consider a,b == b,a
					for (int i = 0; i < rightShiftCount; i++) {
						rightRR.next(rkey, rvalue);
					}
					rightShiftCount++;
				}
			}

			// Read the next key value pair from the right data set
			if (rightRR.next(rkey, rvalue)) {
				// If success, set key and value for left and right splits
				key.set(lkey.toString() + "~" + rkey.toString());
				value.set(lvalue.toString() + "~" + rvalue.toString());
				// This assumes that key will always be unique among all splits
				if (lkey.toString().equals(rkey.toString())) {
					this.pairWithItself = true;
				}
			} else {
				// Otherwise, this right data set is complete
				// and we should go to the next left pair
				goToNextLeft = true;
			}

			// This loop will continue if we finished reading key/value
			// pairs from the right data set
		} while (goToNextLeft);

		if (alldone) {
			// reset shift counter
			rightShiftCount = 1;
			this.pairWithItself = false;
		}
		// Return true if a key/value pair was read, false otherwise
		return !alldone;
	}

	public void close() throws IOException {
		leftRR.close();
		rightRR.close();
	}

	public float getProgress() throws IOException {
		return leftRR.getProgress();
	}
}
