using System;
using System.Threading;
using System.Threading.Tasks;
using Windows.Networking;
using Windows.Networking.Connectivity;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;

namespace FileTransfer.Networking
{
    public class NetworkingError: FileTransferException
    {
        public NetworkingError(string message)
        {
            errorText = "Network communication error: " + message;
        }
    }
    class TCPSocket
    {
        public StreamSocket internalSocket;
        public StreamSocketListener internalListener;
        public delegate void ConnectionReceivedHandler(TCPSocket sender);
        public event ConnectionReceivedHandler ConnectionReceived;
        private const int MAX_HEADER_LENGTH = 32;
        public delegate void ProgressRepportHandler(int x);
        public event ProgressRepportHandler ProgressRepport;
        private const int CHUNK_SIZE = 16384;
        private int timeoutMilliseconds;
        private CancellationToken getTimeoutToken()
        {
            if (timeoutMilliseconds > 0)
            {
                return new CancellationTokenSource(timeoutMilliseconds).Token;
            }
            else
            {
                return new CancellationTokenSource().Token;
            }
        }
        public TCPSocket(StreamSocket newInternal)
        {
            internalSocket=newInternal;
            SetTimeout(5000);
        }
        public TCPSocket()
        {
            internalListener = new StreamSocketListener();
            internalSocket = new StreamSocket();
            SetTimeout(5000);
        }
        public void SetTimeout(int milliSeconds)
        {
            timeoutMilliseconds = milliSeconds;
        }
        public async Task StartListening(string port)
        {
            internalListener.ConnectionReceived += connectionReceived;
            try
            {
                await internalListener.BindServiceNameAsync(port).AsTask(getTimeoutToken());
            }
            catch (Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }

        private void connectionReceived(StreamSocketListener sender, StreamSocketListenerConnectionReceivedEventArgs args)
        {
            ConnectionReceived(new TCPSocket(args.Socket));
        }
        public string GetIP()
        {
            try
            {
                return internalSocket.Information.RemoteAddress.RawName;
            }
            catch (Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }
        private void repportProgress(int current, int goal)
        {
            if (goal < CHUNK_SIZE) return;
            int percentage = (int)(((double)current) / ((double)goal) * 100);
            if (ProgressRepport != null) ProgressRepport(percentage);
        }
        public async Task Send(string data)
        {
            byte[] dataBytes = Cryptography.TextManipulation.EncodeUTF(data);
            string dataLength = dataBytes.Length.ToString();
            DataWriter socketDataWriter=new DataWriter(internalSocket.OutputStream);
            byte[] dataToSend = Cryptography.TextManipulation.EncodeUTF(dataLength + "MLEN" + data);
            int dataSent = 0;
            while(dataSent<dataToSend.Length)
            {
                int currentChunkSize = CHUNK_SIZE;
                if (dataToSend.Length - dataSent < CHUNK_SIZE) currentChunkSize = dataToSend.Length - dataSent;
                byte[] chunk = new byte[currentChunkSize];
                Array.Copy(dataToSend, dataSent, chunk, 0, currentChunkSize);
                socketDataWriter.WriteBytes(chunk);
                await socketDataWriter.StoreAsync().AsTask(getTimeoutToken());
                await socketDataWriter.FlushAsync().AsTask(getTimeoutToken());
                dataSent += currentChunkSize;
                repportProgress(dataSent, dataToSend.Length);
            }
            await socketDataWriter.StoreAsync().AsTask(getTimeoutToken());
            await socketDataWriter.FlushAsync().AsTask(getTimeoutToken());
            socketDataWriter.DetachStream();
        }
        public async Task<string> Recv()
        {
            string headerBuffer = "";
            try
            {
                DataReader socketDataReader = new DataReader(internalSocket.InputStream);
                while (!headerBuffer.EndsWith("MLEN") && headerBuffer.Length < MAX_HEADER_LENGTH)
                {
                    await socketDataReader.LoadAsync(1).AsTask(getTimeoutToken());
                    headerBuffer += socketDataReader.ReadString(1);
                }
                if (!headerBuffer.EndsWith("MLEN"))
                {
                    return null;
                }
                UInt64 length;
                if (!UInt64.TryParse(headerBuffer.Replace("MLEN", ""), out length))
                {
                    return null;
                }
                byte[] data = new byte[length];
                ulong dataReceived = 0;
                while (dataReceived < length)
                {
                    int currentChunkSize = CHUNK_SIZE;
                    if (length - dataReceived < CHUNK_SIZE) currentChunkSize = (int)(length - dataReceived);
                    byte[] chunk = new byte[currentChunkSize];
                    await socketDataReader.LoadAsync((uint)currentChunkSize).AsTask(getTimeoutToken());
                    socketDataReader.ReadBytes(chunk);
                    Array.Copy(chunk, 0, data, (int)dataReceived, currentChunkSize);
                    dataReceived += (ulong)currentChunkSize;
                    repportProgress((int)dataReceived, (int)length);
                }
                socketDataReader.DetachStream();
                return Cryptography.TextManipulation.DecodeUTF(data);
            }
            catch (Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }
        public async Task Connect(string address, string port)
        {
            try
            {
                await internalSocket.ConnectAsync(new HostName(address), port).AsTask(getTimeoutToken());
            }
            catch(Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }
        public void Close()
        {
            if (internalListener != null) internalListener.Dispose();
            if (internalSocket != null) internalSocket.Dispose();
        }
    }
    class MulticastSocket
    {
        DatagramSocket internalSocket;
        public delegate void ConnectionReceivedHandler(DatagramSocketMessageReceivedEventArgs args);
        public event ConnectionReceivedHandler ConnectionReceived;
        public MulticastSocket()
        {
            internalSocket = new DatagramSocket();
            internalSocket.MessageReceived+=messageReceived;
            internalSocket.Control.OutboundUnicastHopLimit = 200;
        }
        public void Close()
        {
            internalSocket.Dispose();
        }
        public async Task EmmitMulticast(string address, string port, string data)
        {
            try
            {
                await internalSocket.BindServiceNameAsync("", NetworkInformation.GetInternetConnectionProfile().NetworkAdapter);
                internalSocket.JoinMulticastGroup(new HostName(address));
                var stream = await internalSocket.GetOutputStreamAsync(new HostName(address), port);
                var dw = new DataWriter(stream);
                dw.WriteString(data);
                await dw.StoreAsync();
            }
            catch (Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }
        public async Task StartListening(string address, string port)
        {
            try
            {
                await internalSocket.BindServiceNameAsync(port);
                internalSocket.JoinMulticastGroup(new HostName(address));
            }
            catch(Exception error)
            {
                throw new NetworkingError(error.Message);
            }
        }
        void messageReceived(DatagramSocket sender, DatagramSocketMessageReceivedEventArgs args)
        {
            ConnectionReceived(args);
        }
    }
}
