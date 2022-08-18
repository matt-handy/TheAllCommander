package c2.crypto;

import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

public class AESEncryptor implements Encryptor {
	private byte[] key;
	private Cipher cipher;

	public AESEncryptor(byte[] key) {
		this.key = key;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			// This can't happen with this algo
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			// This can't happen with this algo
		}
	}

	@Override
	public String encrypt(String strToEncrypt) {
		try {
			Random random = new Random();
			byte iv[] = new byte[16];
			random.nextBytes(iv);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			Key SecretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, SecretKey, ivspec);

			
			byte[] encryptedPayload = cipher.doFinal(strToEncrypt.getBytes());
			byte[] c = new byte[iv.length + encryptedPayload.length];
			System.arraycopy(iv, 0, c, 0, iv.length);
			System.arraycopy(encryptedPayload, 0, c, iv.length, encryptedPayload.length);
			return new String(Base64.getEncoder().encode(c));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String decrypt(String encryptedMessage) {
		try {
			byte decodedMessage[] = Base64.getDecoder().decode(encryptedMessage);
			byte iv[] = Arrays.copyOf(decodedMessage, 16);
			byte message[] = Arrays.copyOfRange(decodedMessage, 16, decodedMessage.length);
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			Key SecretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, SecretKey, ivspec);

			if(message.length % 16 != 0) {
				System.out.println("Improper message: " + encryptedMessage);
			}
			
			String dMessage = new String(cipher.doFinal(message));
			return dMessage;

		} catch (Exception e) {
			e.printStackTrace();
			// Keep going
		}
		return null;
	}
}
