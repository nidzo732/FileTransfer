package com.nidzo.filetransfer;

import android.content.Intent;

import com.nidzo.filetransfer.JSON.JSONHandling;
import com.nidzo.filetransfer.cryptography.DiffieHellman;
import com.nidzo.filetransfer.networking.MulticastDiscovery;
import com.nidzo.filetransfer.networking.TCPSocket;
import com.nidzo.filetransfer.networking.TransferProgressHandler;
import com.nidzo.filetransfer.transferclasses.File;
import com.nidzo.filetransfer.transferclasses.PairingRequest;
import com.nidzo.filetransfer.transferclasses.Peer;
import com.nidzo.filetransfer.transferclasses.PreSendRequest;
import com.nidzo.filetransfer.transferclasses.PublicKey;
import com.nidzo.filetransfer.transferclasses.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

public class Communicator {
    private MainActivity owner;
    private HashMap<String, Peer> peers;
    private MulticastDiscovery discovery;
    private TCPSocket listenerSocket;
    private Hashtable<String, PreSendRequest> preSendRegistrations;
    private FileHandling fileHandling;
    private Hashtable<String, RequestHandler> requestHandlers;
    private RequestHandler pairingHandler = new RequestHandler() {
        @Override
        public void handle(TCPSocket socket, Request request) throws FileTransferException {
            progressIndeterminate();
            final PairingRequest pairingRequest = new PairingRequest();
            JSONHandling.deserializeObject(request.getData(), pairingRequest);
            String secret = DialogBoxes.showInputBox(
                    "Pairing request",
                    "Pairing request from " + pairingRequest.getName() + " please enter shared password",
                    owner);
            if (secret == null || secret.equals("")) {
                progressStop();
                socket.send(Strings.RESPONSE_REJECT);
                return;
            }
            if (!pairingRequest.getPublicKey().verifySignature(secret)) {
                progressStop();
                notifyError("Shared passwords don't match");
                socket.send(Strings.RESPONSE_BAD_SIGNATURE);
                return;
            }
            String myPrivateKey = DiffieHellman.generatePrivate();
            String myPublicKey = DiffieHellman.calculatePublic(myPrivateKey);
            PublicKey returnedKey = new PublicKey(myPublicKey, secret);
            sendRequestToSocket(socket, Strings.REQYEST_TYPE_PUBLICKEY, JSONHandling.serializeObject(returnedKey), 5000);
            String response = socket.recv();
            progressStop();
            if (!response.equals(Strings.RESPONSE_OK)) {
                notifyError(response);
                return;
            }
            Peer newPeer;
            if (peers.containsKey(pairingRequest.getGuid())) {
                newPeer = peers.get(pairingRequest.getGuid());
            } else {
                newPeer = new Peer();
                peers.put(pairingRequest.getGuid(), newPeer);
            }
            newPeer.setIP(socket.ipAddress());
            newPeer.pair(secret, pairingRequest.getPublicKey().getKey(), myPrivateKey);
            storePeers();
            owner.updatePeerList();
        }
    };
    private RequestHandler preSendHandler = new RequestHandler() {
        @Override
        public void handle(TCPSocket socket, Request request) throws FileTransferException {
            if ((!peers.containsKey(request.getSenderGuid())) || (!peers.get(request.getSenderGuid()).isPaired())) {
                socket.send(Strings.RESPONSE_NOT_PAIRED);
                return;
            }
            PreSendRequest receivedRequest = new PreSendRequest();
            JSONHandling.deserializeObject(request.getData(), receivedRequest);
            if (!receivedRequest.getPublicKey().verifySignature(peers.get(request.getSenderGuid()).getSharedPassword())) {
                try {
                    socket.send(Strings.RESPONSE_BAD_SIGNATURE);
                } catch (FileTransferException ignored) {
                    return;
                }
            }
            progressIndeterminate();
            boolean acceptFile = DialogBoxes.showConfirmationBox("File received",
                    "Accept file '" + receivedRequest.getFileName() + "' from " + peers.get(request.getSenderGuid()).getName(),
                    owner);
            if (!acceptFile) {
                socket.send(Strings.RESPONSE_REJECT);
            } else {
                preSendRegistrations.remove(request.getSenderGuid());
                preSendRegistrations.put(request.getSenderGuid(), receivedRequest);
                socket.send(Strings.RESPONSE_OK);
            }
            progressStop();
        }
    };
    private RequestHandler sendHandler = new RequestHandler() {
        @Override
        public void handle(TCPSocket socket, Request request) throws FileTransferException {
            if ((!peers.containsKey(request.getSenderGuid())) || (!peers.get(request.getSenderGuid()).isPaired())) {
                notifyError("Not paired");
                progressStop();
                return;
            }
            if (!preSendRegistrations.containsKey(request.getSenderGuid())) {
                notifyError("No pre-send registration");
                progressStop();
                return;
            }
            progressIndeterminate();
            PreSendRequest registration = preSendRegistrations.get(request.getSenderGuid());
            preSendRegistrations.remove(request.getSenderGuid());
            Peer selectedPeer = peers.get(request.getSenderGuid());
            String aesKey = DiffieHellman.calculateAESKey(selectedPeer.getMyPrivateKey(), registration.getPublicKey().getKey());
            File receivedFile = new File();
            JSONHandling.deserializeObject(request.getData(), receivedFile);
            if (!receivedFile.getFileName().equals(registration.getFileName())) {
                notifyError("Name mismatch, rejected");
                progressStop();
                return;
            }
            byte[] fileContents = receivedFile.getFileContents(aesKey);
            java.io.File savedFile = fileHandling.saveFile(fileContents, receivedFile.getFileName());
            progressStop();
            if (DialogBoxes.showConfirmationBox("File received",
                    "File received and saved to downloads folder. Do you want to try to open it?", owner)) {
                owner.fileReceived(savedFile);
            }
        }
    };

