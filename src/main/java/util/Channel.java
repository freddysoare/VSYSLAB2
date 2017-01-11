package util;


import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * Created by rhinigtassalvex on 07.01.17.
 */
public abstract class Channel implements IChannel
{
    protected IChannel baseChannel;
    private boolean open;

    public Channel()
    {
        open = false;
        baseChannel = null;
    }

    public Channel(IChannel baseChannel)
    {
        this.baseChannel = baseChannel;
        open = baseChannel.isOpen();
    }

    @Override
    public void println(String msg) throws IOException
    {
        if (open)
        {
            baseChannel.println(msg);
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public String readLine() throws IOException
    {
        if (open)
        {
            return baseChannel.readLine();
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public void write(byte[] msg) throws IOException
    {
        if (open)
        {
            baseChannel.write(msg);
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public byte[] read() throws IOException
    {
        if (open)
        {
            return baseChannel.read();
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public void close() throws IOException
    {
        if (open)
        {
            baseChannel.close();
            open = false;
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public boolean isOpen()
    {
        return open;
    }

    @Override
    public void flush() throws IOException
    {
        if (open)
        {
            baseChannel.flush();
        }
    }

    protected void setOpen(boolean open)
    {
        this.open = open;
    }

    protected void setBaseChannel(Channel baseChannel)
    {
        this.baseChannel = baseChannel;
    }
}
