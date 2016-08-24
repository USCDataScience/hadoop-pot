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

package org.pooledtimeseries.seqfile;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.pooledtimeseries.util.PoTConstants;
import org.pooledtimeseries.util.PoTSerialiser;
import org.pooledtimeseries.util.ReadSeqFileUtil;

public class FullFileRecordReader extends RecordReader<Text, BytesWritable> {
	public static final byte[] VECTOR_SEPERATOR = PoTConstants.VECTOR_SEPERATOR.getBytes();
	
	private FileSplit fileSplit;
	private Configuration conf;
	private BytesWritable value = new BytesWritable();
	private Text key = new Text();

	private boolean processed = false;

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
		this.fileSplit = (FileSplit) split;
		this.conf = context.getConfiguration();
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (!processed) {

			String files[] = new String[2];
			files[0] = fileSplit.getPath().toString() + ".of.txt";
			files[1] = fileSplit.getPath().toString() + ".hog.txt";

			byte[] listFeatures = PoTSerialiser.getBytes(ReadSeqFileUtil.computeFeatureFromSeries(files) );
			
			value.set(listFeatures, 0, listFeatures.length );
			key.set(fileSplit.getPath().toString());
			processed = true;
			return true;
		}
		return false;
	}


	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public BytesWritable getCurrentValue() throws IOException, InterruptedException {
		return value;
	}

	@Override
	public float getProgress() throws IOException {
		return processed ? 1.0f : 0.0f;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}
}