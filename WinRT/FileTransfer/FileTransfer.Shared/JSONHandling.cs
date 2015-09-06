using Newtonsoft.Json;
namespace FileTransfer
{
    public static class JSONHandling
    {
        public static ResponseType ParseJSONResponse<ResponseType>(string response)
        {
            if (response == null) return default(ResponseType);
            return JsonConvert.DeserializeObject<ResponseType>(response);
        }
        public static string SerializeObject(object instance)
        {
            return JsonConvert.SerializeObject(instance);
        }
    }
}