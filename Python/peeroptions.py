import os
from PyQt4 import QtGui
from dialogboxes import showMessageBox
from peeroptionsui import Ui_MainWindow


class PeerOptionsUi(QtGui.QMainWindow):
    def __init__(self, parent, peer, communicator):
        super(PeerOptionsUi, self).__init__(parent)
        self.ui = Ui_MainWindow()
        self.ui.setupUi(self)
        self.peer = peer
        self.communicator = communicator
        self.parentWindow = parent
        self.ui.togglePairingButton.clicked.connect(self.togglePairing)
        self.ui.sendFileButton.clicked.connect(self.sendFile)
        self.ui.deletePeerButton.clicked.connect(self.deletePeer)
        self.ui.infoButton.clicked.connect(self.showPeerInfo)

    def show(self):
        super(PeerOptionsUi, self).show()
        self.ui.peerName.setText(self.peer.name)
        if self.peer.publicKey is None:
            self.ui.togglePairingButton.setText("Pair")
            self.ui.togglePairingButton.setEnabled(self.peer.lastKnownIP is not None)
        else:
            self.ui.togglePairingButton.setText("Unpair")
        self.ui.sendFileButton.setEnabled((self.peer.publicKey is not None) and (self.peer.lastKnownIP is not None))

    def togglePairing(self):
        if self.peer.publicKey is None:
            self.communicator.pair(self.peer.guid)
        else:
            self.communicator.unpair(self.peer.guid)
        self.close()

    def sendFile(self):
        fileName = QtGui.QFileDialog.getOpenFileName()
        if not fileName:
            return
        fileContents = open(fileName, "rb").read()
        basename = os.path.basename(fileName)
        self.communicator.sendFile(basename, fileContents, self.peer.guid)
        self.close()

    def deletePeer(self):
        self.communicator.deletePeer(self.peer.guid)
        self.close()

    def showPeerInfo(self):
        infoText = "Name: " + self.peer.name
        if self.peer.lastKnownIP:
            infoText += "\nAvailable: Yes\nIP address: " + self.peer.lastKnownIP
        else:
            infoText += "\nAvailable: No"
        if self.peer.publicKey:
            infoText += "\nPaired: Yes"
        else:
            infoText += "\nPaired: No"
        infoText += "\nUnique ID: " + self.peer.guid
        showMessageBox(self, infoText)
