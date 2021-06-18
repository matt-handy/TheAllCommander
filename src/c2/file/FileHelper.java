package c2.file;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileHelper {

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	// TODO: Migrate to Java Commons
	public static String getFileAsHex(String filename) throws IOException {
		File inputFile = new File(filename);
		InputStream is = new FileInputStream(inputFile);
		DataInputStream dis = new DataInputStream(is);
		byte[] data = new byte[(int) inputFile.length()];
		dis.read(data);
		dis.close();
		
		return bytesToHex(data);
	}
}
