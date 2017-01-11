package util;


import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.security.*;

/**
 * Created by Balthasar on 06.01.2017.
 */
public class AESChannel extends Channel
{
    private Cipher encryption, decryption;
    private Key key;
    private byte[] iv;

    public AESChannel(Channel baseChannel, Key key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException
    {
        super(baseChannel);
        this.key = key;
        this.iv = iv;

        encryption = Cipher.getInstance("AES/CTR/NoPadding");
        decryption = Cipher.getInstance("AES/CTR/NoPadding");
    }


    public void println(String msg) throws IOException
    {
        if (super.isOpen())
            try
            {
                baseChannel.write(encrypt(msg.getBytes()));
            }
            catch (InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | ClassNotFoundException | NoSuchPaddingException e)
            {
                throw new IOException("Error while encrypting message!", e);

            }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public String readLine() throws IOException
    {
        if (super.isOpen())
        {
            try
            {
                return new String( decrypt(baseChannel.read()));
            }
            catch (InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException | ClassNotFoundException e)
            {
                throw new IOException("Error while decrypting message!",e);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    /*private String encrypt(String msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException
    {
        //SecurityUtils.registerBouncyCastle();
        byte[] iv = SecurityUtils.secureRandomNumber();
        byte[] secureShit = "01234567012345670123456701234567".getBytes();
        Key key = new SecretKeySpec(secureShit, "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        //return new String(Base64.encode(cipher.doFinal(msg.getBytes())));
        return SecurityUtils.base64Encode(new String(cipher.doFinal(msg.getBytes())));
    }*/

    private byte[] encrypt(byte[] msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException
    {
        encryption.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return encryption.doFinal(msg);
    }


    /*private String decrypt(String msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException
    {
        //SecurityUtils.registerBouncyCastle();
        byte[] iv = SecurityUtils.secureRandomNumber();
        byte[] secureShit = "01234567012345670123456701234567".getBytes();
        Key key = new SecretKeySpec(secureShit, "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, SecurityUtils.generateAES_KEY(128), new IvParameterSpec(iv));

        //return new String(cipher.doFinal(Base64.decode(msg.getBytes())));
        return new String(cipher.doFinal(SecurityUtils.base64Decode((msg)).getBytes()));
    }*/

    private byte[] decrypt(byte[] msg) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException
    {
        decryption.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return decryption.doFinal(msg);
    }


}
