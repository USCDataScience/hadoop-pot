package org.pooledtimeseries.seqfile;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

public class PoTVideoPathFilter extends Configured implements PathFilter{
	Configuration conf;
    FileSystem fs;
    
    @Override
	public boolean accept(Path path) {
		try {
			if (fs.isDirectory(path)) {
				return true;
			} else {
				//only accept files with mp4
				if (path.getName().endsWith(".mp4")) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
        if (conf != null) {
            try {
                fs = FileSystem.get(conf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
