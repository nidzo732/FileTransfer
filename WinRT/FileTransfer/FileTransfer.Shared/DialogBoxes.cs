using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Windows.UI.Popups;
using Windows.UI.Xaml.Controls;
using WinRTXamlToolkit.Controls;

namespace FileTransfer
{
    class DialogBoxes
    {
        public async static Task ShowMessageBox(string message)
        {
            await (new MessageDialog(message)).ShowAsync();
        }
        public async static Task<string> AskForInput(string title, string text, bool allowCancel=true)
        {
            InputDialog dialog = new InputDialog();
            /*dialog.Background = Colors.BackgroundColor;
            dialog.Foreground = Colors.TextColor;*/
            dialog.ButtonStyle = new Button().Style;
            string response;
            if (allowCancel)
            {
                response = await dialog.ShowAsync(title,
                text,
                "OK",
                "Cancel");
            }
            else
            {
                response = await dialog.ShowAsync(title,
                text,
                "OK");
            }
            if (response == "OK")
                return dialog.InputText;
            else return null;
        }
        public static async Task AskForConfirmation(string message, Action onAccepted, Action onRejected=null)
        {
            MessageDialog dialog = new MessageDialog(message);
            dialog.Commands.Add(new UICommand("Yes", new UICommandInvokedHandler((a) => { onAccepted(); })));
            if(onRejected!=null)
            {
                dialog.Commands.Add(new UICommand("No", new UICommandInvokedHandler((a)=>{onRejected();})));
            }
            else dialog.Commands.Add(new UICommand("No"));
            await dialog.ShowAsync();
        }
    }
}
