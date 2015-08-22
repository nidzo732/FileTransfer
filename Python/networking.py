import socket
from exception import FileTransferBaseException

MESSAGE_LENGTH_DELIMITER = b"MLEN"
MAX_MESSAGELENGTH_LENGTH = 20
CHUNK_SIZE = 16384


class NetworkingError(FileTransferBaseException):
    def __init__(self, message=""):
        FileTransferBaseException.__init__(self, "- networking error "+message)


class TCPSocket:
    def __init__(self, socketToUse=None, address=None):
        if socketToUse:
            self.internalSocket = socketToUse
        else:
            self.internalSocket = socket.socket(socket.AF_INET,
                                                socket.SOCK_STREAM)

        self.internalSocket.setsockopt(socket.SOL_SOCKET,
                                       socket.SO_REUSEADDR, 1)
        self.internalSocket.settimeout(5.0)
        self.address = address

    def listen(self, address, port, timeout=None):
        self.internalSocket.settimeout(timeout)
        self.internalSocket.bind((address, port))
        self.internalSocket.listen(5)

    def setTimeout(self, timeout):
        self.internalSocket.settimeout(timeout)

    def recv(self, progressCallback=None):
        try:
            receivedData = b""
            while receivedData.find(MESSAGE_LENGTH_DELIMITER) == -1:
                if len(receivedData) > MAX_MESSAGELENGTH_LENGTH:
                    raise NetworkingError
                newData = self.internalSocket.recv(1)
                if len(newData) == 0:
                    raise NetworkingError("Socket got closed before receiving the entire message")
                receivedData += newData
            sizelen = receivedData.find(MESSAGE_LENGTH_DELIMITER)
            messageLength = int(receivedData[0:sizelen])
            receivedData = receivedData[sizelen + len(MESSAGE_LENGTH_DELIMITER):]

            while len(receivedData) < messageLength:
                chunkSize = CHUNK_SIZE
                if chunkSize > messageLength - len(receivedData):
                    chunkSize = messageLength - len(receivedData)
                newData = self.internalSocket.recv(chunkSize)
                if len(newData) == 0:
                    raise NetworkingError("socket got closed before receiving the entire message")
                receivedData += newData
                if progressCallback and messageLength > CHUNK_SIZE:
                    progressCallback(len(receivedData) / messageLength * 100)
            return receivedData
        except OSError as e:
            raise NetworkingError("failed to receive data")

    def send(self, data, progressCallback=None):
        try:
            if type(data) == str:
                data = data.encode("utf-8")
            dataLength = str(len(data)).encode(encoding="ASCII")
            message = dataLength + MESSAGE_LENGTH_DELIMITER + data
            dataSent = 0
            while dataSent < len(message):
                chunkSize = CHUNK_SIZE
                if chunkSize > len(message) - dataSent:
                    chunkSize = len(message) - dataSent
                chunk = message[dataSent:dataSent + chunkSize]
                self.internalSocket.sendall(chunk)
                if progressCallback and len(message) > CHUNK_SIZE:
                    progressCallback(dataSent / len(message) * 100)
                dataSent += chunkSize
        except OSError:
                raise NetworkingError("failed to send data")

    def connect(self, address, port):
        try:
            self.address = address
            self.internalSocket.connect((address, port))
        except OSError:
            raise NetworkingError("failed to connect")

    def accept(self):
        requestData = self.internalSocket.accept()
        return TCPSocket(requestData[0], requestData[1][0])

    def close(self):
        # close the socket
        self.internalSocket.close()
