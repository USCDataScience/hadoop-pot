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