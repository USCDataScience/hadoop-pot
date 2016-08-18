package org.pooledtimeseries.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PoTSerialiser {

	private static final Logger LOG = Logger.getLogger(PoTSerialiser.class.getName());

	public static byte[] getBytes(Object value) {
		long start = System.currentTimeMillis();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] byteArr = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(value);
			byteArr = bos.toByteArray();
			LOG.fine("Time taken serializing - " + (System.currentTimeMillis() - start));
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unable to serialize", e);
			
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception ex) {
				// ignore close exception
			}
			try {
				bos.close();
			} catch (Exception ex) {
				// ignore close exception
			}
		}

		return byteArr;
	}
	
	public static Object getObject(byte[] byteArr) {
		 
		if(byteArr == null || byteArr.length == 0){
			return null;
		}
		long start = System.currentTimeMillis();
		ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			LOG.fine("Time taken deserializing - " + (System.currentTimeMillis() - start));
			return in.readObject();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unable to deserialize", e);
			return null;
		} finally {
			try {
				bis.close();
			} catch (Exception ex) {
				// ignore close exception
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception ex) {
				// ignore close exception
			}
		}

	}
	
}
