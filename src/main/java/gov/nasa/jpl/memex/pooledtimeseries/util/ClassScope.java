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

package gov.nasa.jpl.memex.pooledtimeseries.util;

import java.util.Vector;
import java.util.logging.Logger;

public class ClassScope {
	private static java.lang.reflect.Field LIBRARIES;
	private static final Logger LOG = Logger.getLogger(ClassScope.class.getName());

	static {
		try {
			LIBRARIES = ClassLoader.class.getDeclaredField("loadedLibraryNames");
		} catch (Exception e) {
			LIBRARIES = null;
			e.printStackTrace();
		}
		LIBRARIES.setAccessible(true);
	}

	private static Vector<String> getLoadedLibraries(final ClassLoader loader) throws Exception {
		final Vector<String> libraries = (Vector<String>) LIBRARIES.get(loader);
		return libraries;
	}

	public static boolean isLibraryLoaded(String library) {
		try {
			final Vector<String> libraries = ClassScope.getLoadedLibraries(ClassLoader.getSystemClassLoader());
			LOG.info("Libraries found - " + libraries);
			return libraries.contains(library);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

}