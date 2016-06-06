package gov.nasa.jpl.memex.pooledtimeseries.healthcheck;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

public class CheckOpenCV {
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
		System.out.println("mat = " + mat.dump());

		String filename = args[0];

		System.out.println("opening video file " + filename);
		VideoCapture capture = new VideoCapture(filename.toString());

		if (!capture.isOpened()) {
			System.out.println("video file " + filename + " could not be opened.");

		}
	}
}
