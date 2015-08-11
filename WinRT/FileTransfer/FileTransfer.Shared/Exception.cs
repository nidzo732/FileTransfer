using System;
using System.Collections.Generic;
using System.Text;

namespace FileTransfer
{
    public class FileTransferException: Exception
    {
        protected string errorText;
        public string Message
        {
            get
            {
                return errorText;
            }
        }
    }
}
