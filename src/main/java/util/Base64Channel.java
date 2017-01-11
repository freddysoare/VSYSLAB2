package util;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

/**
 * Created by rhinigtassalvex on 07.01.17.
 */
public class Base64Channel extends Channel
{

    public Base64Channel(IChannel baseChannel)
    {
        super(baseChannel);
    }

    @Override
    public void println(String msg) throws IOException
    {
        if (super.isOpen())
        {
            baseChannel.println(SecurityUtils.base64Encode(msg));
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
            baseChannel.write(SecurityUtils.base64Encode(msg));
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
            return SecurityUtils.base64Decode(baseChannel.readLine());
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
            return SecurityUtils.base64Decode(baseChannel.read());
        }
        else
        {
            throw new ClosedChannelException();
        }
    }
}
