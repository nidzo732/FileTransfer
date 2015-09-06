package com.nidzo.filetransfer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {

    private Communicator communicator;
    private PeerListAdapter peersAdapter;
    private ListView peerList;
    private EditText deviceName;
    private ProgressBar progressIndicator;
    private FileHandling fileHandling;
    private String selectedPeerGuid;
    private Intent fileToSendIntent;
    private CheckBox enableEncryption;
    private TextWatcher deviceNameChangedWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            Identification.setName(deviceName.getText().toString(), getApplicationContext());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            fileToSendIntent = intent;
        }
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        peersAdapter = new PeerListAdapter(this);
        peerList = (ListView) findViewById(R.id.peerList);
        peerList.setAdapter(peersAdapter);
        deviceName = (EditText) findViewById(R.id.deviceName);
        progressIndicator = (ProgressBar) findViewById(R.id.progressIndicator);
        enableEncryption = (CheckBox)findViewById(R.id.enableEncryptionCheckbox);
        progressStop();
        deviceName.setText(Identification.getName(this));
        deviceName.addTextChangedListener(deviceNameChangedWatcher);
        try {
            communicator = new Communicator(this);
            communicator.discoverPeers();
            fileHandling = new FileHandling(this);
            updatePeerList();
        } catch (FileTransferException e) {
            DialogBoxes.showMessageBox("Error", "Failed to start " + e.getMessage(), this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        communicator.halt();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refreshPeers) {
            communicator.discoverPeers();
        }

        return true;
    }

    public void updatePeerList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peersAdapter.reset(communicator.getPeers());
            }
        });
    }

    public void progressIndeterminate() {
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(true);
    }

    public void progressReport(double progress) {
        progressIndicator.setIndeterminate(false);
        progressIndicator.setVisibility(View.VISIBLE);
        int progressValue = (int) (progress * 100);
        progressIndicator.setProgress(progressValue);
    }

    public void progressStop() {
        progressIndicator.setIndeterminate(false);
        progressIndicator.setVisibility(View.INVISIBLE);
        progressIndicator.setProgress(0);
    }

    public void deletePeer(String guid) {
        try {
            communicator.deletePeer(guid);
        } catch (FileTransferException e) {
            DialogBoxes.showMessageBox("Error", e.getMessage(), this);
        }
    }

    public void unpairPeer(String guid) {
        try {
            communicator.unpairPeer(guid);
        } catch (FileTransferException e) {
            DialogBoxes.showMessageBox("Error", e.getMessage(), this);
        }
    }

    public void pair(String guid) {
        communicator.pair(guid);
    }

    public void sendFile(String guid) {
        selectedPeerGuid = guid;
        if (fileToSendIntent != null) {
            communicator.sendFile(guid, fileToSendIntent, enableEncryption.isChecked());
            fileToSendIntent = null;
        } else fileHandling.selectFileToSend();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == FileHandling.FILE_SELECT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                communicator.sendFile(selectedPeerGuid, result, enableEncryption.isChecked());
            }
        }
    }

    public void fileReceived(final java.io.File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fileHandling.offerToOpenFile(file);
            }
        });
    }
}
