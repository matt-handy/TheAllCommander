package c2.crypto;

public interface Encryptor {
	public String encrypt(String strToEncrypt);
	
	public String decrypt(String EncryptedMessage);
}
