package com.skyncar.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.*;
import android.content.pm.*;
import android.media.session.*;
import android.media.MediaMetadata;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import org.json.*;
import java.util.*;

/** Local-only launcher shell. No remote web page is given access to the bridge. */
public class MainActivity extends Activity {
    private WebView web;
    private android.content.SharedPreferences prefs;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("skyncar", MODE_PRIVATE);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        web=new WebView(this);
        web.setBackgroundColor(0xff08090d);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "SkyNative");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){ return true; }
        });
        setContentView(web);
        // android_asset is available even with general file access disabled.
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onBackPressed(){ web.evaluateJavascript("window.go && window.go('home')",null); }
    @Override protected void onDestroy(){ web.removeJavascriptInterface("SkyNative"); web.destroy(); super.onDestroy(); }
    private void message(final String text){ runOnUiThread(new Runnable(){ public void run(){Toast.makeText(MainActivity.this,text,Toast.LENGTH_LONG).show();} }); }
    private void launch(final Intent i){ runOnUiThread(new Runnable(){ public void run(){try { startActivity(i); } catch(Exception e){message("Aplicación no disponible en este radio");}} }); }
    private boolean packageLaunch(String name){ Intent i=getPackageManager().getLaunchIntentForPackage(name); if(i==null)return false; launch(i);return true; }
    private MediaController controller(){
        try {
            MediaSessionManager m=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> cs=m.getActiveSessions(new ComponentName(this,MediaListener.class));
            for(MediaController c:cs) if(c.getPlaybackState()!=null && c.getPlaybackState().getState()==PlaybackState.STATE_PLAYING)return c;
            return cs.isEmpty()?null:cs.get(0);
        }catch(SecurityException e){return null;}
    }
    public class Bridge {
        @JavascriptInterface public String load(){return prefs.getString("profile","{}");}
        @JavascriptInterface public void save(String json){try {new JSONObject(json); if(json.length()<10000)prefs.edit().putString("profile",json).apply();}catch(JSONException ignored){} }
        @JavascriptInterface public void open(String action){
            switch(action){
                case "maps": if(!packageLaunch("com.waze") && !packageLaunch("com.google.android.apps.maps"))launch(new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=")));break;
                case "music": if(!packageLaunch("com.spotify.music") && !packageLaunch("com.google.android.apps.youtube.music"))launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC));break;
                case "phone": launch(new Intent(Intent.ACTION_DIAL)); break;
                case "bluetooth": launch(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));break;
                case "settings": launch(new Intent(Settings.ACTION_SETTINGS));break;
                case "mediaPermission": launch(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));break;
                case "homeSettings": launch(new Intent(Settings.ACTION_HOME_SETTINGS));break;
                case "radio":
                    String name=prefs.getString("radioPackage","");
                    if(name.isEmpty() || !packageLaunch(name))message("Elige la aplicación de radio FM en Aplicaciones");break;
            }
        }
        @JavascriptInterface public String apps(){
            JSONArray result=new JSONArray();
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps=getPackageManager().queryIntentActivities(query,0);
            Collections.sort(apps,new ResolveInfo.DisplayNameComparator(getPackageManager()));
            Set<String> seen=new HashSet<>();
            for(ResolveInfo a:apps){String p=a.activityInfo.packageName;if(p.equals(getPackageName())||!seen.add(p))continue;
                try {result.put(new JSONObject().put("name",a.loadLabel(getPackageManager()).toString()).put("package",p));}catch(JSONException ignored){} }
            return result.toString();
        }
        @JavascriptInterface public void app(String pkg){packageLaunch(pkg);}
        @JavascriptInterface public void radioApp(String pkg){if(getPackageManager().getLaunchIntentForPackage(pkg)!=null){prefs.edit().putString("radioPackage",pkg).apply();message("Aplicación de radio guardada");}}
        @JavascriptInterface public String media(){
            JSONObject o=new JSONObject(); MediaController c=controller();
            try {o.put("available",c!=null);if(c!=null){MediaMetadata m=c.getMetadata();PlaybackState s=c.getPlaybackState();
                o.put("title",m==null?"":m.getString(MediaMetadata.METADATA_KEY_TITLE));o.put("artist",m==null?"":m.getString(MediaMetadata.METADATA_KEY_ARTIST));o.put("playing",s!=null&&s.getState()==PlaybackState.STATE_PLAYING);}}
            catch(JSONException ignored){}return o.toString();
        }
        @JavascriptInterface public void mediaControl(String action){MediaController c=controller();if(c==null){message("Activa el acceso multimedia y reproduce música en una app");return;}
            MediaController.TransportControls t=c.getTransportControls();
            switch(action){case "previous":t.skipToPrevious();break;case "next":t.skipToNext();break;case "toggle":if(c.getPlaybackState()!=null&&c.getPlaybackState().getState()==PlaybackState.STATE_PLAYING)t.pause();else t.play();break;}
        }
    }
}
