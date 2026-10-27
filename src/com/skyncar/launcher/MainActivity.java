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
    @Override protected void onResume(){
        super.onResume();
        if(web!=null)web.evaluateJavascript("window.refreshInstalledApps && window.refreshInstalledApps()",null);
    }
    private boolean assigned(String slot){
        String value=prefs.getString("app_"+slot,"");
        if(value.isEmpty())return false;
        ComponentName component=ComponentName.unflattenFromString(value);
        if(component==null)return packageLaunch(value);
        try {
            ActivityInfo info=getPackageManager().getActivityInfo(component,0);
            if(!info.exported || !info.enabled || !info.applicationInfo.enabled)return false;
            launch(new Intent(Intent.ACTION_MAIN).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }catch(PackageManager.NameNotFoundException e){return false;}
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
                case "maps": if(assigned("navigation"))break; if(!packageLaunch("com.waze") && !packageLaunch("com.google.android.apps.maps"))launch(new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=")));break;
                case "music": if(assigned("music"))break; if(!packageLaunch("com.spotify.music") && !packageLaunch("com.google.android.apps.youtube.music"))launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC));break;
                case "phone": if(assigned("phone"))break; launch(new Intent(Intent.ACTION_DIAL)); break;
                case "bluetooth": if(assigned("bluetooth"))break; launch(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));break;
                case "settings": launch(new Intent(Settings.ACTION_SETTINGS));break;
                case "mediaPermission": launch(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));break;
                case "homeSettings": launch(new Intent(Settings.ACTION_HOME_SETTINGS));break;
                case "waze": if(!packageLaunch("com.waze"))message("Waze no está instalado o no está disponible");break;
                case "googleMaps": if(!packageLaunch("com.google.android.apps.maps"))message("Google Maps no está instalado o no está disponible");break;
                case "factory": if(!assigned("factory"))message("Elige el inicio original en Ajustes de SkyNCar");break;
                case "radio":
                    if(assigned("radio"))break;
                    String name=prefs.getString("radioPackage","");
                    if(name.isEmpty() || !packageLaunch(name))message("Elige la aplicación de radio FM en SkyNCar → Radio FM");break;
            }
        }
        @JavascriptInterface public String apps(){
            JSONArray result=new JSONArray();
            PackageManager pm=getPackageManager();
            List<ResolveInfo> list=new ArrayList<>();
            for(String category:new String[]{Intent.CATEGORY_LAUNCHER,Intent.CATEGORY_INFO,Intent.CATEGORY_HOME}){
                list.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0));
            }
            Collections.sort(list,new ResolveInfo.DisplayNameComparator(pm));
            Set<String> seen=new HashSet<>();
            for(ResolveInfo a:list){
                ActivityInfo info=a.activityInfo;
                ComponentName component=new ComponentName(info.packageName,info.name);
                String id=component.flattenToString();
                if(info.packageName.equals(getPackageName()) || !info.exported || !info.enabled || !info.applicationInfo.enabled || !seen.add(id))continue;
                try {
                    android.graphics.drawable.Drawable drawable=a.loadIcon(pm);
                    android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
                    drawable.setBounds(0,0,64,64);drawable.draw(canvas);
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);bitmap.recycle();
                    result.put(new JSONObject().put("name",a.loadLabel(pm).toString())
                        .put("package",info.packageName).put("component",id)
                        .put("system",(info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0)
                        .put("icon","data:image/png;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP)));
                }catch(Exception ignored){
                    try{result.put(new JSONObject().put("name",info.packageName).put("package",info.packageName).put("component",id).put("system",(info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0));}catch(JSONException ignoredAgain){}
                }
            }
            return result.toString();
        }
        @JavascriptInterface public void app(String id){
            ComponentName c=ComponentName.unflattenFromString(id);
            if(c==null){if(!packageLaunch(id))message("Aplicación no disponible");return;}
            try{ActivityInfo info=getPackageManager().getActivityInfo(c,0);
                if(info.exported && info.enabled && info.applicationInfo.enabled)launch(new Intent(Intent.ACTION_MAIN).setComponent(c));
                else message("Esta actividad no está disponible");
            }catch(PackageManager.NameNotFoundException e){message("La aplicación ya no está instalada");}
        }
        @JavascriptInterface public void setAssignment(String slot,String id){
            if(!Arrays.asList("radio","navigation","music","bluetooth","phone","factory").contains(slot))return;
            ComponentName c=ComponentName.unflattenFromString(id);if(c==null)return;
            try{ActivityInfo info=getPackageManager().getActivityInfo(c,0);
                if(info.exported && info.enabled && info.applicationInfo.enabled)prefs.edit().putString("app_"+slot,id).apply();
            }catch(PackageManager.NameNotFoundException ignored){}
        }
        @JavascriptInterface public String assignments(){
            JSONObject result=new JSONObject();
            for(String slot:Arrays.asList("radio","navigation","music","bluetooth","phone","factory")){
                String id=prefs.getString("app_"+slot,"");
                if(id.isEmpty() && slot.equals("radio"))id=prefs.getString("radioPackage","");
                if(id.isEmpty())continue;
                try{ComponentName c=ComponentName.unflattenFromString(id);
                    String label=c==null?getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(id,0)).toString():getPackageManager().getActivityInfo(c,0).loadLabel(getPackageManager()).toString();
                    result.put(slot,new JSONObject().put("id",id).put("name",label));
                }catch(Exception ignored){}
            }return result.toString();
        }
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
