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

package gov.nasa.jpl.memex.pooledtimeseries.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import com.google.common.io.Files;

public class HadoopFileUtil {
	private static final Logger LOG = Logger.getLogger(HadoopFileUtil.class.getName());
	
	/**
	 * Copies file to a temporary directory and return File object to temporary file
	 */
	public File copyToTempDir(String value) throws IOException {
		Path videoPath = new Path(value.toString());
		videoPath.getFileSystem(new Configuration());
		
		LOG.info("Reading file from - " + videoPath);

		File tempDir = Files.createTempDir();

		// Get the filesystem - HDFS
		FileSystem fs = FileSystem.get(URI.create(value.toString()), new Configuration());

		// Open the path mentioned in HDFS
		FSDataInputStream in = null;
		OutputStream out = null;
		LOG.info("Copying file to a TempDir - " + tempDir.getPath());
		try {
			in = fs.open(videoPath);
			LOG.info("Available byte - " + in.available());
			out = new FileOutputStream(tempDir.getAbsolutePath() + "/" + videoPath.getName());
			IOUtils.copyBytes(in, out, new Configuration());
			
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error while copying to TempDir", e);
			return null;
		} finally {
			try {
				in.close();
				out.close();
			} catch (Exception e) {}
		}
		LOG.info("Available files - " + Arrays.asList(tempDir.listFiles()) );
		
		return new File(tempDir.getAbsolutePath() + "/" + videoPath.getName());
	}
}
