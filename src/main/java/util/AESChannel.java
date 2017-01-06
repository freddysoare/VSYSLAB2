package util;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.PrintWriter;
import java.security.*;

/**
 * Created by Balthasar on 06.01.2017.
 */
public class AESChannel {

    private Cipher cipher;
    private PrintWriter out;

    public AESChannel(PrintWriter out) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.out = out;
        cipher = Cipher.getInstance("AES/CTR/NoPadding");
    }

    public void println(String msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException {
        out.println(encrypt(msg));
        out.flush();
    }

    public String encrypt(String msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException {
        byte[] iv = "sechzehnbyteslol".getBytes();
        byte[] secureShit = "01234567012345670123456701234567".getBytes();
        Key key = new SecretKeySpec(secureShit, "AES");

        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(Base64.encode(cipher.doFinal(msg.getBytes())));
    }

    public String decrypt(String msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, Base64DecodingException {
        byte[] iv = "sechzehnbyteslol".getBytes();
        byte[] secureShit = "01234567012345670123456701234567".getBytes();
        Key key = new SecretKeySpec(secureShit, "AES");

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(cipher.doFinal(Base64.decode(msg.getBytes())));
    }

}
