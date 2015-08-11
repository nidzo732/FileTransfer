using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.Serialization;
using System.Text;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Data;

namespace FileTransfer
{
    [DataContract]
    public class Peer
    {
        [DataMember(Name="guid")]
        public string guid;
        [DataMember(Name = "name")]
        private string name;
        [DataMember(Name = "myPrivate")]
        public string MyPrivateKey;
        [DataMember(Name = "publicKey")]
        public string PublicKey;
        [DataMember(Name = "sharedPassword")]
        public string SharedPassword;
        public string LastKnownIP;

        public string Guid
        {
            get { return guid; }
            set { guid = value; }
        }
        public string Name
        {
            get { return name; }
            set { name = value; }
        }
        public string SubText
        {
            get
            {
                string text = "";
                if (Paired) text += "Paired "; else text += "Unpaired ";
                if (Present)
                {
                    text+= "Available " + LastKnownIP;
                }
                else
                {
                    text+= "Unavaliable";
                }
                return text;
            }
        }
        public bool Paired
        {
            get
            {
                return PublicKey != null;
            }
        }
        public bool Present
        {
            get
            {
                return LastKnownIP != null;
            }
        }
        public Peer(string guid, string name)
        {
            Guid = guid;
            Name = name;
            MyPrivateKey = PublicKey = null;
        }
    }
}
