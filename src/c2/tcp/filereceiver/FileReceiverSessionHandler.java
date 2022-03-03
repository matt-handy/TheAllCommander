package c2.tcp.filereceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import util.Time;

public class FileReceiverSessionHandler implements Runnable {

	private int counter;
	private static int threadInc = 1;

	private Socket socket;

	public static final int FILE_BUFFER_LENGTH = 50000;

	private boolean stayAlive = true;
	private CountDownLatch deathLatch = new CountDownLatch(1);

	private Path contentDir;

	public FileReceiverSessionHandler(Path contentDir, Socket socket) {
		this.contentDir = contentDir;
		this.socket = socket;
		counter = threadInc;
		threadInc++;
	}

	private boolean testIfXDataOnSocketBeforeTimeout(InputStream is, int desiredBytes) throws IOException {
		if (desiredBytes < 0) {
			throw new IOException(counter + ": Socket asking for impossible bytes: " + desiredBytes);
		}
		boolean available = true;
		// System.out.println(counter + ": Testing avail: " + desiredBytes);
		try {
			int idx = 0;
			while (is.available() < desiredBytes && idx < 10) {
				Time.sleepWrapped(200);
				idx++;
			}
			if (idx >= 10) {
				stayAlive = false;
				available = false;
			}
		} catch (IOException ex) {
			stayAlive = false;
			available = false;
		}

		// System.out.println(counter + ": Avail? " + available);
		return available;
	}

	@Override
	public void run() {
		try {
			InputStream is = socket.getInputStream();
			byte[] hostNameLengthBytes = is.readNBytes(4);// int for hostname length

			ByteBuffer hostNameLenBuffer = ByteBuffer.wrap(hostNameLengthBytes);
			int hostNameLength = hostNameLenBuffer.getInt();
			// System.out.println("Hostname len: " + hostNameLength);
			byte[] hostnameBytes = is.readNBytes(hostNameLength);

			String hostname = new String(hostnameBytes);
			Date dt = new Date();
			String timeStamp = dt.getTime() + "";

			Path rootPath = Paths.get(contentDir.toString(), hostname, timeStamp);

			// System.out.println(socket.getSoTimeout());
			while (stayAlive) {
				try {
					// TODO Add ability to restore session - implement on the client side
					if (!testIfXDataOnSocketBeforeTimeout(is, 4)) {
						continue;
					}
					byte[] fileNameLengthBytes = new byte[4];
					// System.out.println(counter + ": Reading FN");
					is.read(fileNameLengthBytes);// int for filename length
					// System.out.println(counter + ": Read FN");
					ByteBuffer nameLenBuffer = ByteBuffer.wrap(fileNameLengthBytes);
					int fileNameLength = nameLenBuffer.getInt();
					// System.out.println("Filename len: " + fileNameLength);
					if (!testIfXDataOnSocketBeforeTimeout(is, fileNameLength)) {
						continue;
					}
					// System.out.println(counter + ": Reading FN Len");
					byte[] filenameBytes = is.readNBytes(fileNameLength);
					// System.out.println(counter + ": Read FN Len");
					String filename = new String(filenameBytes);
					if (filename.equals("End of transmission")) {
						// System.out.println(counter + ": Shutting Down Receiver");
						stayAlive = false;
						continue;
					}
					if (!testIfXDataOnSocketBeforeTimeout(is, 8)) {
						continue;
					}
					// System.out.println(counter + ": Reading FCon");
					byte[] fileContentLengthBytes = is.readNBytes(8);// Long for file length
					// System.out.println(counter + ": Read FCon");
					ByteBuffer fileContentLengthBuffer = ByteBuffer.wrap(fileContentLengthBytes);
					long fileContentLength = fileContentLengthBuffer.getLong();
					// System.out.println(counter + ": Content len: " + fileContentLength);

					// When the socket is closed, often there is corruption in the fileContentLength
					// transmissions.
					// test for corruption.
					if (fileContentLength < 0 || fileContentLength > 100000000000L) {
						throw new IOException(counter + ": Socket asking for impossible bytes: " + fileContentLength);
					}

					Path localFilePath = null;

					Path filenamePath = Paths.get(filename);
					// System.out.println("Filename: " + filenamePath);

					Path remoteRootPath = filenamePath.getRoot();
					if (remoteRootPath == null) {
						// We're running on a non-Windows system
						String manuallyDeWindowsedFilename = filename.substring(3);
						manuallyDeWindowsedFilename = manuallyDeWindowsedFilename.replace("\\", "/");
						filenamePath = Paths.get(manuallyDeWindowsedFilename);
						localFilePath = Paths.get(rootPath.toString(), filenamePath.toString());
					} else {
						localFilePath = Paths.get(rootPath.toString(), remoteRootPath.relativize(filenamePath).toString());
					}
					Files.createDirectories(localFilePath.getParent());
					OutputStream output = Files.newOutputStream(localFilePath);

					long counter = 0;
					// System.out.println(counter + ": Enter file read");
					while (counter < fileContentLength && stayAlive) {
						int nextReadLength;
						if (fileContentLength - counter > FILE_BUFFER_LENGTH) {
							nextReadLength = FILE_BUFFER_LENGTH;
						} else {
							nextReadLength = (int) (fileContentLength - counter);
						}
						if (!testIfXDataOnSocketBeforeTimeout(is, nextReadLength)) {
							continue;
						}
						// System.out.println("Reading segment");
						byte[] nextSegment = is.readNBytes(nextReadLength);
						// System.out.println("Read segment");
						output.write(nextSegment);
						output.flush();
						counter += nextReadLength;
					}
					// System.out.println(counter + ": left file read");
					output.close();
				} catch (SocketTimeoutException ex) {
					stayAlive = false;
				}
			}

			socket.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		// System.out.println(counter + ": Breaking Loop");
		deathLatch.countDown();
	}

	public void kill() {
		// System.out.println(counter + ": Killing listener");
		stayAlive = false;
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// Ignore
		}
		// System.out.println(counter + ": Killed listener");
	}

	public boolean isDead() {
		return deathLatch.getCount() == 0;
	}

}
