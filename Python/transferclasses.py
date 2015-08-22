from cryptography import generateSignature, verifySignature, generateDHPrivate, calculateDHPublic, calculateDHAES, \
    AESEncrypt, AESDecrypt
from strings import *


class Request:
    jsonKeys = {"type": JSON_REQUEST_TYPE,
                "data": JSON_REQUEST_DATA,
                "guid": JSON_GUID}

    def __init__(self, type, data, guid):
        self.type = type
        self.data = data
        self.guid = guid


class PublicKey:
    jsonKeys = {"key": JSON_PUBLICKEY,
                "signature": JSON_SIGNATURE}

    def __init__(self, key, secret):
        self.key = key
        self.signature = generateSignature(self.key, secret)

    def verifySignature(self, secret):
        return verifySignature(self.key, secret, self.signature)


class Peer:
    jsonKeys = {"guid": JSON_GUID,
                "name": JSON_NAME,
                "myPrivateKey": JSON_PRIVATEKEY,
                "publicKey": JSON_PUBLICKEY,
                "sharedPassword": "sharedPassword"}

    def __new__(cls, *args, **kwargs):
        newObject = object.__new__(Peer)
        newObject.lastKnownIP = None
        newObject.myPrivateKey = None
        newObject.publicKey = None
        newObject.sharedPassword = None
        newObject.lastKnownIP = None
        return newObject

    def __init__(self, guid, name):
        self.guid = guid
        self.name = name
        self.myPrivateKey = None
        self.publicKey = None
        self.sharedPassword = None
        self.lastKnownIP = None

    def pair(self, myPrivateKey, publicKey, sharedPassword):
        self.myPrivateKey = myPrivateKey
        self.publicKey = publicKey
        self.sharedPassword = sharedPassword


class PairingRequest:
    jsonKeys = {"name": JSON_NAME,
                "guid": JSON_GUID,
                "publicKey": (JSON_PUBLICKEY, PublicKey)}

    def __init__(self, name, guid, publicKey):
        self.name = name
        self.guid = guid
        self.publicKey = publicKey


class File:
    jsonKeys = {"contents": JSON_FILE_CONTENTS,
                "fileName": JSON_FILE_NAME}

    def __init__(self, fileName, fileContents, aesKey):
        self.contents = AESEncrypt(fileContents, aesKey)
        self.fileName = fileName

    def getContents(self, aesKey):
        return AESDecrypt(self.contents, aesKey, False)


class PreSendRequest:
    jsonKeys = {"fileName": JSON_FILE_NAME,
                "publicKey": (JSON_PUBLICKEY, PublicKey)}

    def __init__(self, fileName, publicKey):
        self.publicKey = publicKey
        self.fileName = fileName
