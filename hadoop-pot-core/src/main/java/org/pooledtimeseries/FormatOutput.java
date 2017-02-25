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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

import com.google.common.base.Charsets;

public class FormatOutput {
	
	/**
	 * Sample output-
	 * ,1.mp4, 2.mp4, 3.mp4,<br/>
	 * 1.mp4, 1.0, 0.677986882429, 0.514423983869,<br/>
	 * 2.mp4, , 1.0, 0.12525353988,<br/>
	 * 3.mp4, , , 1.0,<br/>
	 * 
	 * @param args <br/>
	 * args[0] - path to CSV file to write the formatted similarity calc csv<br/>
	 * args[1] - Video pairs with similarity score<br/>
	 * args[2] - List of all videos<br/>
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Improper usage. Execute with below 3 arguments- ");
			System.err.println("args[0] - path to CSV file to write the formatted similarity calc csv ");
			System.err.println("args[1] - Video pairs with similarity score- 'vid1,vid2\t0.5' ");
			System.err.println("args[2] - List of all videos ");
			throw new RuntimeException("Insufficient Input");
		}
		File outFile = new File(args[0]);//CSV file to write the output formatted_similarity_calc.csv
		if (outFile.exists()) {
			throw new RuntimeException("Output file already exists-" + outFile.getAbsolutePath());
		}
		
		File simFile = new File(args[1]);//Video pairs with similarity score
		
		Path inputList = Paths.get(args[2]);// List of all videos
		
		List<String> videoList = Files.readAllLines(inputList, Charsets.UTF_8);
		//adding a blank at first position to match output
		videoList.add(0,"");
		//Result is a 2D square matrix of size video count + 1
		//additional 1 is for storing video file name
		String[][] resultMatrix = new String [videoList.size()][videoList.size()];
		System.out.println("Initialised input files and resultMatrix");
		
		//init first row with just video name
		resultMatrix[0]=videoList.toArray(new String[videoList.size()]);
		//init first col with just video name
		for (int i=1;i<videoList.size();i++){
			resultMatrix[i][0]=videoList.get(i);
		}
		
		//Fill all scores from simFile to resultMatrix
		BufferedReader br = new BufferedReader(new FileReader(simFile));
		String simLine = null;
		while ((simLine = br.readLine()) != null) {
			fillSimLineInResult(simLine,resultMatrix,videoList);
		}
		br.close();
		
		//Write output in outFile
		PrintWriter similarity = new PrintWriter(new FileWriter(outFile,true));

		for(String[] resultRow: resultMatrix){
			StringBuffer sb = new StringBuffer("");
			for(String resultCell: resultRow){
				//if resultCell == null print empty string else value
				sb.append((resultCell == null?"":resultCell) +",");
			}
			similarity.print(sb.substring(0, sb.length()-1));
			similarity.println();
			
		}
		similarity.close();
		
		System.out.println("Stored results in: " + outFile.getAbsolutePath());
	}

	/**
	 * Store similarity score for a pair at correct place in resultMatrix 
	 * @param simLine
	 * @param resultMatrix
	 * @param videoList
	 */
	private static void fillSimLineInResult(String simLine, String[][] resultMatrix, List<String> videoList) {

		DecimalFormat df = new DecimalFormat("0.00");

		String score = "";
		
		int indexOfvid1 = 0;
		int indexOfvid2 = 0;

		{
			// scoped under a brace to limit scope of temp variables
			String[] pairAndScore = simLine.split("\t");
			
			score = df.format(Double.parseDouble(pairAndScore[1]) );
			String[] pair = pairAndScore[0].split(",");
			indexOfvid1 = videoList.indexOf(pair[0]);
			indexOfvid2 = videoList.indexOf(pair[1]);
		}
		
		//if this video is not present in input list of video skip it from matrix
		//This is used when we create output for a subset of videos
		if(indexOfvid2 == -1 || indexOfvid1 == -1) 
			return;
		
		//Fill only upper matrix
		if (indexOfvid1 < indexOfvid2) {
			resultMatrix[indexOfvid1][indexOfvid2]=score;
		} else {
			//equal score will be one anyway
			resultMatrix[indexOfvid2][indexOfvid1]=score;
		}

	}
	
}
