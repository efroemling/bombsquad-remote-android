package net.froemling.bsremote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ScanActivity extends Activity {

    // UGLY; this currently gets used by LogThread
    static public Context mContext = null;

    public final static boolean debug = false;

    public final static String TAG = "SCAN";

    static final int BS_PACKET_REMOTE_GAME_QUERY = 8;
    static final int BS_PACKET_REMOTE_GAME_RESPONSE = 9;

    private Timer _processTimer;

    private WorkerThread _scannerThread;
    private WorkerThread _stopperThread;
    private WorkerThread _readThread;

    private DatagramSocket _scannerSocket;

    class _ServerEntry {
        InetAddress address;
        int port;
        long lastPingTime;
    }

    class ObjRunnable<T> implements Runnable {
        T obj;

        ObjRunnable(T objIn) {
            obj = objIn;
        }

        public void run() {
        }
    }

    Map<String, _ServerEntry> _serverEntries;

    protected ListView _listView;
    protected LibraryAdapter _adapter;

    static String playerName = "The Dude";

    static public String getPlayerName() {
        return playerName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (debug)
            Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        mContext = this.getApplicationContext();

        // we want to use our longer title here but we cant set it in the manifest
        // since it carries over to our icon; grumble.
        setTitle(R.string.app_name);

        setContentView(R.layout.gen_list);

        _scannerThread = new WorkerThread();
        _scannerThread.start();

        _stopperThread = new WorkerThread();
        _stopperThread.start();

        this._adapter = new LibraryAdapter(this);
        this._listView = this.findViewById(android.R.id.list);
        this._listView.addHeaderView(_adapter.headerView, null, false);
        this._listView.setAdapter(_adapter);

        // pull our player name from prefs
        final SharedPreferences preferences = getSharedPreferences("BSRemotePrefs", Context.MODE_PRIVATE);
        String playerNameVal = preferences.getString("playerName", "");
        assert playerNameVal != null;
        if (!playerNameVal.equals("")) {
            playerName = playerNameVal;
        }

        EditText editText = (EditText) _adapter.headerView.findViewById(R.id.nameEditText);
        if (editText != null) {

            // hmmm this counts emoji as 2 still but whatever...
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});

            editText.setText(getPlayerName());
            editText.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    playerName = s.toString();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("playerName", playerName);
                    editor.apply();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        this._listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();

                _scannerThread.doRunnable(new ObjRunnable<String>(name) {
                    public void run() {
                        if (_serverEntries.containsKey(obj)) {
                            _ServerEntry se = _serverEntries.get(obj);
                            Intent myIntent = new Intent(ScanActivity.this, GamePadActivity.class);
                            myIntent.putExtra("connectAddrs", new String[]{se.address.getHostName()});
                            myIntent.putExtra("connectPort", se.port);
                            myIntent.putExtra("newStyle", true);
                            startActivity(myIntent);

                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.act_scan, menu);

        return true;
    }

    @Override
    public void onDestroy() {
        if (debug)
            Log.v(TAG, "onDestroy()");
        super.onDestroy();
        // have the worker threads shut themselves down
        _scannerThread.doRunnable(new Runnable() {
            public void run() {
                _scannerThread.getLooper().quit();
                _scannerThread = null;

                // have the worker threads shut themselves down
                _stopperThread.doRunnable(new Runnable() {
                    public void run() {
                        _stopperThread.getLooper().quit();
                        _stopperThread = null;
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        if (debug) {
            Log.v(TAG, "onStart()");
        }
        super.onStart();

        _adapter._knownGames.clear();
        _adapter.notifyDataSetChanged();

        _serverEntries = new HashMap<>();

        try {
            _scannerSocket = new DatagramSocket();
            _scannerSocket.setBroadcast(true);
        } catch (SocketException e1) {
            LogThread.log("Error setting up scanner socket", e1);
        }

        _readThread = new WorkerThread();
        _readThread.start();

        // all the read-thread does is wait for data to come in
        // and pass it to the process-thread
        _readThread.doRunnable(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        _scannerSocket.receive(packet);
                        abstract class PacketRunnable implements Runnable {
                            DatagramPacket p;
                            PacketRunnable(DatagramPacket pIn) {
                                p = pIn;
                            }
                            public abstract void run();
                        }
                        _scannerThread.doRunnable(new PacketRunnable(packet) {
                            public void run() {

                                // if this is a game response packet...
                                if (p.getData().length > 1 && p.getData()[0] == BS_PACKET_REMOTE_GAME_RESPONSE) {
                                    // extract name
                                    String s = new String(Arrays.copyOfRange(p.getData(), 1, p.getLength()));

                                    // if this one isnt on our list, add it and
                                    // inform the ui of its existence
                                    if (!_serverEntries.containsKey(s)) {
                                        _serverEntries.put(s, new _ServerEntry());
                                        // hmm should we store its address only
                                        // when adding or every time
                                        // we hear from them?..
                                        _serverEntries.get(s).address = p.getAddress();
                                        _serverEntries.get(s).port = p.getPort();
                                        runOnUiThread(new ObjRunnable<String>(s) {
                                            public void run() {
                                                _adapter.notifyFound(obj);
                                            }
                                        });
                                    }
                                    _serverEntries.get(s).lastPingTime = SystemClock.uptimeMillis();
                                }
                            }
                        });

                    } catch (IOException e) {
                        // assuming this means the socket is closed..
                        _readThread.getLooper().quit();
                        _readThread = null;
                        break;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        LogThread.log("Got excessively sized datagram packet", e);
                    }
                }
            }
        });

        // start our timer to send out query packets, etc
        _processTimer = new Timer();
        _processTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                // when this timer fires, tell our process thread to run
                _scannerThread.doRunnable(new Runnable() {
                    public void run() {

                        // send broadcast packets to all our network interfaces to find games
                        try {

                            byte[] sendData = new byte[1];
                            sendData[0] = BS_PACKET_REMOTE_GAME_QUERY;

                            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                            while (interfaces.hasMoreElements()) {
                                NetworkInterface networkInterface = interfaces.nextElement();

                                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                                    continue;
                                }

                                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                                    InetAddress broadcast = interfaceAddress.getBroadcast();
                                    if (broadcast == null) {
                                        continue;
                                    }
                                    try {
                                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 43210);
                                        _scannerSocket.send(sendPacket);
                                    } catch (Exception e) {
                                        Log.v(TAG, "Broadcast datagram send error");
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            LogThread.log("", ex);
                        }

                        // prune servers we haven't heard from in a while..
                        long curTime = SystemClock.uptimeMillis();
                        Iterator<Entry<String, _ServerEntry>> it = _serverEntries.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, _ServerEntry> thisEntry = it.next();
                            long age = curTime - thisEntry.getValue().lastPingTime;
                            if (age > 5000) {
                                runOnUiThread(new ObjRunnable<String>(thisEntry.getKey()) {
                                    public void run() {
                                        _adapter.notifyLost(obj);
                                    }
                                });
                                it.remove();
                            }
                        }

                    }
                });
            }
        }, 0, 1000);

    }

    @Override
    public void onStop() {
        if (debug) {
            Log.v(TAG, "onStop()");
        }
        super.onStop();
        _processTimer.cancel();
        _processTimer.purge();
        _scannerSocket.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_search_by_ip) {

            // Creating the AlertDialog object
            final AlertDialog.Builder ipDialog = new AlertDialog.Builder(this);

            LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(R.layout.dialog, null);

            final SharedPreferences preferences = getSharedPreferences("BSRemotePrefs", Context.MODE_PRIVATE);
            String addrVal = preferences.getString("manualConnectAddress", "");

            // set up EditText to get input
            final EditText editText = view.findViewById(R.id.editText);
            editText.setText(addrVal);

            editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

            // handle OK clicks
            ipDialog.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    String ipFromUser = editText.getText().toString().trim();

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("manualConnectAddress", ipFromUser);
                    editor.apply();

                    Intent myIntent = new Intent(ScanActivity.this, GamePadActivity.class);
                    myIntent.putExtra("connectAddrs", new String[]{ipFromUser});
                    myIntent.putExtra("connectPort", 43210);
                    myIntent.putExtra("newStyle", true);
                    startActivity(myIntent);
                }
            });

            // handle cancel clicks
            ipDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // do nothing
                }
            });
            ipDialog.setView(view);

            ipDialog.show();
            return true;
        } else {
            return false;
        }
    }

    public class LibraryAdapter extends BaseAdapter {
        Context _context;
        LayoutInflater _inflater;
        View headerView;

        final LinkedList<String> _knownGames = new LinkedList<>();

        LibraryAdapter(Context context) {
            this._context = context;
            this._inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.headerView = _inflater.inflate(R.layout.item_network, null, false);
        }

        void notifyLost(String gameName) {
            if (Looper.getMainLooper().getThread() != Thread.currentThread())
                throw new AssertionError();
            if (_knownGames.contains(gameName)) {
                _knownGames.remove(gameName);
                notifyDataSetChanged();
            }
        }

        void notifyFound(String gameName) {
            if (Looper.getMainLooper().getThread() != Thread.currentThread())
                throw new AssertionError();
            if (!_knownGames.contains(gameName)) {
                _knownGames.add(gameName);
                notifyDataSetChanged();
            }
        }

        public Object getItem(int position) {
            if (!_knownGames.isEmpty()) {
                return _knownGames.get(position);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return _knownGames.size();
        }

        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("SetTextI18n")
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = _inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                TextView tv = convertView.findViewById(android.R.id.text1);
                tv.setPadding(40, 0, 0, 0);
                tv.setTextColor(0xFFCCCCFF);
            }
            try {
                String gameName = (String) this.getItem(position);
                TextView tv = convertView.findViewById(android.R.id.text1);
                tv.setText(gameName);

            } catch (Exception e) {
                LogThread.log("Problem getting zero-conf info", e);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText("Unknown");
            }
            return convertView;
        }
    }
}
