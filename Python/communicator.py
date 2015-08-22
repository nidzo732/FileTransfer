import threading
from PyQt4 import QtCore
from threading import Thread
from PyQt4.QtCore import QSettings, QObject
from cryptography import generateDHPrivate, calculateDHPublic, calculateDHAES
from dialogboxes import askForInput, askForConfirmation
import dialogboxes
from exception import FileTransferBaseException
from identification import getStoredName, getStoredGuid, DEFAULT_NAME
from jsonhandling import *
import json
from multicast import UDPDiscovery
from networking import TCPSocket
from storage import *
from transferclasses import Peer, Request, PairingRequest, PublicKey, File, PreSendRequest
from strings import *

storedPeers = QSettings("FileTransfer", "peers")


class Communicator(QObject):
    requestReceivedSignal = QtCore.pyqtSignal(TCPSocket, bytes)
    peersUpdated = QtCore.pyqtSignal()
    fileReceived = QtCore.pyqtSignal(str, bytes)
    showMessageBox = QtCore.pyqtSignal(str)
    continueSend = QtCore.pyqtSignal(str, bytes, str)

    def __init__(self, parent):
        QObject.__init__(self)
        self.name = getStoredName() or DEFAULT_NAME
        self.guid = getStoredGuid()
        self.peers = self.getStoredPeers()
        self.preSendRegistrations = {}
        self.handlerMap = {}
        self.initHandlerMap()
        self.startRequestListener()
        self.requestReceivedSignal.connect(self.requestReceived)
        self.discoveryManger = UDPDiscovery()
        self.discoveryManger.peerDiscovered.connect(self.peerDiscovered)
        self.discoveryManger.peerDiscovered.connect(self.peerDiscovered)
        self.showMessageBox.connect(self.showMessageBoxSlot)
        self.discoveryManger.startDiscoveryServer()
        self.parent = parent
        self.continueSend.connect(self.continueSendSlot)

    def initHandlerMap(self):
        self.handlerMap[REQUEST_TYPE_PAIR] = self.handlePairRequest
        self.handlerMap[REQUEST_TYPE_PRE_SEND] = self.handlePreSendRequest
        self.handlerMap[REQUEST_TYPE_SEND] = self.receiveFile

    def continueSendSlot(self, fileName, fileContents, guid):
        self.sendFile(fileName, fileContents, guid)

    def showMessageBoxSlot(self, message):
        self.progressStop()
        dialogboxes.showMessageBox(self.parent, message)

    def progressRepport(self, value):
        self.parent.progressSet.emit(value)

    def progressIndeterminate(self):
        self.parent.progressIndeterminate.emit()

    def progressStop(self):
        self.parent.progressStop.emit()

    def updateName(self, newName):
        self.name = newName or DEFAULT_NAME

    def sendRequestToPeer(self, peer, type, data, timeout=None):
        peerSocket = TCPSocket()
        peerSocket.connect(self.peers[peer].lastKnownIP, 32102)
        self.sendRequestToSocket(peerSocket, type, data, timeout)
        return peerSocket

    def sendRequestToSocket(self, socket, type, data, timeout=None):
        request = objectToJSON(Request(type, data, self.guid))
        socket.setTimeout(timeout)
        socket.send(request, progressCallback=self.progressRepport)

    def peerDiscovered(self, guid, name, ip):
        if guid == self.guid:
            return
        if guid in self.peers:
            self.peers[guid].name = name
            self.peers[guid].lastKnownIP = ip
        else:
            self.peers[guid] = Peer(guid, name)
        self.storePeers()
        self.peersUpdated.emit()

    def listenerThread(self):
        listener = TCPSocket()
        listener.listen("0.0.0.0", 32102)
        while True:
            requestSocket = listener.accept()
            requestHandlerThread = Thread(target=self.receiverThread, args=(requestSocket,))
            requestHandlerThread.start()

    def receiverThread(self, socket):
        try:
            receivedData = socket.recv(self.progressRepport)
            self.progressStop()
            self.requestReceivedSignal.emit(socket, receivedData)
        except FileTransferBaseException as e:
            self.showMessageBox.emit(e.message)

    def requestReceived(self, socket, data):
        try:
            receivedRequest = JSONToObject(Request, data.decode("utf-8"))
            self.handlerMap[receivedRequest.type](socket, receivedRequest)
        except FileTransferBaseException as e:
            self.showMessageBox.emit(e.message)
        except Exception as e:
            self.showMessageBox.emit(str(e))

    def startRequestListener(self):
        thread = Thread(target=self.listenerThread, daemon=True)
        thread.start()

    def storePeers(self):
        storedPeersDict = {}
        for peer in self.peers:
            storedPeersDict[peer] = objectToJSON(self.peers[peer])
        storeItem(storedPeers, "peers", json.dumps(storedPeersDict))

    def getStoredPeers(self):
        if getStoredItem(storedPeers, "peers") is None:
            return {}
        else:
            storedPeersDict = json.loads(getStoredItem(storedPeers, "peers"))
            returnedDict = {}
            for peer in storedPeersDict:
                returnedDict[peer] = JSONToObject(Peer, storedPeersDict[peer])
            return returnedDict

    def discoverPeers(self):
        self.discoveryManger.discoverPeers()

    def pair(self, guid, continueToSend=None):
        sharedPassword = askForInput(self.parent, "Enter shared password")

        def pairThread():
            try:
                self.progressIndeterminate()
                myPrivateKey = generateDHPrivate()
                myPublicKey = calculateDHPublic(myPrivateKey)
                nonlocal sharedPassword
                if sharedPassword is None:
                    self.progressStop()
                    return False
                sharedPassword = sharedPassword
                requestPublicKey = PublicKey(myPublicKey, sharedPassword)
                pairingRequest = PairingRequest(self.name, self.guid, requestPublicKey)
                peerSocket = self.sendRequestToPeer(guid, REQUEST_TYPE_PAIR, objectToJSON(pairingRequest))
                response = peerSocket.recv().decode("utf-8")
                if response == RESPONSE_REJECT or response == RESPONSE_BAD_SIGNATURE:
                    self.showMessageBox.emit(response)
                    return False
                peersKey = JSONToObject(PublicKey, JSONToObject(Request, response).data)
                if not peersKey.verifySignature(sharedPassword):
                    self.showMessageBox.emit("Bad shared password")
                    peerSocket.send(RESPONSE_BAD_SIGNATURE)
                    return False
                peerSocket.setTimeout(5)
                peerSocket.send(RESPONSE_OK)
                self.peers[guid].myPrivateKey = myPrivateKey
                self.peers[guid].publicKey = peersKey.key
                self.peers[guid].sharedPassword = sharedPassword
                self.storePeers()
                self.peersUpdated.emit()
                self.progressStop()
                if continueToSend:
                    self.continueSend.emit(*continueToSend)
            except FileTransferBaseException as e:
                self.showMessageBox.emit(e.message)

        pairerThread = Thread(target=pairThread)
        pairerThread.start()

    def unpair(self, guid):
        if guid in self.peers:
            self.peers[guid].publicKey = self.peers[guid].myPrivateKey = self.peers[guid].sharedPassword = None
            self.storePeers()
            self.peersUpdated.emit()

    def deletePeer(self, guid):
        if guid in self.peers:
            del self.peers[guid]
            self.peersUpdated.emit()
            self.storePeers()

    def sendFile(self, fileName, fileContents, guid):
        try:
            self.progressIndeterminate()
            selectedPeer = self.peers[guid]
            filePrivateKey = generateDHPrivate()
            filePublicKey = calculateDHPublic(filePrivateKey)
            preSendPublicKey = PublicKey(filePublicKey, self.peers[guid].sharedPassword)
            preSendRequest = PreSendRequest(fileName, preSendPublicKey)
            preSendSocket = self.sendRequestToPeer(guid, REQUEST_TYPE_PRE_SEND, objectToJSON(preSendRequest))
            response = preSendSocket.recv().decode("utf-8")
            if response == RESPONSE_NOT_PAIRED:
                self.pair(guid, continueToSend=(fileName, fileContents, guid))
                return
            elif response == RESPONSE_REJECT:
                self.showMessageBox.emit("File rejected")
                self.progressStop()
                return
        except FileTransferBaseException as e:
            self.showMessageBox.emit(e.message)
            return

        def senderThread():
            try:
                aesKey = calculateDHAES(selectedPeer.publicKey, filePrivateKey)
                sentFile = File(fileName, fileContents, aesKey)
                self.sendRequestToPeer(guid, REQUEST_TYPE_SEND, objectToJSON(sentFile), 5)
                self.progressStop()
            except FileTransferBaseException as e:
                self.showMessageBox.emit(e.message)

        sThread = threading.Thread(target=senderThread)
        sThread.start()

    def handlePairRequest(self, socket, request):
        requestData = JSONToObject(PairingRequest, request.data)
        signatureSecret = askForInput(self.parent,
                                      "Pairing request from " + requestData.name + ". Enter shared password:")

        def handlerThread():
            try:
                if not signatureSecret:
                    socket.send(RESPONSE_REJECT)
                    return
                if not requestData.publicKey.verifySignature(signatureSecret):
                    self.showMessageBox.emit("Bad shared password")
                    socket.send(RESPONSE_BAD_SIGNATURE)
                    return
                myPrivateKey = generateDHPrivate()
                myPublicKey = calculateDHPublic(myPrivateKey)
                publicKey = PublicKey(myPublicKey, signatureSecret)
                self.sendRequestToSocket(socket, REQUEST_TYPE_PUBLICKEY, objectToJSON(publicKey), 5)
                response = socket.recv().decode("utf-8")
                if response != RESPONSE_OK:
                    self.showMessageBox.emit(response)
                    return
                if request.guid not in self.peers:
                    self.peers[request.guid] = Peer(request.guid, requestData.name)
                self.peers[request.guid].myPrivateKey = myPrivateKey
                self.peers[request.guid].publicKey = requestData.publicKey.key
                self.peers[request.guid].sharedPassword = signatureSecret
                self.peers[request.guid].lastKnownIP = socket.address
                self.storePeers()
                self.peersUpdated.emit()
            except FileTransferBaseException as e:
                self.showMessageBox.emit(e.message)

        hThread = Thread(target=handlerThread)
        hThread.start()

    def handlePreSendRequest(self, socket, request):
        if (request.guid in self.peers) and self.peers[request.guid].sharedPassword:
            preSendRequest = JSONToObject(PreSendRequest, request.data)
            if not preSendRequest.publicKey.verifySignature(self.peers[request.guid].sharedPassword):
                socket.send(RESPONSE_BAD_SIGNATURE)
                return
            if askForConfirmation(self.parent,
                                  "Accept file " + preSendRequest.fileName + " from " + self.peers[
                                      request.guid].name + "?"):
                self.preSendRegistrations[request.guid] = preSendRequest
                socket.send(RESPONSE_OK)
            else:
                socket.send(RESPONSE_REJECT)
        else:
            socket.send(RESPONSE_NOT_PAIRED)

    def receiveFile(self, socket, request):
        def fileReceiverThread():
            self.progressIndeterminate()
            socket.setTimeout(5)
            if not ((request.guid in self.preSendRegistrations) and self.preSendRegistrations[request.guid]):
                self.showMessageBox.emit("No presend registration")
                return
            registration = self.preSendRegistrations[request.guid]
            self.preSendRegistrations[request.guid] = False
            receivedFile = JSONToObject(File, request.data)
            if registration.fileName != receivedFile.fileName:
                self.showMessageBox.emit("Name mismatch, rejected")
                self.progressStop()
                return
            aesKey = calculateDHAES(registration.publicKey.key, self.peers[request.guid].myPrivateKey)
            fileContents = receivedFile.getContents(aesKey)
            self.fileReceived.emit(receivedFile.fileName, fileContents)
            self.progressStop()

        rThread = threading.Thread(target=fileReceiverThread)
        rThread.start()
