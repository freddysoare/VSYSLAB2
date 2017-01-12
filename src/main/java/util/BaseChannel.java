package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.ClosedChannelException;

/**
 * Created by rhinigtassalvex on 07.01.17.
 */
public class BaseChannel extends Channel
{
    private PrintWriter out;
    private BufferedReader in;

    public BaseChannel(PrintWriter out)
    {
        setOpen(true);
        setBaseChannel(this);
        this.out = out;
    }

    public BaseChannel(BufferedReader in)
    {
        setOpen(true);
        setBaseChannel(this);
        this.in = in;
    }

    public BaseChannel(BufferedReader in, PrintWriter out)
    {
        setOpen(true);
        setBaseChannel(this);
        this.out = out;
        this.in = in;
    }

    @Override
    public void println(String msg) throws IOException
    {
        if (out != null && super.isOpen())
        {
            out.println(msg);
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public String readLine() throws IOException
    {
        if (in != null && super.isOpen())
        {
            return in.readLine();
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public void write(byte[] msg) throws IOException
    {
        if (out != null && super.isOpen())
        {
            out.println(new String(msg));
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public byte[] read() throws IOException
    {
        if (in != null && super.isOpen())
        {
            try
            {
                return in.readLine().getBytes();
            }
            catch (NullPointerException e)
            {
                return new byte[4];
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public void close() throws IOException
    {
        if (out != null)
        {
            out.close();
        }
        if (in != null)
        {
            in.close();
        }
    }

    @Override
    public void flush() throws IOException
    {
        if (out != null)
        {
            out.flush();
        }
    }

    public PrintWriter getOut()
    {
        return out;
    }

    public void setOut(PrintWriter out)
    {
        this.out = out;
    }

    public BufferedReader getIn()
    {
        return in;
    }

    public void setIn(BufferedReader in)
    {
        this.in = in;
    }
}
