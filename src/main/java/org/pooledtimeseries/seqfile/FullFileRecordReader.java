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
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.pooledtimeseries.util.PoTConstants;

public class FullFileRecordReader extends RecordReader<Text, Text> {
	public static final byte[] VECTOR_SEPERATOR = PoTConstants.VECTOR_SEPERATOR.getBytes();
	
	private FileSplit fileSplit;
	private Configuration conf;
	private Text value = new Text();
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

			Path files[] = new Path[2];
			files[0] = new Path(fileSplit.getPath().toString() + ".of.txt");
			files[1] = new Path(fileSplit.getPath().toString() + ".hog.txt");

			byte[] ofBytes = readBytesFromFile(files[0]);
			byte[] hogBytes = readBytesFromFile(files[1]);
			byte[] contents = new byte[ofBytes.length + hogBytes.length + VECTOR_SEPERATOR.length];
			
			System.arraycopy(ofBytes, 0, contents, 0, ofBytes.length);
			System.arraycopy(VECTOR_SEPERATOR, 0, contents, ofBytes.length, VECTOR_SEPERATOR.length);
			System.arraycopy(hogBytes, 0, contents, ofBytes.length + VECTOR_SEPERATOR.length, hogBytes.length);
			
			value.set(contents);
			key.set(fileSplit.getPath().toString());
			processed = true;
			return true;
		}
		return false;
	}

	private byte[] readBytesFromFile(Path path) throws IOException, InterruptedException {
		FileSystem fs = path.getFileSystem(conf);
		FSDataInputStream in = null;
		try {
			in = fs.open(path);
			
			byte[] contentFile = new byte[(int) fs.getContentSummary(path).getLength()];
			
			IOUtils.readFully(in, contentFile, 0, contentFile.length);
			return contentFile;
		} finally {
			IOUtils.closeStream(in);
		}

	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public Text getCurrentValue() throws IOException, InterruptedException {
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