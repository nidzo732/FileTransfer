using System.Runtime.Serialization;
namespace FileTransfer
{
    [DataContract]
    public class Request
    {
        [DataMember(Name = Strings.JSON_REQUEST_TYPE)]
        public string Type;
        [DataMember(Name = Strings.JSON_REQUEST_DATA)]
        public string Data;
        [DataMember(Name = Strings.JSON_GUID)]
        public string SenderGuid;
    }
    [DataContract]
    public class PreSendRequest
    {
        [DataMember(Name = Strings.JSON_FILE_NAME)]
        public string FileName;
        [DataMember(Name = Strings.JSON_PUBLICKEY)]
        public PublicKey key;
    }
    [DataContract]
    public class PreSendRequestNoCrypt
    {
        [DataMember(Name = Strings.JSON_FILE_NAME)]
        public string FileName;
    }
    [DataContract]
    public class PublicKey
    {
        [DataMember(Name=Strings.JSON_PUBLICKEY)]
        public string Key;
        [DataMember(Name=Strings.JSON_SIGNATURE)]
        public string Signature;
        public bool VerifySignature(string secret)
        {
            return Cryptography.Signatures.VerifySignature(secret, Key, Signature);
        }
        public PublicKey(string key, string secret)
        {
            Key = key;
            Signature = Cryptography.Signatures.GenerateSignature(secret, key);
        }
        public PublicKey()
        {

        }
    }
    [DataContract]
    public class PairingRequest
    {
        [DataMember(Name = Strings.JSON_NAME)]
        public string Name;
        [DataMember(Name = Strings.JSON_GUID)]
        public string Guid;
        [DataMember(Name = Strings.JSON_PUBLICKEY)]
        public PublicKey PublicKey;
    }
    [DataContract]
    public class File
    {
        [DataMember(Name = Strings.JSON_FILE_CONTENTS)]
        public string Contents;
        [DataMember(Name=Strings.JSON_FILE_NAME)]
        public string FileName;
        public File()
        {

        }
        public File(string fileName, byte[] fileContents, byte[] aesKey)
        {
            string filePrivateKey = Cryptography.DiffieHellman.generate_DH_Private();
            Contents = Cryptography.AES.Encrypt(fileContents, aesKey);
            FileName = fileName;

        }
        public File(string fileName, byte[] fileContents)
        {
            Contents = Cryptography.TextManipulation.Encode64(fileContents);
            FileName = fileName;
        }
        public byte[] GetContents(byte[] aesKey)
        {
            return Cryptography.AES.Decrypt(Contents, aesKey);
        }
        public byte[] GetContents()
        {
            return Cryptography.TextManipulation.Decode64(Contents);
        }
    }
}