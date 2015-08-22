package com.nidzo.filetransfer.networking;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.nidzo.filetransfer.Communicator;
import com.nidzo.filetransfer.FileTransferException;
import com.nidzo.filetransfer.Identification;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastDiscovery {
    private static final String
        DEFAULT_MULTICAST_GROUP = "224.5.6.7",
        DEFAULT_MULTICAST_MESSAGE = "DISCOVERY";
    private static final int
        DEFAULT_MULTICAST_PORT = 32100,
        DEFAULT_DISCOVERY_PORT = 32101;
    private Context appContext;
    private Communicator owner;
    private TCPSocket listenerSocket;
    private WifiManager wifiManager;
    private WifiManager.MulticastLock multicastLock;
    private MulticastSocket receiverSocket;
    public MulticastDiscovery(Context context, Communicator ownerCommunicator)
    {
        appContext=context;
        owner=ownerCommunicator;
        wifiManager = (WifiManager)appContext.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("receiverLock");
        multicastLock.acquire();
        StartReceivingMulitcast();
        StartReceivingResponses();
    }
    public void halt()
    {
        multicastLock.release();
        if(listenerSocket!=null) listenerSocket.close();
        if(receiverSocket!=null) receiverSocket.close();
    }
    public void StartReceivingResponses()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                responseReceiverThread();
            }
        }).start();
    }
    public void EmmitMulticast()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                emmitMulticastThread();
            }
        }).start();
    }
    private void emmitMulticastThread()
    {
        try {
            InetAddress multicastGroup = InetAddress.getByName(DEFAULT_MULTICAST_GROUP);
            MulticastSocket senderSocket = new MulticastSocket(DEFAULT_MULTICAST_PORT);
            senderSocket.joinGroup(multicastGroup);
            byte[] sentBuffer = new byte[DEFAULT_MULTICAST_MESSAGE.length()];
            DatagramPacket sentPacket = new DatagramPacket(sentBuffer, sentBuffer.length, multicastGroup, DEFAULT_MULTICAST_PORT);
            sentPacket.setData(DEFAULT_MULTICAST_MESSAGE.getBytes());
            senderSocket.send(sentPacket);
        } catch (IOException ignored) {

        }
    }
    public void StartReceivingMulitcast()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                multicastReceiverThread();
            }
        }).start();
    }
    public void multicastReceiverThread() {
        try {
            InetAddress multicastGroup = InetAddress.getByName(DEFAULT_MULTICAST_GROUP);
            receiverSocket = new MulticastSocket(DEFAULT_MULTICAST_PORT);
            receiverSocket.joinGroup(multicastGroup);
            receiverSocket.setBroadcast(true);
            while (true)
            {
                try {
                    byte[] buffer = new byte[DEFAULT_MULTICAST_MESSAGE.length()];
                    final DatagramPacket packet = new DatagramPacket(buffer, DEFAULT_MULTICAST_MESSAGE.length());
                    receiverSocket.receive(packet);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            multicastResponderThread(packet);
                        }
                    }).start();
                }
                catch (IOException ignored)
                {

                }
            }

        } catch (IOException error) {
            owner.notifyError("Error starting networking system: " + error.getMessage());
        }
    }
    private void multicastResponderThread(DatagramPacket receivedPacket)
    {
        try {
            TCPSocket responderSocket = new TCPSocket();
            responderSocket.connect(receivedPacket.getAddress().getHostAddress(), DEFAULT_DISCOVERY_PORT);
            responderSocket.send(Identification.getGuid(appContext) + ":" + Identification.getName(appContext));
        } catch (FileTransferException ignored) {

        }
    }
    private void responseReceiverThread() {
        try {
            listenerSocket = new TCPSocket();
            listenerSocket.bind(DEFAULT_DISCOVERY_PORT);
            while(true)
            {
                try {
                    final TCPSocket receivedRequest = listenerSocket.accept();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            responseHandlerThread(receivedRequest);
                        }
                    }).start();
                }
                catch (FileTransferException ignored)
                {

                }

            }
        } catch (FileTransferException error) {
            owner.notifyError("Error starting networking system: "+error.getMessage());
        }
    }
    private void responseHandlerThread(TCPSocket receivedRequest)
    {
        try {
            final String receivedData =receivedRequest.recv();
            String[] details = receivedData.split(":");
            if(details.length!=2) return;
            String guid=details[0], name=details[1];
            owner.peerDiscovered(guid, name, receivedRequest.ipAddress());
        }
        catch (FileTransferException ignored)
        {

        }
    }
}
