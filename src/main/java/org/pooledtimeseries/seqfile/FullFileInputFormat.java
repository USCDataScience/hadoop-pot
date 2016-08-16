package org.pooledtimeseries.seqfile;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

public class FullFileInputFormat extends
		FileInputFormat<Text, BytesWritable> {
	@Override
	protected boolean isSplitable(JobContext context, Path file) {
		return false; 
	}

	@Override
	public RecordReader<Text, BytesWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
		FullFileRecordReader reader = new FullFileRecordReader();
		reader.initialize(split, context);
		return reader;
	}
}