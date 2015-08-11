using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Windows.Networking.Connectivity;
using Windows.Storage;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Navigation;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.ApplicationModel.DataTransfer.ShareTarget;

namespace FileTransfer
{
    public sealed partial class MainPage : Page
    {
        private static string myName;
        private static string selectedGuid;
        private StorageFile fileToShare;
        private ShareOperation shareOperation;
        protected async override void OnNavigatedTo(NavigationEventArgs e)
        {
            if (await DeviceName == null)
            {
                myName = "Unnamed Device";
            }
            else
            {
                myName = await DataStorage.GetStoredItemAsync("myName");
            }
            if (await DeviceName == null)
            {
                deviceName.Text = "";
            }
            else deviceName.Text = myName;
            deviceName.TextChanged += setNewName;
            try
            {
                if ((await ((ShareOperation)e.Parameter).Data.GetStorageItemsAsync()).Count > 0)
                {
                    fileToShare = (StorageFile)(await ((ShareOperation)e.Parameter).Data.GetStorageItemsAsync())[0];
                    shareOperation = (ShareOperation)e.Parameter;
                }
                else fileToShare = null;
            }
            catch (InvalidCastException)
            {
                fileToShare = null;
            }
            progressStop();
            Communicator.FileReceived += this.saveFile;
            Communicator.ProgressRepport += this.progressRepport;
            Communicator.ProgressIndeterminate += this.progressIndeterminate;
            Communicator.ProgressStop += this.progressStop;
            if (NetworkInformation.GetInternetConnectionProfile() == null)
            {
                await DialogBoxes.ShowMessageBox("No network access, please enable network and restart");
            }
            else
            {
                await Communicator.Init(myName);
                await Communicator.DiscoverDevices();
                peerList.DataContext = Communicator.Peers;
            }
        }
        private async void rebootNetworking(object sender, RoutedEventArgs e)
        {
            await Communicator.Reboot();
            peerList.DataContext = Communicator.Peers;
        }   
        private Task<string> DeviceName
        {
            get
            {
                return DataStorage.GetStoredItemAsync("myName");
            }
        }
        private async void setNewName(object sender, TextChangedEventArgs e)
        {
            myName = deviceName.Text;
            if (myName == "") myName = "Unnamed device";
            await DataStorage.StoreItemAsync("myName", myName);
            Communicator.SetNewName(myName);
        }
        private async void refreshPeerList(object sender, RoutedEventArgs e)
        {
            await Communicator.DiscoverDevices();
        }
        private  async void peerSelected(object sender, ItemClickEventArgs e)
        {
            Peer selectedPeer = (Peer)e.ClickedItem;
            if (!selectedPeer.Paired)
            {
                await Communicator.Pair(selectedPeer.Guid);
            }
            else if(selectedPeer.Present)
            {
                if (fileToShare == null)
                {
                    await sendFile(selectedPeer.guid);
                }
                else
                {
                    await uploadFile(fileToShare, selectedPeer.Guid);
                    shareOperation.ReportCompleted();
                }
            }
        }
        private async void unpairPeer(object sender, RoutedEventArgs e)
        {
            await Communicator.Unpair(selectedGuid);
        }
        private async void deletePeer(object sender, RoutedEventArgs e)
        {
            await Communicator.DeletePeer(selectedGuid);
        } 
        private void peerLongClick(object sender, HoldingRoutedEventArgs e)
        {
            StackPanel clickedItemContents = (StackPanel)sender;
            selectedGuid = ((TextBlock)clickedItemContents.Children[0]).Text;
            showPeerContextMenu(sender, Communicator.GetPeer(selectedGuid));
            e.Handled = true;
        }

        private async Task uploadFile(StorageFile file, string peer)
        {
            if (file == null) return;
            if (shareOperation != null) shareOperation.ReportStarted();
            var fileSize = (await file.GetBasicPropertiesAsync()).Size;
            if ( fileSize> 20 * 1024 * 1024)
            {
                await DialogBoxes.AskForConfirmation("File may be too big to handle. The file size is "+(fileSize/1024/1024).ToString()+"MB. Files bigger than 20 MB may cause the application to crash. Proceed at your own risk?",
                    async () =>
                    {
                        byte[] fileContents = (await FileIO.ReadBufferAsync(file)).ToArray();
                        if (shareOperation != null) shareOperation.ReportDataRetrieved();
                        await Communicator.SendFile(file.Name, fileContents, peer);
                    });
            }
            else
            {
                byte[] fileContents = (await FileIO.ReadBufferAsync(file)).ToArray();
                await Communicator.SendFile(file.Name, fileContents, peer);
            }
        }

        private void peerRightClick(object sender, RightTappedRoutedEventArgs e)
        {
            StackPanel clickedItemContents = (StackPanel)sender;
            selectedGuid = ((TextBlock)clickedItemContents.Children[0]).Text;
            showPeerContextMenu(sender, Communicator.GetPeer(selectedGuid));
            e.Handled = true;
        }

        private void showPeerContextMenu(object sender, Peer peer)
        {
            peerContextMenu.Items[0].IsEnabled = (peer.Paired);            
            peerContextMenu.ShowAt((FrameworkElement)sender);
        }
    }
}