    public Communicator(MainActivity ownerActivity) throws FileTransferException {
        owner = ownerActivity;
        fileHandling = new FileHandling(ownerActivity);
        loadPeers();
        discovery = new MulticastDiscovery(ownerActivity, this);
        preSendRegistrations = new Hashtable<>();
        setRequestHandlers();
        startRequestListener();
    }

    public void setRequestHandlers() {
        requestHandlers = new Hashtable<>();
        requestHandlers.put(Strings.REQUEST_TYPE_PAIR, pairingHandler);
        requestHandlers.put(Strings.REQUEST_TYPE_PRE_SEND, preSendHandler);
        requestHandlers.put(Strings.REQUEST_TYPE_SEND, sendHandler);
    }

    public void deletePeer(String guid) throws FileTransferException {
        peers.remove(guid);
        storePeers();
        owner.updatePeerList();
    }

    public void unpairPeer(String guid) throws FileTransferException {
        Peer peer = peers.get(guid);
        peer.unpair();
        storePeers();
        owner.updatePeerList();
    }

    private void storePeers() throws FileTransferException {
        try {
            HashMap<String, String> peersToStore = new HashMap<>();
            for (String peerKey : peers.keySet()) {
                peersToStore.put(peerKey, JSONHandling.serializeObject(peers.get(peerKey)));
            }
            JSONObject storedObject = new JSONObject(peersToStore);
            DataStorage.storeItem("PEERS_MAP", storedObject.toString(), owner);
        } catch (FileTransferException e) {
            throw new FileTransferException("Storage failure, the application might fail to work");
        }
    }

    private void loadPeers() throws FileTransferException {
        try {
            peers = new HashMap<>();
            if (DataStorage.getStoredItem("PEERS_MAP", owner) == null) return;
            JSONObject storedPeers = new JSONObject(DataStorage.getStoredItem("PEERS_MAP", owner));
            Iterator<String> peersIterator = storedPeers.keys();
            while (peersIterator.hasNext()) {
                String guid = peersIterator.next();
                Peer newPeer = new Peer();
                JSONHandling.deserializeObject(storedPeers.getString(guid), newPeer);
                peers.put(guid, newPeer);
            }
        } catch (JSONException e) {
            throw new FileTransferException("JSON Failure");
        }

    }

    public void halt() {
        discovery.halt();
        if (listenerSocket != null) listenerSocket.close();
    }

