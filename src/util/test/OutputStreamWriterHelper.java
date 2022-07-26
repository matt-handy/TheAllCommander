package util.test;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class OutputStreamWriterHelper {

	public static void writeAndSend(OutputStreamWriter bw, String message) throws IOException{
		bw.write(message + System.lineSeparator());
		bw.flush();
	}
}
