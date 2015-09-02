package com.nidzo.filetransfer.networking;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamWithTimeout {
    private final Object lock;
    private OutputStream internalStream;
    private IOException exceptionRaised;
    private boolean done;

    public OutputStreamWithTimeout(OutputStream stream) {
        internalStream = stream;
        exceptionRaised = null;
        lock = new Object();
    }

    public void write(final byte[] data, final int start, final int length, int timeout) throws IOException {
        done = false;
        exceptionRaised = null;
        Thread writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    internalStream.write(data, start, length);
                } catch (IOException error) {
                    synchronized (lock) {
                        exceptionRaised = error;
                    }
                }
                synchronized (lock) {
                    done = true;
                }
            }
        });
        writerThread.start();
        try {
            writerThread.join(timeout);
            synchronized (lock) {
                if (done) {
                    if (exceptionRaised != null) throw exceptionRaised;
                } else throw new IOException("Network timeout");
            }
        } catch (InterruptedException e) {
            throw new IOException("Unknown error " + e.toString());
        }
    }

    public void flush() throws IOException {
        internalStream.flush();
    }
}
