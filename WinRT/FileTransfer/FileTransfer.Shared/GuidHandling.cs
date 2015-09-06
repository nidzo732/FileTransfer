using System;
using System.Threading.Tasks;

namespace FileTransfer
{
    class GuidHandling
    {
        public async static Task<string> GetMyGuid()
        {
            if (await DataStorage.GetStoredItemAsync("GUID") == null)
            {
                string guid = Guid.NewGuid().ToString();
                await DataStorage.StoreItemAsync("GUID", guid);
                return guid;
            }
            else
            {
                return await DataStorage.GetStoredItemAsync("GUID");
            }
        }
    }
}
