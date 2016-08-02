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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;

public class FormatOutput {
	static ArrayList<String> sim = new ArrayList<String>();
	static File out_file;
	static int j=0;
	public static void main(String[] args) throws IOException {
		 out_file = new File(args[0]);//CSV file to write the output formatted_similarity_calc.csv
		if(out_file.exists())
			out_file.delete();
		Path in_file = Paths.get(args[1]);//Video pairs generated
		List<String> files =  java.nio.file.Files.readAllLines(in_file,
	            Charsets.UTF_8);
		int len = files.size();
		System.out.println(len);
		File input = new File(args[2]);// List of videos
		PrintWriter header = new PrintWriter(new FileWriter(out_file,true));
		BufferedReader br= new BufferedReader(new FileReader(input));
		String line = null,temp ="";
		files = new ArrayList<String>();
		header.print(",");
		while(null!=(line=br.readLine())){
			line = line.substring(line.lastIndexOf("\\")+1);
			
			files.add(line);
		}
		for(j=0;j<files.size();j++){
			header.print(files.get(j)+",");
			System.out.print(files.get(j)+",");
		}
		j=0;
		header.close();
		br.close();
		br= new BufferedReader(new FileReader(args[1]));// Video pairs generated
		
		int count=0;
		while(null!=(line=br.readLine())){
			String videos[]=line.split(",");
			if(!temp.equals(videos[0]) && count!=0){
				writeToFile(sim,files);
				sim.clear();
			}
			temp = videos[0];
			sim.add(videos[2]);
			count++;
		}
		writeToFile(sim,files);
		sim.clear();
		br.close();
	}
	public static void writeToFile(ArrayList<String> sim2, List<String> files) throws IOException{
		PrintWriter similarity = new PrintWriter(new FileWriter(out_file,true));
		similarity.println();
		System.out.println(sim2.size());
			
		similarity.print(files.get(j)+",");
		System.out.print(files.get(j)+",");
		j++;
		for(int i=1;i<j;i++)
			similarity.print(",");
		for(int i=0;i<sim2.size();i++){
			similarity.print(sim2.get(i)+",");
			System.out.print(sim2.get(i)+",");
		}
		similarity.close();
	}

}
