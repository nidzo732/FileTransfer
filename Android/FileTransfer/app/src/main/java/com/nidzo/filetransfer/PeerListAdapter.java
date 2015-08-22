package com.nidzo.filetransfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.nidzo.filetransfer.transferclasses.Peer;

import java.util.ArrayList;
import java.util.List;


public class PeerListAdapter extends ArrayAdapter<Peer> {
    private List<Peer> peers;
    private LayoutInflater inflater;
    private MainActivity owner;

    public PeerListAdapter(MainActivity owner) {
        super(owner, R.layout.peer_list_item, new ArrayList<Peer>());
        this.peers = new ArrayList<>();
        this.owner = owner;
        inflater = (LayoutInflater) owner.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void reset(List<Peer> newPeers) {
        this.clear();
        this.addAll(newPeers);
        this.peers = newPeers;
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        if (convertView != null) {
            rowView = convertView;
        } else {
            rowView = inflater.inflate(R.layout.peer_list_item, parent, false);
        }
        TextView peerNameView = (TextView) rowView.findViewById(R.id.peerName);
        TextView peerInfoView = (TextView) rowView.findViewById(R.id.peerInfo);
        ImageView statusIndicator = (ImageView) rowView.findViewById(R.id.statusIndicator);
        final Peer currentPeer = this.peers.get(position);
        peerNameView.setText(currentPeer.getName());
        String peerInfo = "";
        if (currentPeer.getMyPrivateKey() != null) {
            peerInfo += "Paired, ";
        } else peerInfo += "Not paired, ";
        if (currentPeer.getIP() != null) {
            peerInfo += "available " + currentPeer.getIP();
        } else peerInfo += "not available";
        peerInfoView.setText(peerInfo);
        if (currentPeer.getIP() != null) {
            if (currentPeer.getMyPrivateKey() != null) {
                statusIndicator.setImageResource(android.R.drawable.presence_online);
            } else {
                statusIndicator.setImageResource(android.R.drawable.presence_busy);
            }
        } else {
            if (currentPeer.getMyPrivateKey() != null) {
                statusIndicator.setImageResource(android.R.drawable.presence_away);
            } else {
                statusIndicator.setImageResource(android.R.drawable.presence_offline);
            }
        }
        View rootLayout = rowView.findViewById(R.id.rootLayout);
        rootLayout.setOnLongClickListener(new PeerLongClickListner(currentPeer, owner, rootLayout));
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPeer.getIP() != null) {
                    if (currentPeer.getSharedPassword() == null) {
                        owner.pair(currentPeer.getGuid());
                    } else {
                        owner.sendFile(currentPeer.getGuid());
                    }
                }
            }
        });
        return rowView;

    }

    class PeerLongClickListner implements View.OnLongClickListener {
        Peer peer;
        MainActivity ownerActivity;
        View ownerView;

        public PeerLongClickListner(Peer peer, MainActivity ownerActivity, View ownerView) {
            this.peer = peer;
            this.ownerActivity = ownerActivity;
            this.ownerView = ownerView;
        }

        @Override
        public boolean onLongClick(View v) {
            final PopupMenu peerMenu = new PopupMenu(ownerActivity, ownerView);
            peerMenu.inflate(R.menu.peer_menu);
            if (peer.getSharedPassword() != null) {
                peerMenu.getMenu().findItem(R.id.unpairPeer).setEnabled(true);
            } else peerMenu.getMenu().findItem(R.id.unpairPeer).setEnabled(false);
            peerMenu.getMenu().findItem(R.id.unpairPeer).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ownerActivity.unpairPeer(peer.getGuid());
                    return true;
                }
            });
            peerMenu.getMenu().findItem(R.id.deletePeer).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ownerActivity.deletePeer(peer.getGuid());
                    return true;
                }
            });
            peerMenu.show();
            return true;
        }
    }
}
