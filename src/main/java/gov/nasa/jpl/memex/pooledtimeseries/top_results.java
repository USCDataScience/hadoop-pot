package gov.nasa.jpl.memex.pooledtimeseries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class top_results {
	static File out_file;
	static ArrayList<Integer> value = new ArrayList<Integer>();
	static ArrayList<String> first = new ArrayList<String>();
	static ArrayList<String> second = new ArrayList<String>();
	static ArrayList<String> header = new ArrayList<String>();
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		out_file = new File("top_results.txt");
		if(out_file.exists())
			out_file.delete();
		File input = new File("formatted_similarity_calc.csv");
		BufferedReader br= new BufferedReader(new FileReader(input));
		String line = null;
		int count=0;
		while(null!=(line=br.readLine())){
			if(count==0){
				String head[]=line.split(",");
				for(int i=0;i<head.length;i++)
					header.add(head[i]);
			}
			else{
				String col[]=line.split(",");
				for(int i=1;i<col.length;i++){
					if(value.size()<10 && col[i]!="")
						value.add(Integer.parseInt(col[i]));
					else{
						
					}
				}
			}
			count++;
		}
		br.close();
	}

}
