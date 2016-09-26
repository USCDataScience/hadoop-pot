/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pooledtimeseries.util;

import java.util.logging.Logger;

import org.opencv.core.Core;
import org.pooledtimeseries.SimilarityCalculation;

public class PoTUtil {
	private static final String DEFAULT_LIB_PATH = "/mnt/apps/opencv-2.4.11/release/lib/libopencv_java2411.so";
	private static final Logger LOG = Logger.getLogger(SimilarityCalculation.class.getName());
	
	public static void loadOpenCV(String libraryPath){
		
		if (!ClassScope.isLibraryLoaded(Core.NATIVE_LIBRARY_NAME)) {
    		LOG.info("Trying to load - " + Core.NATIVE_LIBRARY_NAME);
    		try{
    			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    		}catch (java.lang.UnsatisfiedLinkError e){
    			System.load(libraryPath);
    		} 
    	}
	}
	
	public static void loadOpenCV(){
		loadOpenCV(DEFAULT_LIB_PATH);
	}
}
