using System;
using System.Collections.Generic;
using System.Numerics;
using System.Text;
using Windows.Security.Cryptography;
using Windows.Security.Cryptography.Core;

namespace FileTransfer.Cryptography
{
    public class CryptographyException: FileTransferException
    {
        public CryptographyException(string message)
        {
            errorText = "Cryptographic error: " + message;
        }
    }
    public static class TextManipulation
    {
        public static string Encode64(byte[] data)
        {
            return Convert.ToBase64String(data);
        }
        public static byte[] Decode64(string data)
        {
            return Convert.FromBase64String(data);
        }
        public static byte[] EncodeUTF(string s)
        {
            return Encoding.UTF8.GetBytes(s);
        }
        public static string DecodeUTF(byte[] s)
        {
            return Encoding.UTF8.GetString(s, 0, s.Length);
        }
    }
    public static class DiffieHellman
    {

        public static string generate_DH_Private(uint length = 64)
        {
            BigInteger value = 0;
            while (calculate_DH_Public(value) == 1)
            {
                byte[] number;
                CryptographicBuffer.CopyToByteArray(CryptographicBuffer.GenerateRandom(length), out number);
                number[0] |= 128;
                value = NumberConversion.BytesToBigInteger(number);
            }
            return NumberConversion.BigIntegerToBase64(value);
        }
        public static BigInteger calculate_DH_Public(BigInteger exponent)
        {
            BigInteger publicNumber = BigInteger.ModPow(Constants.generator, exponent, Constants.modulus);
            return publicNumber;
        }
        public static string calculate_DH_Public(string privateKey)
        {
            return NumberConversion.BigIntegerToBase64(calculate_DH_Public(NumberConversion.Base64ToBigInteger(privateKey)));
        }
        private static byte[] calculate_DH_Result(string publicKey, string privateKey)
        {
            if(TextManipulation.Decode64(publicKey).Length<511)
            {
                throw new CryptographyException("Invalid public key");
            }
            BigInteger exponent = NumberConversion.Base64ToBigInteger(privateKey);
            BigInteger publicBase = NumberConversion.Base64ToBigInteger(publicKey);
            BigInteger result = BigInteger.ModPow(publicBase, exponent, Constants.modulus);
            return NumberConversion.BigIntegerToBytes(result);
        }
        public static byte[] calculate_DH_AES(string publicKey, string privateKey)
        {
            byte[] keySeed = calculate_DH_Result(publicKey, privateKey);
            byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            for (int i = 0; i < keySeed.Length; i++)
            {
                key[i % 16] ^= keySeed[i];
            }
            return key;
        }
    }
    static class HMAC
    {
        private static readonly MacAlgorithmProvider macProvider = MacAlgorithmProvider.OpenAlgorithm(MacAlgorithmNames.HmacSha256);
        public static byte[] GenerateHMAC(byte[] data, byte[] key)
        {
            var dataBuffer = CryptographicBuffer.CreateFromByteArray(data);
            var keyBuffer = CryptographicBuffer.CreateFromByteArray(key);
            var macKey = macProvider.CreateKey(keyBuffer);
            var macBuffer = CryptographicEngine.Sign(macKey, dataBuffer);
            byte[] mac;
            CryptographicBuffer.CopyToByteArray(macBuffer, out mac);
            return mac;
        }
        public static bool VerifyHMAC(byte[] data, byte[] key, byte[] hmac)
        {
            byte[] ownMac = GenerateHMAC(data, key);
            bool identical = true;
            for (int i = 0; i < ownMac.Length; i++) identical &= ownMac[i] == hmac[i];
            return identical;
        }
    }
    public static class AES
    {
        private static readonly SymmetricKeyAlgorithmProvider aesAlgo = SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithmNames.AesCbcPkcs7);
        public static string Encrypt(byte[] data, byte[] key)
        {
            var keyBuffer = CryptographicBuffer.CreateFromByteArray(key);
            var dataBuffer = CryptographicBuffer.CreateFromByteArray(data);

            var ivBuffer = CryptographicBuffer.GenerateRandom(aesAlgo.BlockLength);

            var aesKey = aesAlgo.CreateSymmetricKey(keyBuffer);

            var cipherTextBuffer = CryptographicEngine.Encrypt(aesKey, dataBuffer, ivBuffer);

            byte[] iv, ciphertext, hashedPart;
            CryptographicBuffer.CopyToByteArray(ivBuffer, out iv);
            CryptographicBuffer.CopyToByteArray(cipherTextBuffer, out ciphertext);

            hashedPart = new byte[iv.Length + ciphertext.Length];
            Array.Copy(iv, hashedPart, iv.Length);
            Array.Copy(ciphertext, 0, hashedPart, iv.Length, ciphertext.Length);
            byte[] mac = HMAC.GenerateHMAC(hashedPart, key);

            byte[] fullCipherText = new byte[hashedPart.Length + mac.Length];
            Array.Copy(hashedPart, fullCipherText, hashedPart.Length);
            Array.Copy(mac, 0, fullCipherText, hashedPart.Length, mac.Length);
            return TextManipulation.Encode64(fullCipherText);
        }
        public static byte[] Decrypt(string data, byte[] key)
        {
            var realData = TextManipulation.Decode64(data);

            byte[] iv = new byte[16];
            byte[] hashedPart = new byte[realData.Length - 32];
            byte[] mac = new byte[32];
            byte[] cipherText = new byte[realData.Length - 48];

            Array.Copy(realData, iv, 16);
            Array.Copy(realData, hashedPart, hashedPart.Length);
            Array.Copy(realData, hashedPart.Length, mac, 0, 32);
            Array.Copy(realData, 16, cipherText, 0, cipherText.Length);

            var keyBuffer = CryptographicBuffer.CreateFromByteArray(key);
            var macBuffer = CryptographicBuffer.CreateFromByteArray(mac);
            var ivBuffer = CryptographicBuffer.CreateFromByteArray(iv);
            var ciphertextBuffer = CryptographicBuffer.CreateFromByteArray(cipherText);
            if (!HMAC.VerifyHMAC(hashedPart, key, mac))
            {
                throw new CryptographyException("Received data is damaged");

            }
            var aesKey = aesAlgo.CreateSymmetricKey(keyBuffer);
            var decryptedMessageBuffer = CryptographicEngine.Decrypt(aesKey, ciphertextBuffer, ivBuffer);
            byte[] decryptedMessage;
            CryptographicBuffer.CopyToByteArray(decryptedMessageBuffer, out decryptedMessage);
            return decryptedMessage;
        }
    }
    public static class HASH
    {
        public static string SHA256Hash(byte[] data)
        {
            var hashedDataBuffer = CryptographicBuffer.CreateFromByteArray(data);
            var hashBuffer = HashAlgorithmProvider.OpenAlgorithm(HashAlgorithmNames.Sha256).HashData(hashedDataBuffer);
            return CryptographicBuffer.EncodeToBase64String(hashBuffer);
        }
        public static string SHA512Hash(byte[] data)
        {
            var hashedDataBuffer = CryptographicBuffer.CreateFromByteArray(data);
            var hashBuffer = HashAlgorithmProvider.OpenAlgorithm(HashAlgorithmNames.Sha512).HashData(hashedDataBuffer);
            return CryptographicBuffer.EncodeToBase64String(hashBuffer);
        }
    }
    public static class Signatures
    {
        public static string GenerateSignature(string secret, string publickey)
        {
            BigInteger signatureBase = NumberConversion.Base64ToBigInteger(publickey);
            string exponent64 = HASH.SHA512Hash(TextManipulation.EncodeUTF(secret));
            BigInteger exponent = NumberConversion.Base64ToBigInteger(exponent64);
            return NumberConversion.BigIntegerToBase64(BigInteger.ModPow(signatureBase, exponent, Constants.modulus));
        }
        public static bool VerifySignature(string secret, string publickey, string signature)
        {
            return GenerateSignature(secret, publickey) == signature;
        }
    }
    public static class Constants
    {
        public static readonly BigInteger modulus = BigInteger.Parse("1044388881413152506679602719846529545831269060992135009022588756444338172022322690710444046669809783930111585737890362691860127079270495454517218673016928427459146001866885779762982229321192368303346235204368051010309155674155697460347176946394076535157284994895284821633700921811716738972451834979455897010306333468590751358365138782250372269117968985194322444535687415522007151638638141456178420621277822674995027990278673458629544391736919766299005511505446177668154446234882665961680796576903199116089347634947187778906528008004756692571666922964122566174582776707332452371001272163776841229318324903125740713574141005124561965913888899753461735347970011693256316751660678950830027510255804846105583465055446615090444309583050775808509297040039680057435342253926566240898195863631588888936364129920059308455669454034010391478238784189888594672336242763795138176353222845524644040094258962433613354036104643881925238489224010194193088911666165584229424668165441688927790460608264864204237717002054744337988941974661214699689706521543006262604535890998125752275942608772174376107314217749233048217904944409836238235772306749874396760463376480215133461333478395682746608242585133953883882226786118030184028136755970045385534758453247");
        public static readonly BigInteger generator = 2;
    }
    public static class NumberConversion
    {
        public static BigInteger BytesToBigInteger(byte[] data)
        {
            BigInteger value = 0;
            foreach (var octet in data)
            {
                value *= 256;
                value += octet;
            }
            return value;
        }
        public static BigInteger Base64ToBigInteger(string base64)
        {
            return BytesToBigInteger(TextManipulation.Decode64(base64));
        }
        public static byte[] BigIntegerToBytes(BigInteger n)
        {
            byte[] byteNumber = new byte[4096];
            int lp = 4095;

            while (!n.IsZero)
            {
                byteNumber[lp] = (byte)(n % 256);
                n /= 256;
                lp--;
            }
            lp++;
            byte[] realNumber = new byte[4096 - lp];
            Array.Copy(byteNumber, lp, realNumber, 0, 4096 - lp);
            return realNumber;
        }
        public static string BigIntegerToBase64(BigInteger n)
        {

            return TextManipulation.Encode64(BigIntegerToBytes(n));
        }
    }
}
