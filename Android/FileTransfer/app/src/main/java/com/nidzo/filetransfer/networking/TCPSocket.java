package com.nidzo.filetransfer.networking;


import com.nidzo.filetransfer.FileTransferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TCPSocket {
    private static final int DEFAULT_CHUNK_SIZE = 16384;
    private TransferProgressHandler progressHandler=new TransferProgressHandler() {
        @Override
        public void handleProgress(double progress) {
            //Ignore by default
        }
    };
    private ServerSocket listenerSocket;
    private Socket socket;
    private String targetAddress;
    private int targetPort;
    private int timeout=5000;
    public TCPSocket()
    {

    }
    TCPSocket(Socket existingSocket) throws FileTransferException {
        socket=existingSocket;
        setTimeout(5000);
    }
    public void setTimeout(int t) throws FileTransferException{
        try {
            if(socket!=null) socket.setSoTimeout(t);
            timeout=t;
        } catch (SocketException e) {
            throw new FileTransferException("Networking error");
        }
    }
    public void connect(String address, int port)
    {
        targetAddress = address;
        targetPort=port;
    }
    public void send(final String data) throws FileTransferException {
        int dataLength = data.getBytes().length;
        String message = Integer.toString(dataLength) + "MLEN" + data;
        int bytesSent = 0;
        byte[] bytesToSend = message.getBytes();
        try {
            if (socket == null) {
                socket = new Socket(targetAddress, targetPort);
                setTimeout(timeout);
            }
            OutputStream socketStream = socket.getOutputStream();
            OutputStreamWithTimeout sendStream = new OutputStreamWithTimeout(socketStream);
            while (bytesSent < bytesToSend.length) {
                int chunkSize = DEFAULT_CHUNK_SIZE;
                if (bytesToSend.length - bytesSent < chunkSize)
                    chunkSize = bytesToSend.length - bytesSent;
                sendStream.write(bytesToSend, bytesSent, chunkSize, timeout);
                sendStream.flush();
                bytesSent += chunkSize;
                if (bytesToSend.length > DEFAULT_CHUNK_SIZE * 10)
                    progressHandler.handleProgress(((double) bytesSent) / ((double) bytesToSend.length));
            }
        } catch (IOException error) {
            throw new FileTransferException("Networking error " + error.toString() + "->" + error.getMessage());
        }
    }
    public void bind(int port) throws FileTransferException {
        try {
            listenerSocket = new ServerSocket(port);
        }
        catch (IOException error)
        {
            throw new FileTransferException("Networking error "+error.toString()+"->"+error.getMessage());
        }
    }
    public TCPSocket accept() throws FileTransferException{
        try {
            return new TCPSocket(listenerSocket.accept());
        }
        catch (IOException error)
        {
            throw new FileTransferException("Networking error "+error.toString()+"->"+error.getMessage());
        }
    }
    public String recv() throws FileTransferException {
        try {
            InputStream recvStream = socket.getInputStream();
            byte[] lengthPart = new byte[25];
            Boolean lengthKnown = false;
            int lengthPosition = 0;
            int length = 0;
            while (!lengthKnown) {
                if (lengthPosition == 25) {
                    throw new IOException("Length indicator too long");
                }
                lengthPosition += recvStream.read(lengthPart, lengthPosition, 1);
                String lengthString = new String(lengthPart);
                if (lengthString.contains("MLEN")) {
                    try {
                        length = Integer.parseInt(lengthString.substring(0, lengthString.indexOf("MLEN")));
                    } catch (NumberFormatException e) {
                        throw new IOException("Length indicator invalid");
                    }
                    lengthKnown = true;
                }
            }
            int receivedLength = 0;
            byte[] receivedMessage = new byte[length];
            while (receivedLength < length) {
                int chunkSize = DEFAULT_CHUNK_SIZE;
                if (chunkSize > length - receivedLength) chunkSize = length - receivedLength;
                receivedLength += recvStream.read(receivedMessage, receivedLength, chunkSize);
                if (length > DEFAULT_CHUNK_SIZE * 10)
                    progressHandler.handleProgress(((double) receivedLength) / ((double) length));
            }
            return new String(receivedMessage);
        }
        catch (IOException error)
        {
            throw new FileTransferException("Networking error "+error.toString()+"->"+error.getMessage());
        }
    }
    public String ipAddress()
    {
        return socket.getInetAddress().getHostAddress();
    }
    public void close()
    {
        try {
            if (listenerSocket != null) listenerSocket.close();
        }
        catch (IOException ignored)
        {

        }
    }

    public void setProgressHandler(TransferProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }
}
