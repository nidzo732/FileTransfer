import json


def objectToDict(obj):
    generatedDict = {}
    for attr in obj.jsonKeys:
        identifier = obj.jsonKeys[attr]
        if type(identifier) == str:
            attribute = obj.__getattribute__(attr)
            if type(attribute) == bytes:
                attribute = attribute.decode("utf-8")
            generatedDict[identifier] = attribute
        elif type(identifier) == tuple:
            generatedDict[identifier[0]] = objectToDict(obj.__getattribute__(attr))
    return generatedDict


def objectToJSON(obj):
    return json.dumps(objectToDict(obj))


def dictToObject(base, objDict):
    obj = base.__new__(base)
    for attr in obj.jsonKeys:
        identifier = obj.jsonKeys[attr]
        if type(identifier) == str:
            obj.__setattr__(attr, objDict[identifier])
        else:
            obj.__setattr__(attr, dictToObject(identifier[1], objDict[identifier[0]]))
    return obj


def JSONToObject(base, jsonString):
    return dictToObject(base, json.loads(jsonString))
