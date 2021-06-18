package c2.crypto;

import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESEncryptor implements Encryptor{
	private byte[] iv;
	private byte[] key;
	private Cipher _Cipher;
	
	public AESEncryptor(byte[] iv, byte[] key) {
		this.iv = iv;
		this.key = key;
		try {
			_Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			//This can't happen with this algo
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			//This can't happen with this algo
		}
	}
	
	@Override
	public String encrypt(String strToEncrypt)
    {       
        try
        {   
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key, "AES");    
            _Cipher.init(Cipher.ENCRYPT_MODE, SecretKey, ivspec);       

            return new String(Base64.getEncoder().encode(_Cipher.doFinal(strToEncrypt.getBytes())));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

	@Override
    public String decrypt(String EncryptedMessage)
    {
        try
        {
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key, "AES");
            _Cipher.init(Cipher.DECRYPT_MODE, SecretKey, ivspec);           

            byte DecodedMessage[] = Base64.getDecoder().decode(EncryptedMessage);
            return new String(_Cipher.doFinal(DecodedMessage));

        }
        catch (Exception e)
        {
            //Keep going
        }
        return null;
    }
}
