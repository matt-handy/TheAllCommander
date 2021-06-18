package c2.crypto;

public class NullEncryptor implements Encryptor {

	@Override
	public String encrypt(String strToEncrypt) {
		return strToEncrypt;
	}

	@Override
	public String decrypt(String EncryptedMessage) {
		return EncryptedMessage;
	}

}
