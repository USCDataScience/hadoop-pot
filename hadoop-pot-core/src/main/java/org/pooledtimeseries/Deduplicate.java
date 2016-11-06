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
package org.pooledtimeseries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Deduplicate {
	private static final double DEFAULT_THRESHOLD = 0.99d;

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Improper usage. Execute with below 2 arguments- ");
			System.err.println("args[0] - path to CSV file to write the deduped similarity calc csv ");
			System.err.println("args[1] - path to List of video names ");
			System.err.println("args[2] - Video pairs with similarity score- 'vid1,vid2\t0.5' ");
			System.err.println("args[3] - similarity threshold. Default 0.99 ");
			throw new RuntimeException("Insufficient Input");
		}
		File outFile = new File(args[0]);// CSV file to write the output deduped_similarity_calc.csv
		File outFileNames = new File(args[1]);// List of video names

		if (outFile.exists() || outFileNames.exists()) {
			throw new RuntimeException(String.format("Some output file already esists - %s , %s ", outFile.getAbsolutePath()
					, outFileNames.getAbsolutePath()) );
		}

		File simFile = new File(args[2]);// Video pairs with similarity score
		double threshold = args.length == 4 ? Double.parseDouble(args[3]) : DEFAULT_THRESHOLD;
		// All videos to discard
		Set<String> videosToDelete = new HashSet<>();
		// One video from each similar set will be kept
		Set<String> videosToKeep = new HashSet<>();

		BufferedReader br = new BufferedReader(new FileReader(simFile));
		String simLine = null;
		while ((simLine = br.readLine()) != null) {
			storeVideosToDelete(videosToDelete, videosToKeep, simLine, threshold);

		}
		br.close();

		// Write output in outFile
		PrintWriter similarity = new PrintWriter(new FileWriter(outFile, true));
		//reset videosToKeep for outputting video names
		videosToKeep = new HashSet<>();
		
		br = new BufferedReader(new FileReader(simFile));
		simLine = null;
		while ((simLine = br.readLine()) != null) {
			String[] pairAndScore = simLine.split("\t");
			String[] pair = pairAndScore[0].split(",");
			boolean vid1InDelete = videosToDelete.contains(pair[0]);
			boolean vid2InDelete = videosToDelete.contains(pair[1]);
			if (vid1InDelete || vid2InDelete) {
				continue;
			}else{
				videosToKeep.addAll(Arrays.asList(pair));
				similarity.println(simLine);
			}

		}
		br.close();
		similarity.close();
		
		PrintWriter listOfFile = new PrintWriter(new FileWriter(outFileNames, true));
		for (String videos : videosToKeep){
			listOfFile.println(videos);
		}
		listOfFile.close();
		
		System.out.println("Stored results in: " + outFile.getAbsolutePath());

	}

	private static void storeVideosToDelete(Set<String> videosToDelete, Set<String> videosToKeep, String simLine, double threshold) {
		String[] pairAndScore = simLine.split("\t");
		double score = Double.parseDouble(pairAndScore[1]);
		if (score >= threshold) {
			String[] pair = pairAndScore[0].split(",");
			String vid1 = pair[0];
			String vid2 = pair[1];
			if(vid1.equals(vid2)){
				return;
			}
			boolean vid1InKeep = videosToKeep.contains(pair[0]);
			boolean vid2InKeep = videosToKeep.contains(pair[1]);
			boolean vid1InDelete = videosToDelete.contains(pair[0]);
			boolean vid2InDelete = videosToDelete.contains(pair[1]);

			if (vid1InDelete || vid2InDelete) {
				return;
			}
			// None of the video is kept
			if (!vid1InKeep && !vid2InKeep) {
				videosToKeep.add(vid1);
				videosToDelete.add(vid2);
			} else if (vid1InKeep && vid2InKeep) {// Both of the video are kept
				videosToDelete.add(vid1);// delete any one of them
				videosToKeep.remove(vid1);
			} else if (vid1InKeep) { // Only vid1 is in keep
				videosToDelete.add(vid2);
			} else if (vid2InKeep) { // Only vid2 is in keep
				videosToDelete.add(vid1);
			}

		}
	}
}
