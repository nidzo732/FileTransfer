import os
from PyQt4 import QtGui, QtCore
from PyQt4.QtGui import QListWidgetItem, QProgressBar
from communicator import Communicator
from identification import getStoredName, storeName
from mainwindowui import Ui_MainWindow
from peeroptions import PeerOptionsUi


class MainWinGui(QtGui.QMainWindow):
    progressIndeterminate = QtCore.pyqtSignal()
    progressSet = QtCore.pyqtSignal(int)
    progressStop = QtCore.pyqtSignal()

    def __init__(self, parent=None):
        super(MainWinGui, self).__init__(parent)
        self.ui = Ui_MainWindow()
        self.ui.setupUi(self)
        self.ui.myName.setText(getStoredName() or "")

        self.ui.myName.textEdited.connect(self.nameChanged)

        self.communicator = Communicator(self)
        self.communicator.peersUpdated.connect(self.refreshPeers)
        self.communicator.fileReceived.connect(self.fileReceived)

        self.ui.refreshPeersButton.clicked.connect(self.discoverPeers)
        self.ui.peerList.itemClicked.connect(self.peerSelected)

        self.progressIndeterminate.connect(self.progressIndeterminateSlot)
        self.progressSet.connect(self.progressSetSlot)
        self.progressStop.connect(self.progressStopSlot)

        self.progressIndicator = QProgressBar(self.ui.statusbar)
        self.progressIndicator.setMinimumHeight(5)
        self.progressIndicator.setVisible(False)
        self.progressIndicator.setMaximum(0)
        self.progressIndicator.setMinimum(0)
        self.ui.statusbar.addWidget(self.progressIndicator)

    def nameChanged(self, newName):
        storeName(newName)
        self.communicator.updateName(newName)

    def progressStart(self):
        self.progressIndicator.setVisible(True)

    def progressSetSlot(self, value):
        self.progressStart()
        self.progressIndicator.setMaximum(100)
        self.progressIndicator.setValue(value)

    def progressIndeterminateSlot(self):
        self.progressStart()
        self.progressIndicator.setMaximum(0)

    def progressStopSlot(self):
        self.progressIndicator.setVisible(False)

    def show(self):
        super(MainWinGui, self).show()
        self.refreshPeers()
        self.discoverPeers()

    def discoverPeers(self):
        self.communicator.discoverPeers()

    def refreshPeers(self):
        self.ui.peerList.clear()
        for peer in self.communicator.peers:
            peer = self.communicator.peers[peer]
            peerName = QListWidgetItem(self.ui.peerList)
            peerName.peer = peer
            nameFont = QtGui.QFont()
            nameFont.setPointSize(14)
            peerDetails = QListWidgetItem(self.ui.peerList)
            peerDetails.peer = peer
            detailsFont = QtGui.QFont()
            detailsFont.setPointSize(10)
            name = peer.name
            details = ""
            if peer.publicKey is None:
                details += "Unpaired, "
            else:
                details += "Paired, "
            if peer.lastKnownIP is None:
                details += "unavailable"
            else:
                details += "available: " + peer.lastKnownIP
            peerName.setFont(nameFont)
            peerName.setText(name)
            peerDetails.setFont(detailsFont)
            peerDetails.setText(details)
            self.ui.peerList.addItem(peerName)
            self.ui.peerList.addItem(peerDetails)
            separatorItem = QListWidgetItem(self.ui.peerList)
            separatorItem.guid = peer.guid
            separatorItem.peer = None
            separatorItem.setFlags(QtCore.Qt.NoItemFlags)
            self.ui.peerList.addItem(separatorItem)

    def peerSelected(self, selectedItem):
        selectedItem.setSelected(False)
        if selectedItem.peer:
            PeerOptionsUi(self, selectedItem.peer, self.communicator).show()
        else:
            pass

    def sendFile(self, guid):
        fileName = QtGui.QFileDialog.getOpenFileName()
        if not fileName:
            return
        fileContents = open(fileName, "rb").read()
        basename = os.path.basename(fileName)
        self.communicator.sendFile(basename, fileContents, guid)

    def fileReceived(self, fileName, fileContents):
        fileName = QtGui.QFileDialog.getSaveFileName(directory=fileName)
        if not fileName:
            return
        with open(fileName, mode="wb") as file:
            file.write(fileContents)
