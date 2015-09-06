using FileTransfer.Networking;
using System;
using System.Threading.Tasks;

namespace FileTransfer
{
    class MulticastDiscovery
    {
        private static string 
            DEFAULT_MULTICAST_GROUP = "224.5.6.7",
            DEFAULT_MULTICAST_PORT = "32100",
            DEFAULT_MULTICAST_MESSAGE = "DISCOVERY",
            DEFAULT_DISCOVERY_ECHO = "DISCOVERY_RESPONSE",
            DEFAULT_DISCOVERY_PORT = "32101";
        private static TCPSocket echoListener;
        private static MulticastSocket echoResponder;
        public delegate void DeviceDiscoveredHandler(string ip, string guid, string name);
        public static event DeviceDiscoveredHandler DeviceDiscovered;
        private static string myName;
        public async static Task DiscoverDevices()
        {
            if (echoListener==null) await runDiscoveryServer();
            var discoveryEmitter = new MulticastSocket();
            await discoveryEmitter.EmmitMulticast(DEFAULT_MULTICAST_GROUP, DEFAULT_MULTICAST_PORT, DEFAULT_MULTICAST_MESSAGE);
        }
        public static void UpdateName(string newName) { myName = newName; }
        public async static Task StartMulticastResponder(string name)
        {
            if (echoResponder != null) return;
            myName = name;
            echoResponder = new MulticastSocket();
            await echoResponder.StartListening(DEFAULT_MULTICAST_GROUP, DEFAULT_MULTICAST_PORT);
            echoResponder.ConnectionReceived += discoveryRequestReceived;
        }
        public static void Stop()
        {
            if(echoListener != null) echoListener.Close();
            if(echoResponder != null) echoResponder.Close();
            echoResponder = null;
            echoListener = null;
        }
        private async static void discoveryRequestReceived(Windows.Networking.Sockets.DatagramSocketMessageReceivedEventArgs args)
        {
            if (args.RemoteAddress.RawName == "127.0.0.1") return;
            try
            {
                var responseSocket = new TCPSocket();
                await responseSocket.Connect(args.RemoteAddress.RawName, DEFAULT_DISCOVERY_PORT);
                await responseSocket.Send(await GuidHandling.GetMyGuid() + ":" + myName);
            }
            catch(Exception)
            {

            }
        }
        private async static Task runDiscoveryServer()
        {
            echoListener = new TCPSocket();
            echoListener.ConnectionReceived += deviceDiscovered;
            await echoListener.StartListening(DEFAULT_DISCOVERY_PORT);
        }
        
        private static async void deviceDiscovered(TCPSocket sender)
        {
            try
            {
                string ip = sender.GetIP();
                string receivedString = await sender.Recv();
                if (receivedString.IndexOf(":") == -1 || receivedString.IndexOf(":") == receivedString.Length-1) return;
                string guid = receivedString.Substring(0, receivedString.IndexOf(":"));
                if (guid == (await GuidHandling.GetMyGuid())) return;
                string name = receivedString.Substring(receivedString.IndexOf(":") + 1);
                DeviceDiscovered(ip, guid, name);
            }
            catch(Exception)
            {

            }
        }
    }
}