    private void progressIndeterminate() {
        owner.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    owner.progressIndeterminate();
                                }
                            }
        );
    }

    public void progressReport(final double progress) {
        owner.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    owner.progressReport(progress);
                                }
                            }
        );
    }

    public void progressStop() {
        owner.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    owner.progressStop();
                                }
                            }
        );
    }

    public void discoverPeers() {
        discovery.EmmitMulticast();
    }

    public void notifyError(final String errorMessage) {
        owner.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogBoxes.showMessageBox("Error", errorMessage, owner);
            }
        });
    }

    public void peerDiscovered(String guid, String name, String ip) {
        if (guid.equals(Identification.getGuid(owner))) return;
        if (peers.containsKey(guid)) {
            Peer modifiedPeer = peers.get(guid);
            modifiedPeer.setName(name);
            modifiedPeer.setIP(ip);
        } else {
            Peer newPeer = new Peer(guid, name);
            newPeer.setIP(ip);
            peers.put(guid, newPeer);
        }
        try {
            storePeers();
        } catch (FileTransferException e) {
            notifyError(e.getMessage());
        }
        owner.updatePeerList();
    }

    private void startRequestListener() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestReceiverThread();
            }
        }).start();
    }

    private void requestReceiverThread() {
        listenerSocket = new TCPSocket();
        try {
            listenerSocket.bind(32102);
        } catch (FileTransferException e) {
            notifyError("Networking system failed to start, the application might fail to work. " + e.getMessage());
            return;
        }
        while (true) {
            try {
                final TCPSocket receivedRequest = listenerSocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        requestHandlerThread(receivedRequest);
                    }
                }).start();
            } catch (FileTransferException ignored) {

            }
        }
    }

    private void requestHandlerThread(TCPSocket socket) {
        try {
            Request request = new Request();
            socket.setProgressHandler(new TransferProgressHandler() {
                @Override
                public void handleProgress(double progress) {
                    progressReport(progress);
                }
            });
            JSONHandling.deserializeObject(socket.recv(), request);
            if (requestHandlers.containsKey(request.getType())) {
                requestHandlers.get(request.getType()).handle(socket, request);
            }
        } catch (FileTransferException error) {
            progressStop();
            notifyError(error.getMessage());
        }
        catch (Exception error)
        {
            progressStop();
            notifyError("Unknown error "+error.getMessage()+" "+error.toString());
        }
    }

    private TCPSocket sendRequestToPeer(String guid, String type, String data, int timeout) throws FileTransferException {
        TCPSocket peerSocket = new TCPSocket();
        peerSocket.setProgressHandler(new TransferProgressHandler() {
            @Override
            public void handleProgress(double progress) {
                progressReport(progress);
            }
        });
        peerSocket.connect(peers.get(guid).getIP(), 32102);
        sendRequestToSocket(peerSocket, type, data, timeout);
        return peerSocket;
    }

    private void sendRequestToSocket(TCPSocket socket, String type, String data, int timeout) throws FileTransferException {
        Request sentRequest = new Request(type, data, Identification.getGuid(owner));
        socket.setTimeout(timeout);
        socket.send(JSONHandling.serializeObject(sentRequest));
    }

    public void pair(final String guid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                pairingThread(guid);
            }
        }).start();
    }

    private void pairingThread(String guid) {
        String secret = DialogBoxes.showInputBox("Pairing", "Please enter shared password", owner);
        try {
            progressIndeterminate();
            if (secret == null || secret.equals("")) {
                progressStop();
                return;
            }
            Peer peer = peers.get(guid);
            String myPrivateKey = DiffieHellman.generatePrivate();
            String myPublicKey = DiffieHellman.calculatePublic(myPrivateKey);
            PairingRequest request = new PairingRequest(Identification.getName(owner), Identification.getGuid(owner), new PublicKey(myPublicKey, secret));
            TCPSocket socket = sendRequestToPeer(guid, Strings.REQUEST_TYPE_PAIR, JSONHandling.serializeObject(request), 0);
            String response = socket.recv();
            if (response.equals(Strings.RESPONSE_REJECT) || response.equals(Strings.RESPONSE_BAD_SIGNATURE)) {
                progressStop();
                notifyError(response);
                return;
            }
            Request responseRequest = new Request();
            PublicKey peersKey = new PublicKey();
            JSONHandling.deserializeObject(response, responseRequest);
            JSONHandling.deserializeObject(responseRequest.getData(), peersKey);
            if (!peersKey.verifySignature(secret)) {
                notifyError("Bad shared password");
                progressStop();
                socket.send(Strings.RESPONSE_BAD_SIGNATURE);
                return;
            }
            socket.setTimeout(5000);
            socket.send(Strings.RESPONSE_OK);
            progressStop();
            peer.pair(secret, peersKey.getKey(), myPrivateKey);
            peer.setIP(socket.ipAddress());
            storePeers();
            owner.updatePeerList();
        } catch (FileTransferException e) {
            progressStop();
            notifyError(e.getMessage());
        }
    }

    public void sendFile(final String guid, final Intent openFileResult) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendFileThread(guid, openFileResult);
            }
        }).start();
    }

    private void sendFileThread(final String guid, final Intent openFileResult) {
        try {
            progressIndeterminate();
            byte[] fileContents = fileHandling.getFileContents(openFileResult);
            String fileName = fileHandling.getFileName(openFileResult);
            Peer selectedPeer = peers.get(guid);
            String filePrivateKey = DiffieHellman.generatePrivate();
            String filePublicKey = DiffieHellman.calculatePublic(filePrivateKey);
            PublicKey preSendKey = new PublicKey(filePublicKey, selectedPeer.getSharedPassword());
            PreSendRequest preSendRequest = new PreSendRequest(fileName, preSendKey);
            TCPSocket preSendSocket = sendRequestToPeer(guid, Strings.REQUEST_TYPE_PRE_SEND, JSONHandling.serializeObject(preSendRequest), 0);
            String response = preSendSocket.recv();
            if (!response.equals(Strings.RESPONSE_OK)) {
                progressStop();
                notifyError(response);
            }
            String aesKey = DiffieHellman.calculateAESKey(filePrivateKey, selectedPeer.getPublicKey());
            File sentFile = new File(fileName, fileContents, aesKey);
            String sentFileString = JSONHandling.serializeObject(sentFile);
            sendRequestToPeer(guid, Strings.REQUEST_TYPE_SEND, sentFileString, 5000);
        } catch (FileTransferException error) {
            notifyError(error.toString() + "->" + error.getMessage());
        }
        catch (OutOfMemoryError error)
        {
            notifyError("File too big to fit in memory");
        }
        progressStop();
    }

    public HashMap<String, Peer> getPeers() {
        return peers;
    }

    private interface RequestHandler {
        void handle(final TCPSocket socket, final Request request) throws FileTransferException;
    }
}
