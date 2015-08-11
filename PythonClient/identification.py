from PyQt4.QtCore import QSettings
from storage import *
import uuid

storedGuid = QSettings("FileTransfer", "guid")
storedName = QSettings("FileTransfer", "name")

DEFAULT_NAME = "Unnamed device"

def storeGuid(guid):
    storeItem(storedGuid, "guid", guid)

def getStoredGuid():
    if getStoredItem(storedGuid, "guid"):
        return getStoredItem(storedGuid, "guid")
    else:
        guid = str(uuid.uuid4())
        storeGuid(guid)
        return guid

def storeName(name):
    storeItem(storedName, "name", name)

def getStoredName():
    return getStoredItem(storedName, "name")




