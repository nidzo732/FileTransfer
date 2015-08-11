from PyQt4 import QtCore
from PyQt4.QtCore import QObject
from networking import TCPSocket, NetworkingError
import socket
import struct
import threading
from identification import getStoredName, getStoredGuid, DEFAULT_NAME

DEFAULT_MULTICAST_TTL = 1
DEFAULT_MULTICAST_GROUP = "224.5.6.7"
DEFAULT_MULTICAST_PORT = 32100
DEFAULT_DISCOVERY_TIMEOUT = 2
DEFAULT_MULTICAST_MESSAGE = b"DISCOVERY"
DEFAULT_DISCOVERY_PORT = 32101

PARAM_CODE_TTL = "TTL"
PARAM_CODE_TIMEOUT = "TIMEOUT"
PARAM_CODE_REPEAT_COUNT = "REPEAT"


class UDPDiscovery(QObject):
    peerDiscovered = QtCore.pyqtSignal(str, str, str)

    def __init__(self):
        QObject.__init__(self)
        self.responseReceiverThread = None

    def discoverPeers(self):
        if self.responseReceiverThread is None:
            self.responseReceiverThread = threading.Thread(target=self.responseListenerThread, daemon=True)
            self.responseReceiverThread.start()
        emiterSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        emiterSocket.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, struct.pack("b", DEFAULT_MULTICAST_TTL))
        emiterSocket.sendto(DEFAULT_MULTICAST_MESSAGE, (DEFAULT_MULTICAST_GROUP, DEFAULT_MULTICAST_PORT))

    def responseListenerThread(self):
        while True:
            listenerSocket = TCPSocket()
            listenerSocket.listen("0.0.0.0", DEFAULT_DISCOVERY_PORT)
            newConnection = listenerSocket.accept()
            discoveredThread = threading.Thread(target=self.peerDiscoveredThread,
                                                args=(newConnection,))
            discoveredThread.start()

    def peerDiscoveredThread(self, receivedSocket):
        try:
            data = receivedSocket.recv()
            ids = data.decode("utf-8").split(":")
            self.peerDiscovered.emit(ids[0], ids[1], receivedSocket.address)
        except NetworkingError:
            return

    @staticmethod
    def responseSenderThread():
        listenerSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        listenerSocket.bind(("0.0.0.0", DEFAULT_MULTICAST_PORT))
        socketSettings = struct.pack("4sL", socket.inet_aton(DEFAULT_MULTICAST_GROUP), socket.INADDR_ANY)
        listenerSocket.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, socketSettings)
        while True:
            data, address = listenerSocket.recvfrom(1024)
            responder = TCPSocket()
            myName = getStoredName() or DEFAULT_NAME
            try:
                responder.connect(address[0], DEFAULT_DISCOVERY_PORT)
                responder.send((getStoredGuid() + ":" + myName).encode("utf-8"))
            except NetworkingError:
                return

    def startDiscoveryServer(self):
        responderThread = threading.Thread(target=UDPDiscovery.responseSenderThread, daemon=True)
        responderThread.daemon = True
        responderThread.start()
