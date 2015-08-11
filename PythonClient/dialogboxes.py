from PyQt4 import QtGui
from PyQt4.QtGui import QInputDialog


def askForConfirmation(parent, message):
    confirmationBox = QtGui.QMessageBox(parent=parent, text=message)
    confirmationBox.setStandardButtons(QtGui.QMessageBox.Yes | QtGui.QMessageBox.No)
    confirmationBox.setWindowTitle("File transfer")
    return confirmationBox.exec() == QtGui.QMessageBox.Yes


def askForInput(parent, message):
    response = QInputDialog.getText(parent, "File transfer", message)
    if response[1]:
        return response[0]
    else:
        return None


def showMessageBox(parent, messageText):
    messageBox = QtGui.QMessageBox(parent=parent, text=(messageText))
    messageBox.setWindowTitle("File transfer")
    messageBox.show()
