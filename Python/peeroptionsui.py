# -*- coding: utf-8 -*-

# Form implementation generated from reading ui file 'peeroptions.ui'
#
# Created: Sun Sep  6 16:40:33 2015
#      by: PyQt4 UI code generator 4.10.4
#
# WARNING! All changes made in this file will be lost!

from PyQt4 import QtCore, QtGui

try:
    _fromUtf8 = QtCore.QString.fromUtf8
except AttributeError:
    def _fromUtf8(s):
        return s

try:
    _encoding = QtGui.QApplication.UnicodeUTF8
    def _translate(context, text, disambig):
        return QtGui.QApplication.translate(context, text, disambig, _encoding)
except AttributeError:
    def _translate(context, text, disambig):
        return QtGui.QApplication.translate(context, text, disambig)

class Ui_MainWindow(object):
    def setupUi(self, MainWindow):
        MainWindow.setObjectName(_fromUtf8("MainWindow"))
        MainWindow.resize(450, 72)
        self.centralwidget = QtGui.QWidget(MainWindow)
        self.centralwidget.setObjectName(_fromUtf8("centralwidget"))
        self.gridLayout = QtGui.QGridLayout(self.centralwidget)
        self.gridLayout.setObjectName(_fromUtf8("gridLayout"))
        self.verticalLayout = QtGui.QVBoxLayout()
        self.verticalLayout.setObjectName(_fromUtf8("verticalLayout"))
        self.peerName = QtGui.QLabel(self.centralwidget)
        self.peerName.setObjectName(_fromUtf8("peerName"))
        self.verticalLayout.addWidget(self.peerName)
        self.horizontalLayout = QtGui.QHBoxLayout()
        self.horizontalLayout.setObjectName(_fromUtf8("horizontalLayout"))
        self.togglePairingButton = QtGui.QPushButton(self.centralwidget)
        self.togglePairingButton.setObjectName(_fromUtf8("togglePairingButton"))
        self.horizontalLayout.addWidget(self.togglePairingButton)
        self.sendFileButton = QtGui.QPushButton(self.centralwidget)
        self.sendFileButton.setObjectName(_fromUtf8("sendFileButton"))
        self.horizontalLayout.addWidget(self.sendFileButton)
        self.deletePeerButton = QtGui.QPushButton(self.centralwidget)
        self.deletePeerButton.setObjectName(_fromUtf8("deletePeerButton"))
        self.horizontalLayout.addWidget(self.deletePeerButton)
        self.infoButton = QtGui.QPushButton(self.centralwidget)
        self.infoButton.setObjectName(_fromUtf8("infoButton"))
        self.horizontalLayout.addWidget(self.infoButton)
        self.verticalLayout.addLayout(self.horizontalLayout)
        self.gridLayout.addLayout(self.verticalLayout, 0, 0, 1, 1)
        MainWindow.setCentralWidget(self.centralwidget)

        self.retranslateUi(MainWindow)
        QtCore.QMetaObject.connectSlotsByName(MainWindow)

    def retranslateUi(self, MainWindow):
        MainWindow.setWindowTitle(_translate("MainWindow", "Peer options", None))
        self.peerName.setText(_translate("MainWindow", "TextLabel", None))
        self.togglePairingButton.setText(_translate("MainWindow", "PushButton", None))
        self.sendFileButton.setText(_translate("MainWindow", "Send file", None))
        self.deletePeerButton.setText(_translate("MainWindow", "Delete peer", None))
        self.infoButton.setText(_translate("MainWindow", "Peer info", None))

