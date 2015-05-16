package pooled_time_series;

import java.util.ArrayList;

public class FeatureVector {
	public ArrayList<ArrayList<Double>> feature;
	
	public FeatureVector() {
		feature = new ArrayList<ArrayList<Double>>();
	}
	
	public FeatureVector(ArrayList<ArrayList<Double>> f) {
		feature = f;
	}
	
	public int numDim() {
		return feature.size();
	}
}
