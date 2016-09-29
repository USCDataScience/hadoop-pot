package org.pooledtimeseries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class Deduplicate {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Improper usage. Execute with below 2 arguments- ");
			System.err.println("args[0] - path to CSV file to write the deduped similarity calc csv ");
			System.err.println("args[1] - Video pairs with similarity score- 'vid1,vid2\t0.5' ");
			throw new RuntimeException("Insufficient Input");
		}
		File outFile = new File(args[0]);// CSV file to write the output
											// deduped_similarity_calc.csv

		if (outFile.exists()) {
			outFile.delete();
		}

		File simFile = new File(args[1]);// Video pairs with similarity score

		// All videos to discard
		Set<String> videosToDelete = new HashSet<>();
		// One video from each similar set will be kept
		Set<String> videosToKeep = new HashSet<>();

		BufferedReader br = new BufferedReader(new FileReader(simFile));
		String simLine = null;
		while ((simLine = br.readLine()) != null) {
			String[] pairAndScore = simLine.split("\t");
			double score = Double.parseDouble(pairAndScore[1]);
			if (score >= 0.99d) {
				String[] pair = pairAndScore[0].split(",");
				String vid1 = pair[0];
				String vid2 = pair[1];
				if(vid1.equals(vid2)){
					continue;
				}
				boolean vid1InKeep = videosToKeep.contains(pair[0]);
				boolean vid2InKeep = videosToKeep.contains(pair[1]);
				boolean vid1InDelete = videosToDelete.contains(pair[0]);
				boolean vid2InDelete = videosToDelete.contains(pair[1]);

				if (vid1InDelete || vid2InDelete) {
					continue;
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
		br.close();

		// Write output in outFile
		PrintWriter similarity = new PrintWriter(new FileWriter(outFile, true));

		br = new BufferedReader(new FileReader(simFile));
		simLine = null;
		while ((simLine = br.readLine()) != null) {
			System.out.println(simLine);
			String[] pairAndScore = simLine.split("\t");
			String[] pair = pairAndScore[0].split(",");
			boolean vid1InDelete = videosToDelete.contains(pair[0]);
			boolean vid2InDelete = videosToDelete.contains(pair[1]);
			if (vid1InDelete || vid2InDelete) {
				continue;
			}else{
				similarity.println(simLine);
			}

		}
		br.close();
		similarity.close();

		System.out.println("Stored results in: " + outFile.getAbsolutePath());

	}
}
