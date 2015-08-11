def storeItem(storage, name, value):
    if (not value) and (storage.contains(name)):
        storage.remove(name)
    elif value:
        storage.setValue(name, value)
    storage.sync()


def getStoredItem(storage, name):
    if storage.contains(name):
        return storage.value(name)
    else:
        return None
