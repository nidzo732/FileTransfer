using FileTransfer.Cryptography;
using Newtonsoft.Json;
using System.IO;
using System.Runtime.Serialization.Json;
namespace FileTransfer
{
    public static class JSONHandling
    {
        public static ResponseType ParseJSONResponse<ResponseType>(string response)
        {
            if (response == null) return default(ResponseType);
            /*DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(ResponseType));
            byte[] responseStreamStore = TextManipulation.EncodeUTF(response);
            MemoryStream responseStream = new MemoryStream(responseStreamStore);
            object parsedResponseObject = deserializer.ReadObject(responseStream);
            ResponseType parsedResponse = (ResponseType)parsedResponseObject;
            return parsedResponse;*/
            return JsonConvert.DeserializeObject<ResponseType>(response);
        }
        public static string SerializeObject(object instance)
        {
            /*DataContractJsonSerializer serializer = new DataContractJsonSerializer(instance.GetType());
            MemoryStream objectStream = new MemoryStream();
            serializer.WriteObject(objectStream, instance);
            byte[] serialized = objectStream.ToArray();
            return TextManipulation.DecodeUTF(serialized);*/
            return JsonConvert.SerializeObject(instance);
        }
    }
}