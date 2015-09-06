using FileTransfer.Common;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Windows.ApplicationModel.Activation;
using Windows.Storage;
using Windows.Storage.Pickers;
using Windows.UI.ViewManagement;
using Windows.UI.Xaml.Controls;

namespace FileTransfer
{
    public sealed partial class MainPage : Page, IFileSavePickerContinuable, IFileOpenPickerContinuable
    {
        private NavigationHelper navigationHelper;
        private ObservableDictionary defaultViewModel = new ObservableDictionary();
        private byte[] fileContentsToWrite;
        public MainPage()
        {
            this.InitializeComponent();
            this.navigationHelper = new NavigationHelper(this);
            this.navigationHelper.LoadState += this.NavigationHelper_LoadState;
            this.navigationHelper.SaveState += this.NavigationHelper_SaveState;
        }

        public NavigationHelper NavigationHelper
        {
            get { return this.navigationHelper; }
        }

        public ObservableDictionary DefaultViewModel
        {
            get { return this.defaultViewModel; }
        }

        private void NavigationHelper_LoadState(object sender, LoadStateEventArgs e)
        {
        }

        private void NavigationHelper_SaveState(object sender, SaveStateEventArgs e)
        {
        }

        public void saveFile(string fileName, byte[] fileContents)
        {
            FileSavePicker picker = new FileSavePicker();
            picker.SuggestedStartLocation = PickerLocationId.Downloads;
            string extension, name;
            extension = name = "";
            for (int i = fileName.Length - 1; i >= 0; i--)
            {
                if (fileName[i] == '.')
                {
                    extension = fileName.Substring(i);
                    name = fileName.Substring(0, i);
                    break;
                }
            }
            if (extension == ".") extension += "noex";
            if (extension == "") extension = ".noex";
            if (name == "") name = fileName;
            picker.FileTypeChoices.Add("Downloaded file", new List<string>() { extension });
            picker.SuggestedFileName = name;
            fileContentsToWrite = fileContents;
            picker.PickSaveFileAndContinue();
        }
        public async void ContinueFileSavePicker(Windows.ApplicationModel.Activation.FileSavePickerContinuationEventArgs args)
        {
            byte[] contents = fileContentsToWrite;
            if (args.File != null && contents != null)
            {
                await FileIO.WriteBytesAsync(args.File, contents);
            }
        }
        string selectedPeer;
        private async Task sendFile(string peer)
        {
            selectedPeer = peer;
            FileOpenPicker picker = new FileOpenPicker();
            picker.SuggestedStartLocation = PickerLocationId.Downloads;
            picker.FileTypeFilter.Add("*");
            picker.PickSingleFileAndContinue();
        }
        public async void ContinueFileOpenPicker(FileOpenPickerContinuationEventArgs args)
        {
            if (args.Files.Count != 0)
            {
                var file = args.Files[0];
                await uploadFile(file, selectedPeer);
            }
        }
        private void progressRepport(int percentage)
        {
            progressStart();
            StatusBar.GetForCurrentView().ProgressIndicator.ProgressValue = ((double)percentage) / 100;
        }
        private void progressIndeterminate()
        {
            progressStart();
            StatusBar.GetForCurrentView().ProgressIndicator.ProgressValue = null;
        }
        private void progressStart()
        {
            StatusBar.GetForCurrentView().ProgressIndicator.ShowAsync();
        }

        private void progressStop()
        {
            StatusBar.GetForCurrentView().ProgressIndicator.HideAsync();
        }
    }
}
