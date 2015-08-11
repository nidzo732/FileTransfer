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
        [DataMember(Name = Strings.JSON_PUBLICKEY)]
        public string PublicKey;
        [DataMember(Name = Strings.JSON_SIGNATURE)]
        public string Signature;
        [DataMember(Name=Strings.JSON_FILE_NAME)]
        public string FileName;
        public File()
        {

        }
        public File(string fileName, byte[] fileContents, string signatureSecret, string peerKey)
        {
            string filePrivateKey = Cryptography.DiffieHellman.generate_DH_Private();
            PublicKey = Cryptography.DiffieHellman.calculate_DH_Public(filePrivateKey);
            Signature = Cryptography.Signatures.GenerateSignature(signatureSecret, PublicKey);
            byte[] encryptionKey = Cryptography.DiffieHellman.calculate_DH_AES(peerKey, filePrivateKey);
            Contents = Cryptography.AES.Encrypt(fileContents, encryptionKey);
            FileName = fileName;

        }
        public bool CheckSignature(string signatureSecret)
        {
            return Cryptography.Signatures.VerifySignature(signatureSecret, PublicKey, Signature);
        }
        public byte[] GetContents(string privateKey)
        {
            byte[] aesKey = Cryptography.DiffieHellman.calculate_DH_AES(PublicKey, privateKey);
            return Cryptography.AES.Decrypt(Contents, aesKey);
        }
    }
}