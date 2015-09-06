using FileTransfer.Common;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Windows.Storage;
using Windows.Storage.Pickers;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Input;


namespace FileTransfer
{
    public sealed partial class MainPage : Page
    {

        private NavigationHelper navigationHelper;
        private ObservableDictionary defaultViewModel = new ObservableDictionary();

        public ObservableDictionary DefaultViewModel
        {
            get { return this.defaultViewModel; }
        }

        public NavigationHelper NavigationHelper
        {
            get { return this.navigationHelper; }
        }


        public MainPage()
        {
            this.InitializeComponent();
            this.navigationHelper = new NavigationHelper(this);
            this.navigationHelper.LoadState += navigationHelper_LoadState;
            this.navigationHelper.SaveState += navigationHelper_SaveState;
        }

        private void navigationHelper_LoadState(object sender, LoadStateEventArgs e)
        {
        }

        private void navigationHelper_SaveState(object sender, SaveStateEventArgs e)
        {
        }

        private void openItemContextMenu(object sender, RightTappedRoutedEventArgs e)
        {
            
        }

        private void itemSelectionChanged(object sender, SelectionChangedEventArgs e)
        {

        }
        private void progressIndeterminate()
        {
            progressStart();
            progressIndicator.IsIndeterminate = true;
        }
        private void progressRepport(int x)
        {
            progressStart();
            progressIndicator.Value = x;
            progressIndicator.IsIndeterminate = false;
        }
        private void progressStart()
        {
            progressIndicator.Visibility = Visibility.Visible;
        }

        private void progressStop()
        {
            progressIndicator.Visibility = Visibility.Collapsed;
        }

        public async Task sendFile(string peer)
        {
            FileOpenPicker picker = new FileOpenPicker();
            picker.SuggestedStartLocation = PickerLocationId.Downloads;
            picker.FileTypeFilter.Add("*");
            var file = await picker.PickSingleFileAsync();
            await uploadFile(file, peer);
        }
        public async void saveFile(string fileName, byte[] fileContents)
        {
            FileSavePicker picker = new FileSavePicker();
            picker.SuggestedStartLocation = PickerLocationId.Downloads;
            string extension, name;
            extension = name = "";
            bool hasExtension = false;
            for (int i = 0; i < fileName.Length; i++)
            {
                if (hasExtension)
                {
                    extension += fileName[i];
                }
                else
                {
                    if (fileName[i] == '.')
                    {
                        extension += fileName[i];
                        hasExtension = true;
                    }
                    else
                    {
                        name += fileName[i];
                    }
                }
            }
            if (extension == ".") extension += "noex";
            if (extension == "") extension = ".noex";
            picker.FileTypeChoices.Add("Downloaded file", new List<string>() { extension });
            picker.SuggestedFileName = name;
            var file = await picker.PickSaveFileAsync();
            if (file != null)
            {
                await FileIO.WriteBytesAsync(file, fileContents);
            }
        }
    }
}
