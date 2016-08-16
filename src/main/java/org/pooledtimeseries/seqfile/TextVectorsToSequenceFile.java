package org.pooledtimeseries.seqfile;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class TextVectorsToSequenceFile extends Configured  {
	static class SequenceFileMapper extends
			Mapper<Text, Text, Text, Text> {
		private Text filename;

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			InputSplit split = context.getInputSplit();
			Path path = ((FileSplit) split).getPath();
			filename = new Text(path.toString());
		}

		@Override
		protected void map(Text key, Text value,
				Context context) throws IOException, InterruptedException {
			context.write(filename, value);
		}
	}

	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);
		job.setJarByClass(TextVectorsToSequenceFile.class);
		job.setJobName("smallfilestoseqfile");
		job.setInputFormatClass(FullFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		job.setNumReduceTasks(1);
		FullFileInputFormat.setInputPaths(job, new Path(args[0]));
		FileInputFormat.setInputPathFilter(job, PoTVideoPathFilter.class);
		
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
	    
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(SequenceFileMapper.class);
		job.waitForCompletion(true);
		
	}

	
}