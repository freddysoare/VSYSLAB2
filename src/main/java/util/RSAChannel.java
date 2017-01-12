package util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by rhinigtassalvex on 07.01.17.
 */
public class RSAChannel extends Channel
{
    private Cipher encryption, decryption;
    private Key encryptionKey;
    private Key decryptionKey;

    public RSAChannel(Channel baseChannel, Key encryptionKey, Key decryptionKey) throws NoSuchPaddingException, NoSuchAlgorithmException
    {
        super(baseChannel);
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey;

        SecurityUtils.registerBouncyCastle();

        encryption = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
        decryption = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");

    }

    @Override
    public void println(String msg) throws IOException
    {
        if (super.isOpen())
        {
            try
            {
                baseChannel.write(encrypt(msg.getBytes()));
            }
            catch (BadPaddingException | InvalidKeyException | IllegalBlockSizeException e)
            {
                throw new IOException("Error while encrypting message!",e);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public void write(byte[] msg) throws IOException
    {
        if (super.isOpen())
        {
            try
            {
                baseChannel.write(encrypt(msg));
            }
            catch (BadPaddingException | InvalidKeyException | IllegalBlockSizeException e)
            {
                throw new IOException("Error while encrypting message!",e);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    private byte[] encrypt(byte[] msg) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException
    {
        if (encryptionKey != null)
        {
            encryption.init(Cipher.ENCRYPT_MODE, encryptionKey);
            return encryption.doFinal(msg);
        }
        else throw new InvalidKeyException("Key null");
    }

    @Override
    public String readLine() throws IOException
    {
        if (super.isOpen())
        {
            try
            {
                return new String(decrypt(baseChannel.read()));
            }
            catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
            {
                throw new IOException("Error while decrypting message!",e);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public byte[] read() throws IOException
    {
        if (super.isOpen())
        {
            try
            {
                return decrypt(baseChannel.read());
            }
            catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
            {
                throw new IOException("Error while decrypting message!",e);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    private byte[] decrypt(byte[] msg) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        if (decryptionKey != null)
        {
            decryption.init(Cipher.DECRYPT_MODE, decryptionKey);
            return decryption.doFinal(msg);

        }
        else throw new InvalidKeyException("Key null");
    }

    public void setEncryptionKey(Key encryptionKey)
    {
        this.encryptionKey = encryptionKey;
    }

    public void setDecryptionKey(Key decryptionKey)
    {
        this.decryptionKey = decryptionKey;
    }
}
