class FileTransferBaseException(Exception):
    def __init__(self, message=""):
        Exception.__init__(self)
        self.message = "File transfer error "+message
