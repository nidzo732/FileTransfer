using FileTransfer.Cryptography;
using FileTransfer.Networking;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using Windows.UI.Core;

namespace FileTransfer
{
    public static class Communicator
    {
        public static ObservableCollection<Peer> Peers;
        private static Dictionary<string, Peer> peerMap;
        private static Dictionary<string, PreSendRequest> preSendRegistrations;
        private static Dictionary<string, PreSendRequestNoCrypt> preSendRegistrationsNoCrypt;
        private static string myName;
        private static TCPSocket listenerSocket;
        private static Dictionary<string, Func<string, string, TCPSocket, Task>> requestHandlerMap;
        private static CoreDispatcher uiThreadDispatcher;
        public delegate void FileReceivedHandler(string fileName, byte[] fileContents);
        public static event FileReceivedHandler FileReceived;
        public delegate void ProgressRepportHandler(int x);
        public static event ProgressRepportHandler ProgressRepport;
        public delegate void ProgressEventHandler();
        public static event ProgressEventHandler ProgressIndeterminate, ProgressStop;
        public async static Task Init(string name)
        {
            if (uiThreadDispatcher == null) uiThreadDispatcher = Windows.UI.Core.CoreWindow.GetForCurrentThread().Dispatcher;
            myName = name;
            Peers = new ObservableCollection<Peer>();
            preSendRegistrations = new Dictionary<string, PreSendRequest>();
            preSendRegistrationsNoCrypt = new Dictionary<string, PreSendRequestNoCrypt>();
            listenerSocket = new TCPSocket();
            listenerSocket.ConnectionReceived += requestReceived;
            initializeHandlerMap();
            await listenerSocket.StartListening("32102");
            if(!(await DataStorage.GetStoredItemAsync("Peers")==null))
            {
                Peer[] newPeers = JSONHandling.ParseJSONResponse<Peer[]>(await DataStorage.GetStoredItemAsync("Peers"));
                for(int i=0;i<newPeers.Length;i++) Peers.Add(newPeers[i]);
            }
            RefreshPeerMap();
            await MulticastDiscovery.StartMulticastResponder(myName);
            MulticastDiscovery.DeviceDiscovered += newPeerDiscovered;
        }
        public async static Task Reboot()
        {
            if (listenerSocket != null) listenerSocket.Close();
            listenerSocket = null;
            MulticastDiscovery.Stop();
            await Init(myName);
        }
        private static void progressStop()
        {
            if (ProgressStop != null) ProgressStop();
        }
        private static void progressIndeterminate()
        {
            if (ProgressIndeterminate != null) ProgressIndeterminate();
        }
        private static void progressRepport(int percentage)
        {
            if (ProgressRepport != null) ProgressRepport(percentage);
        }
        private async static void requestReceived(TCPSocket sender)
        {
            await uiThreadDispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                try
                {
                    progressIndeterminate();
                    sender.ProgressRepport += (x) => { if (ProgressRepport != null) ProgressRepport(x); };
                    string requestContents = await sender.Recv();
                    progressStop();
                    Request receivedRequest = JSONHandling.ParseJSONResponse<Request>(requestContents);
                    await requestHandlerMap[receivedRequest.Type](receivedRequest.SenderGuid, receivedRequest.Data, sender);
                }
                catch(OutOfMemoryException)
                {
                    DialogBoxes.ShowMessageBox("File too big to fit in memory");
                }
                catch(FileTransferException error)
                {
                    DialogBoxes.ShowMessageBox(error.Message);
                }
                catch(Exception error)
                {
                    DialogBoxes.ShowMessageBox("Unknown error " + error.Message);
                }
                finally
                {
                    progressStop();
                    sender.Send(Strings.RESPONSE_REJECT);
                }
            });
        }

        private async static Task<TCPSocket> sendRequestToPeer(string peer, string type, string data, bool repportProgress=false, int timeout=5000)
        {
            var newSocket = new TCPSocket();
            await newSocket.Connect(peerMap[peer].LastKnownIP, "32102");
            if (repportProgress) newSocket.ProgressRepport += progressRepport;
            return await sendRequestToSocket(newSocket, type, data, timeout);
        }
        private async static Task<TCPSocket> sendRequestToSocket(TCPSocket socket, string type, string data, int timeout=5000)
        {
            var request = new Request();
            request.Data = data;
            request.SenderGuid = await GuidHandling.GetMyGuid();
            request.Type = type;
            socket.SetTimeout(timeout);
            await socket.Send(JSONHandling.SerializeObject(request));
            return socket;
        }
        public async static Task DiscoverDevices()
        {
            await MulticastDiscovery.DiscoverDevices();
        }
        public static void SetNewName(string newName) { myName = newName; MulticastDiscovery.UpdateName(myName); }
        private static void RefreshPeerMap()
        {
            peerMap = new Dictionary<string, Peer>();
            foreach(var peer in Peers)
            {
                peerMap[peer.Guid] = peer;
            }
        }
        private async static void newPeerDiscovered(string ip, string guid, string name)
        {
            await uiThreadDispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                var newPeer = new Peer(guid, name);
                newPeer.LastKnownIP = ip;
                addPeer(newPeer);
                RefreshPeerMap();
                await StorePeers();
            });
        }
        public static Peer GetPeer(string guid)
        {
            return peerMap[guid];
        }
        public async static Task StorePeers()
        {
            Peer[] storedPeers = new Peer[Peers.Count];
            for (int i = 0; i < storedPeers.Length; i++) storedPeers[i] = Peers[i];
            await DataStorage.StoreItemAsync("Peers", JSONHandling.SerializeObject(storedPeers));
        }
        private static void initializeHandlerMap()
        {
            requestHandlerMap = new Dictionary<string, Func<string, string, TCPSocket, Task>>();
            requestHandlerMap[Strings.REQUEST_TYPE_PAIR] = pairRequestReceived;
            requestHandlerMap[Strings.REQUEST_TYPE_PRE_SEND] = preSendRequestReceived;
            requestHandlerMap[Strings.REQUEST_TYPE_SEND] = fileReceived;
            requestHandlerMap[Strings.REQUEST_TYPE_PRE_SEND_NOCRYPT] = preSendNoCryptRequestReceived;
            requestHandlerMap[Strings.REQUEST_TYPE_SEND_NOCRYPT] = fileReceivedNoCrypt;
        }
        private async static Task pairRequestReceived(string peer, string data, TCPSocket socket)
        {
            progressIndeterminate();
            var request = JSONHandling.ParseJSONResponse<PairingRequest>(data);
            if(request.Guid!=peer) 
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_REJECT);
                return;
            }
            string signatureSecret = await DialogBoxes.AskForInput("Pairing request", "Please enter the shared password");
            if (signatureSecret == null)
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_REJECT);
                return;
            }

            if(!request.PublicKey.VerifySignature(signatureSecret))
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_BAD_SIGNATURE);
                await DialogBoxes.ShowMessageBox("Shared password incorrect");
                return;
            }
            string myPrivateKey = DiffieHellman.generate_DH_Private();
            string myPublicKey = DiffieHellman.calculate_DH_Public(myPrivateKey);
            PublicKey returnedKey = new PublicKey(myPublicKey, signatureSecret);

            await sendRequestToSocket(socket, Strings.REQYEST_TYPE_PUBLICKEY, JSONHandling.SerializeObject(returnedKey));
            string response = await socket.Recv();
            progressStop();
            if(response!=Strings.RESPONSE_OK)
            {
                await DialogBoxes.ShowMessageBox(response);
                return;
            }

            Peer newPeer = new Peer(request.Guid, request.Name);
            newPeer.LastKnownIP = socket.GetIP();
            newPeer.MyPrivateKey = myPrivateKey;
            newPeer.PublicKey = request.PublicKey.Key;
            newPeer.SharedPassword = signatureSecret;
            addPeer(newPeer);
            await StorePeers();
            RefreshPeerMap();
        }
        public async static Task<bool> Pair(string guid)
        {
            try
            {
                progressIndeterminate();
                Peer pairedPeer = peerMap[guid];
                PairingRequest request = new PairingRequest();
                request.Guid = await GuidHandling.GetMyGuid();
                request.Name = myName;
                string myPrivateKey = DiffieHellman.generate_DH_Private();
                string myPublicKey = DiffieHellman.calculate_DH_Public(myPrivateKey);
                string signatureSecret = await DialogBoxes.AskForInput("Pairing", "Please enter shared password");
                if (signatureSecret == null) return false;
                request.PublicKey = new PublicKey(myPublicKey, signatureSecret);
                var socket = await sendRequestToPeer(guid, Strings.REQUEST_TYPE_PAIR, JSONHandling.SerializeObject(request), timeout:0);
                string response = await socket.Recv();
                if (response == Strings.RESPONSE_REJECT || response == Strings.RESPONSE_BAD_SIGNATURE)
                {
                    progressStop();
                    await DialogBoxes.ShowMessageBox(response);
                    return false;
                }
                PublicKey peersKey = JSONHandling.ParseJSONResponse<PublicKey>(JSONHandling.ParseJSONResponse<Request>(response).Data);
                progressStop();
                socket.SetTimeout(5000);
                if (!peersKey.VerifySignature(signatureSecret))
                {
                    await DialogBoxes.ShowMessageBox("Bad shared password");
                    await socket.Send(Strings.RESPONSE_BAD_SIGNATURE);
                    return false;
                }
                await socket.Send(Strings.RESPONSE_OK);
                Peer newPeer = new Peer(guid, pairedPeer.Name);
                newPeer.MyPrivateKey = myPrivateKey;
                newPeer.PublicKey = peersKey.Key;
                newPeer.LastKnownIP = socket.GetIP();
                newPeer.SharedPassword = signatureSecret;
                addPeer(newPeer);
                await StorePeers();
                RefreshPeerMap();
                return true;
            }
            catch(FileTransferException error)
            {
                DialogBoxes.ShowMessageBox(error.Message);
                return false;
            }
            catch(Exception error)
            {
                DialogBoxes.ShowMessageBox("Unknown error: " + error.Message);
                return false;
            }
            finally
            {
                progressStop();
            }
        }

        public async static Task Unpair(string guid)
        {
            Peer selectedPeer = peerMap[guid];
            Peer newPeer = new Peer(selectedPeer.guid, selectedPeer.Name);
            newPeer.LastKnownIP = selectedPeer.LastKnownIP;
            Peers.Remove(selectedPeer);
            Peers.Add(newPeer);
            RefreshPeerMap();
            await StorePeers();
        }
        private static void addPeer(Peer addedPeer)
        {
            if(peerMap.ContainsKey(addedPeer.Guid))
            {
                var oldPeer = peerMap[addedPeer.Guid];
                Peers.Remove(oldPeer);
                if (oldPeer.PublicKey != null && addedPeer.PublicKey == null)
                {
                    addedPeer.PublicKey = oldPeer.PublicKey;
                    addedPeer.MyPrivateKey = oldPeer.MyPrivateKey;
                }
                if (oldPeer.SharedPassword != null && addedPeer.SharedPassword == null)
                {
                    addedPeer.SharedPassword = oldPeer.SharedPassword;
                }
            }
            Peers.Add(addedPeer);
            RefreshPeerMap();
        }
        public async static Task SendFile(string fileName, byte[] fileContents, string peer)
        {
            try
            {
                progressIndeterminate();
                Peer selectedPeer = peerMap[peer];
                PreSendRequest preSend = new PreSendRequest();
                String filePrivateKey = DiffieHellman.generate_DH_Private();
                String filePublicKey = DiffieHellman.calculate_DH_Public(filePrivateKey);
                byte[] aesKey = DiffieHellman.calculate_DH_AES(selectedPeer.PublicKey, filePrivateKey);
                preSend.FileName = fileName;
                preSend.key = new PublicKey(filePublicKey, selectedPeer.SharedPassword);
                var preSendSocket = await sendRequestToPeer(peer, Strings.REQUEST_TYPE_PRE_SEND, JSONHandling.SerializeObject(preSend), timeout:0);
                var response = await preSendSocket.Recv();
                if (response == Strings.RESPONSE_NOT_PAIRED)
                {
                    if (!(await Pair(peer))) return;
                    await SendFile(fileName, fileContents, peer);
                    return;
                }
                else if (response == Strings.RESPONSE_REJECT)
                {
                    await DialogBoxes.ShowMessageBox("File rejected");
                    progressStop();
                    return;
                }
                File sentFile = new File(fileName, fileContents, aesKey);
                await sendRequestToPeer(peer, Strings.REQUEST_TYPE_SEND, JSONHandling.SerializeObject(sentFile), true);
                progressStop();
            }
            catch (FileTransferException error)
            {
                DialogBoxes.ShowMessageBox(error.Message);
            }
            catch (Exception error)
            {
                DialogBoxes.ShowMessageBox("Unknown error: " + error.Message);
            }
            finally
            {
                progressStop();
            }
        }
        public async static Task SendFileNoCrypt(string fileName, byte[] fileContents, string peer)
        {
            try
            {
                progressIndeterminate();
                PreSendRequestNoCrypt preSend = new PreSendRequestNoCrypt();
                preSend.FileName = fileName;
                var preSendSocket = await sendRequestToPeer(peer, Strings.REQUEST_TYPE_PRE_SEND_NOCRYPT, JSONHandling.SerializeObject(preSend), timeout: 0);
                var response = await preSendSocket.Recv();
                if (response == Strings.RESPONSE_NOT_PAIRED)
                {
                    if (!(await Pair(peer))) return;
                    await SendFile(fileName, fileContents, peer);
                    return;
                }
                else if (response == Strings.RESPONSE_REJECT)
                {
                    await DialogBoxes.ShowMessageBox("File rejected");
                    progressStop();
                    return;
                }
                File sentFile = new File(fileName, fileContents);
                await sendRequestToPeer(peer, Strings.REQUEST_TYPE_SEND_NOCRYPT, JSONHandling.SerializeObject(sentFile), true);
                progressStop();
            }
            catch (FileTransferException error)
            {
                DialogBoxes.ShowMessageBox(error.Message);
            }
            catch (Exception error)
            {
                DialogBoxes.ShowMessageBox("Unknown error: " + error.Message);
            }
            finally
            {
                progressStop();
            }
        }
        private async static Task fileReceived(string peer, string data, TCPSocket socket)
        {
            progressIndeterminate();
            if(!(peerMap.ContainsKey(peer) && peerMap[peer].Paired))
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_NOT_PAIRED);
                return;
            }
            if (!(preSendRegistrations.ContainsKey(peer)))
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_REJECT);
                return;
            }
            PreSendRequest registration = preSendRegistrations[peer];
            preSendRegistrations.Remove(peer);
            File receivedFile = JSONHandling.ParseJSONResponse<File>(data);
            if(receivedFile.FileName!=registration.FileName)
            {
                progressStop();
                throw new FileTransferException("File name mismatch");
            }
            byte[] aesKey = DiffieHellman.calculate_DH_AES(registration.key.Key, peerMap[peer].MyPrivateKey);
            byte[] fileContents = receivedFile.GetContents(aesKey);
            progressStop();
            FileReceived(receivedFile.FileName, fileContents);
        }

        private async static Task fileReceivedNoCrypt(string peer, string data, TCPSocket socket)
        {
            progressIndeterminate();
            if (!(peerMap.ContainsKey(peer) && peerMap[peer].Paired))
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_NOT_PAIRED);
                return;
            }
            if (!(preSendRegistrationsNoCrypt.ContainsKey(peer)))
            {
                progressStop();
                await socket.Send(Strings.RESPONSE_REJECT);
                return;
            }
            PreSendRequestNoCrypt registration = preSendRegistrationsNoCrypt[peer];
            preSendRegistrationsNoCrypt.Remove(peer);
            File receivedFile = JSONHandling.ParseJSONResponse<File>(data);
            if (receivedFile.FileName != registration.FileName)
            {
                progressStop();
                throw new FileTransferException("File name mismatch");
            }
            byte[] fileContents = receivedFile.GetContents();
            progressStop();
            FileReceived(receivedFile.FileName, fileContents);
        }

        private async static Task preSendRequestReceived(string peer, string data, TCPSocket socket)
        {
            if(peerMap.ContainsKey(peer) && peerMap[peer].Paired)
            {
                PreSendRequest registration = JSONHandling.ParseJSONResponse<PreSendRequest>(data);
                if(!registration.key.VerifySignature(peerMap[peer].SharedPassword))
                {
                    socket.Send(Strings.RESPONSE_REJECT);
                }
                await DialogBoxes.AskForConfirmation(registration.FileName+" from "+peerMap[peer].Name+". Accept?", async () =>
                {
                    preSendRegistrations[peer] = registration;
                    await socket.Send(Strings.RESPONSE_OK);
                }, async () =>
                {
                    await socket.Send(Strings.RESPONSE_REJECT);
                });
            }
            else
            {
                await socket.Send(Strings.RESPONSE_NOT_PAIRED);
            }
        }

        private async static Task preSendNoCryptRequestReceived(string peer, string data, TCPSocket socket)
        {
            if (peerMap.ContainsKey(peer) && peerMap[peer].Paired)
            {
                PreSendRequestNoCrypt registration = JSONHandling.ParseJSONResponse<PreSendRequestNoCrypt>(data);
                await DialogBoxes.AskForConfirmation(registration.FileName + " from " + peerMap[peer].Name + " over insecure connection. Accept?", async () =>
                {
                    preSendRegistrationsNoCrypt[peer] = registration;
                    await socket.Send(Strings.RESPONSE_OK);
                }, async () =>
                {
                    await socket.Send(Strings.RESPONSE_REJECT);
                });
            }
            else
            {
                await socket.Send(Strings.RESPONSE_NOT_PAIRED);
            }
        }

        public async static Task DeletePeer(string selectedGuid)
        {
            Peers.Remove(peerMap[selectedGuid]);
            peerMap.Remove(selectedGuid);
            await StorePeers();
        }
    }
}
