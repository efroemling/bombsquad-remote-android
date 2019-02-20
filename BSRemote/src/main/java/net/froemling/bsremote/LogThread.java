package net.froemling.bsremote;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.util.Pair;

public class LogThread extends Thread {
    private String _log;
    private String _version;
    private static boolean _sent = false;

    static void log(String s, Throwable e) {
        // only report the first error..
        if (!_sent) {
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (!s.equals("")) s += "\n";
                s += sw.toString();
            }

            new LogThread(ScanActivity.mContext, s).start();
            _sent = true;
        }
        // report all to the standard log
        Log.e("BSREMOTE", s);
        if (e != null) e.printStackTrace();
    }

    private LogThread(Context c, String s) {
        super("Error");
        _log = s;
        try {
            if (c == null) _version = "?";
            else _version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (NameNotFoundException ignored) {
        }
    }

    private String getQuery(List<Pair<String, String>> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Pair<String, String> pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(pair.first, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.second, "UTF-8"));
        }
        return result.toString();
    }

    public void run() {
        try {

            URL url = new URL("http://acrobattleserver.appspot.com/bsRemoteLog");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("POST");
            String userAgent = "BombSquad Remote " + _version + " (Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.MANUFACTURER
                    + " " + android.os.Build.MODEL + ")";
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("charset", "utf-8");

            List<Pair<String, String>> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new Pair<>("version", _version));
            nameValuePairs.add(new Pair<>("log", _log));
            String out = getQuery(nameValuePairs);
            byte[] postDataBytes = out.getBytes("UTF-8");

            OutputStream os = conn.getOutputStream();
            os.write(postDataBytes);
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.v("BSREMOTE", "Got response code " + responseCode + " on bsRemoteLog request");
            }

        } catch (IOException e) {
            Log.v("BSREMOTE", "ERR ON LogThread post");
        }
    }

}
