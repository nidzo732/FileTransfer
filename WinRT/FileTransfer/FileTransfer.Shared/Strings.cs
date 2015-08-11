using System;
using System.Collections.Generic;
using System.Text;

namespace FileTransfer
{
    public static class Strings
    {
        public const string JSON_GUID = "GUID",
                      JSON_NAME = "NAME",
                      JSON_PRIVATEKEY = "PRIVATEKEY",
                      JSON_PUBLICKEY = "PUBLICKEY",
                      JSON_REQUEST_TYPE = "REQ_TYPE",
                      JSON_REQUEST_DATA = "REQ_DATA",
                      JSON_SIGNATURE = "SIGNATURE",
                      JSON_FILE_CONTENTS = "FILE_CONTENTS",
                      JSON_FILE_NAME= "FILE_NAME",

                      RESPONSE_OK = "OK",
                      RESPONSE_REJECT = "REJECT",
                      RESPONSE_BAD_SIGNATURE = "BADSIGNATURE",
                      RESPONSE_NOT_PAIRED = "NOT_PAIRED",

                      REQUEST_TYPE_PAIR = "RQ_PAIR",
                      REQYEST_TYPE_PUBLICKEY = "RQ_PUBLICKEY",
                      REQUEST_TYPE_PRE_SEND = "RQ_PRE_SEND",
                      REQUEST_TYPE_SEND = "RQ_SEND";
    }
}
