using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Windows.Storage;

namespace FileTransfer
{
    class DataStorage
    {
        public async static Task<string> GetStoredItemAsync(string name)
        {
            var folder = ApplicationData.Current.LocalFolder;
            try
            {
                var file = await folder.GetFileAsync(name);
                string messages = await FileIO.ReadTextAsync(file);
                return messages;
            }
            catch
            {
                return null;
            }
        }
        public static async Task StoreItemAsync(string name, string value)
        {
            var folder = ApplicationData.Current.LocalFolder;
            if (value == null || value == "")
            {
                try
                {
                    var file = await folder.GetFileAsync(name);
                    await file.DeleteAsync(StorageDeleteOption.PermanentDelete);
                }
                catch { }
            }
            else
            {
                try
                {
                    var file = await folder.CreateFileAsync(name, CreationCollisionOption.ReplaceExisting);
                    await FileIO.WriteTextAsync(file, value);
                }
                catch { }
            }
        }
    }
}
