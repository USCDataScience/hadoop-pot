package gov.nasa.jpl.memex.pooledtimeseries;
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

public class Formatoutput {
	static ArrayList<String> sim = new ArrayList<String>();
	static File out_file = new File("D:\\DR\\testing\\formatted_similarity_calc.csv");
	static int j=0;
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		if(out_file.exists())
			out_file.delete();
		Path in_file = Paths.get("D:\\DR\\testing\\videos.txt");
		List<String> files =  java.nio.file.Files.readAllLines(in_file,
	            Charsets.UTF_8);
		int len = files.size();
		System.out.println(len);
		File input = new File("D:\\DR\\testing\\original_videos.txt");
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
		//System.out.println();
		//header.println();
		header.close();
		br.close();
		br= new BufferedReader(new FileReader("D:\\DR\\testing\\videos.txt"));
		
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
		/*if(j!=0)
			System.out.println();*/

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
		}/*
		if(j>0){
		similarity.println();
		System.out.println();
		}*/
		if(j==7){
			for(int i=0;i<j-1;i++)
				similarity.print(",");
			similarity.print("1");
		}
		similarity.close();
	}

}
