package util;

import java.io.Flushable;
import java.io.IOException;

/**
 * Created by rhinigtassalvex on 08.01.17.
 */
public interface IChannel extends java.nio.channels.Channel, Flushable
{
    void println(String msg) throws IOException;


    void write(byte[] msg) throws IOException;


    String readLine() throws IOException;


    byte[] read() throws IOException;
}
