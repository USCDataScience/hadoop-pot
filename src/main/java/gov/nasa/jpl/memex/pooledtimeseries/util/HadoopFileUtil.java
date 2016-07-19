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
import org.apache.hadoop.io.Text;

import com.google.common.io.Files;

public class HadoopFileUtil {
	private static final Logger LOG = Logger.getLogger(HadoopFileUtil.class.getName());
	
	/**
	 * Copies file to a temporary directory and return File object to temporary file
	 */
	public File copyToTempDir(String value) throws IOException {
		Path videoPath = new Path(value.toString());
		LOG.info("Reading video file from - " + videoPath);

		File tempDir = Files.createTempDir();

		// Get the filesystem - HDFS
		FileSystem fs = FileSystem.get(URI.create(value.toString()), new Configuration());

		// Open the path mentioned in HDFS
		FSDataInputStream in = null;
		OutputStream out = null;
		LOG.info("Copying video to a TempDir - " + tempDir.getPath());
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
		LOG.info("Available videos - " + Arrays.asList(tempDir.listFiles()) );
		
		return new File(tempDir.getAbsolutePath() + "/" + videoPath.getName());
	}
}
