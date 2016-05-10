package gov.nasa.jpl.memex.pooledtimeseries;
import java.io.File;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class DetectFaceDemo {
	  public static int frame_width = 320;
	  public static int frame_height = 240;
   public static void main( String[] args ) { 
	   File file = new File("D:\\DR:\\testing\\1.mp4");
      try {
         System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
         System.out.println(getGradientHistograms(file,5,5,8));
      } catch (Exception e) {
         System.out.println("Error: " + e.getMessage());
      }
   }
   static ArrayList<double[][][]> getGradientHistograms(File filename, int w_d,
		      int h_d, int o_d) throws PoTException{
	    ArrayList<double[][][]> histograms = new ArrayList<double[][][]>();
	    VideoCapture capture=null;
try{
	     capture = new VideoCapture();
}
catch(Exception e){System.out.println(e);}
	    capture.open(filename.toString());

	    if (!capture.isOpened()) {
	      System.out.println("video file not opened.");
	      
	      double[][][] hist = new double[w_d][h_d][o_d];
	      histograms.add(hist);
	    }
	    else {
		    // variables for processing images
		    Mat original_frame = new Mat();
		    Mat resized = new Mat();
		    Mat resized_gray = new Mat();
		
		    // initializing a list of histogram of gradients (i.e. a list of s*s*9
		    // arrays)
		    for (int i = 0;; i++) {
		      // capturing the video images
		      capture.read(original_frame);
		      if (original_frame.empty()) {

	            if (original_frame.empty()) {
	              if (i == 0) {
	                throw new PoTException("Could not read the video file");
	              }
	              else
	                break;
	            }

		      }
		
		      double[][][] hist = new double[w_d][h_d][o_d];
		
		      Imgproc.resize(original_frame, resized, new Size(frame_width,
		          frame_height));
		      Imgproc.cvtColor(resized, resized_gray, Imgproc.COLOR_BGR2GRAY);
		
		      histograms.add(hist);
		    }
		
		    capture.release();
	    }
	  
   return histograms;
   }
}