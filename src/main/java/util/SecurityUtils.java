package util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}

	public static String base64Encode(String message)
	{
		if (message != null)
		{
			return new String(Base64.encode(message.getBytes()));
		}
		else
		{
			return null;
		}
	}

	public static byte[] base64Encode(byte[] message)
	{
		return Base64.encode(message);
	}

	public static String base64Decode(String message)
	{
		if (message != null)
		{
			return new String(Base64.decode(message.getBytes()));
		}
		else
		{
			return null;
		}
	}

	public static byte[] base64Decode(byte[] message)
	{
		return Base64.decode(message);
	}


	public static byte[] secureRandomNumber(Integer size)
	{
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[size];
		secureRandom.nextBytes(number);
		return number;
	}

	public static String createHMAC(String message, Key hmac_key) throws InvalidKeyException, NoSuchAlgorithmException
	{
		Mac hMac = Mac.getInstance("HmacSHA256");
		hMac.init(hmac_key);
		// MESSAGE is the message to sign in bytes
		hMac.update(message.getBytes());
		return new String(hMac.doFinal());
	}

	public static boolean check_HMAC(String message, String HMAC, Key hmac_key) throws NoSuchAlgorithmException, InvalidKeyException
	{
		return createHMAC(message,hmac_key).equals(HMAC);
	}

	public static Key generateAES_KEY(Integer KEYSIZE) throws NoSuchAlgorithmException, IOException, ClassNotFoundException
	{
		KeyGenerator generator = KeyGenerator.getInstance("AES");
		// KEYSIZE is in bits
		generator.init(KEYSIZE);


		SecretKey secretKey = generator.generateKey();
		/*File f = new File("secret.kay");
		if (f.createNewFile())
		{
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
			os.writeObject(secretKey);
			os.close();
		}
		else
		{
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
			Object readObject = is.readObject();
			secretKey = (SecretKey)readObject;
		}*/
		return secretKey;
	}
}
