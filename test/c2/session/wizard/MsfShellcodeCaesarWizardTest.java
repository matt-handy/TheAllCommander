package c2.session.wizard;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import c2.session.wizard.MsfShellcodeCaesarWizard.MsfProcessingException;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class MsfShellcodeCaesarWizardTest {

	private final static String CANONICAL_PROCESSED_ARRAY = "new byte[64] = {0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x12, 0x13, 0x4C, 0x35, 0xD6, 0x69, 0x4C, 0x8F, 0x56, 0x64, 0x4C, 0x8F, 0x56, 0x1C, 0x4C, 0x45, 0xBE, 0xAA, 0x99, 0xC1, 0xA1, 0x03, 0xD9, 0x4C, 0x87, 0xC8, 0x2C, 0x40, 0x0A, 0x80, 0x0E, 0x84, 0xED, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF, 0x00, 0x01, 0x02, 0x03};";
	public final static Path TEST_DATA_FILE = Paths.get("test", "data", "msf_output", "sample_fake_msf_output");
	private final static Path TEST_CS_FILE = Paths.get("config", "pen300_study_tools", "msf_templates", "fake_program.cs");

	@Test
	void testCaesarShift() {
		try {
			byte[] msfcodeBytes = Files.readAllBytes(TEST_DATA_FILE);
			String toProcess = new String(msfcodeBytes);
			List<Short> bytePattern = MsfShellcodeCaesarWizard.processMsfVenom(toProcess, 4);
			assertEquals(64, bytePattern.size());
			assertEquals(4, (int) bytePattern.get(0));
			assertEquals(5, (int) bytePattern.get(1));
			assertEquals(6, (int) bytePattern.get(2));
			assertEquals(0xFE, (int) bytePattern.get(58));
			assertEquals(0xFF, (int) bytePattern.get(59));
			assertEquals(0, (int) bytePattern.get(60));
			assertEquals(1, (int) bytePattern.get(61));
			assertEquals(2, (int) bytePattern.get(62));
			assertEquals(3, (int) bytePattern.get(63));
		} catch (IOException ex) {
			ex.printStackTrace();
			fail("Cannot read test data for: " + this.getClass());
		} catch (MsfProcessingException e) {
			e.printStackTrace();
			fail("Cannot process MSF");
		}
	}

	@Test
	void testDumpToString() {
		try {
			byte[] msfcodeBytes = Files.readAllBytes(TEST_DATA_FILE);
			String toProcess = new String(msfcodeBytes);
			List<Short> bytePattern = MsfShellcodeCaesarWizard.processMsfVenom(toProcess, 4);
			String caesarStr = MsfShellcodeCaesarWizard.shortListToStringByteArray(bytePattern);
			assertEquals(CANONICAL_PROCESSED_ARRAY, caesarStr);
		} catch (IOException ex) {
			ex.printStackTrace();
			fail("Cannot read test data for: " + this.getClass());
		} catch (MsfProcessingException e) {
			e.printStackTrace();
			fail("Cannot process MSF");
		}
	}

	@Test
	void testCommandToArray() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			try {
				String caesarStr = MsfShellcodeCaesarWizard.generateCompilableCaesarEncodedTextFromMsf(
						"powershell -c \"Get-Content -Path " + TEST_DATA_FILE.toAbsolutePath().toString() + "\"", 4);
				assertEquals(CANONICAL_PROCESSED_ARRAY, caesarStr);
			} catch (MsfProcessingException e) {
				e.printStackTrace();
				fail("Cannot process MSF");
			}
		}
	}

	@Test
	void testCommandGivesReplacementFile() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			try {
				String rawFile = Files.readString(TEST_CS_FILE);
				String replacedFile = MsfShellcodeCaesarWizard.generateCompleteReplacementFile(
						"powershell -c \"Get-Content -Path " + TEST_DATA_FILE.toAbsolutePath().toString() + "\"", 4,
						TEST_CS_FILE);
				String[] rawLines = rawFile.split(System.lineSeparator());
				String[] replacedLines = replacedFile.split(System.lineSeparator());
				assertEquals(rawLines.length, replacedLines.length);
				assertEquals(12, rawLines.length);
				for (int idx = 0; idx < rawLines.length; idx++) {
					if (idx != 8) {
						assertEquals(rawLines[idx], replacedLines[idx]);
					}
				}
				assertEquals("		byte[] mah_payload = " + MsfShellcodeCaesarWizard.generateCompilableCaesarEncodedTextFromMsf(
						"powershell -c \"Get-Content -Path " + TEST_DATA_FILE.toAbsolutePath().toString() + "\"", 4), replacedLines[8]);
			} catch (MsfProcessingException e) {
				e.printStackTrace();
				fail("Cannot process MSF");
			} catch (IOException e) {
				e.printStackTrace();
				fail("Cannot read test data");
			}
		}
	}

}
