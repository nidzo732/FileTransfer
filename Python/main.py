#!/usr/bin/python3
import sys

from PyQt4 import QtGui as gui

import mainwin


if __name__ == "__main__":
    mainApp = gui.QApplication(sys.argv)
    mainWindow = mainwin.MainWinGui()
    mainWindow.show()
    mainApp.exec_()
