package home.jarvis;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
public class Home extends Activity {
 TextView status;
 final Handler timer=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){status.setText(Voice.state);timer.postDelayed(this,500);}};
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(48,120,48,40);setContentView(page);
 TextView title=new TextView(this);title.setText("Jarvis");title.setTextSize(36);page.addView(title);
 status=new TextView(this);page.addView(status);
 Button start=new Button(this);start.setText("Start listening");page.addView(start);start.setOnClickListener(v->start());
 Button stop=new Button(this);stop.setText("Stop");page.addView(stop);stop.setOnClickListener(v->stopService(new Intent(this,Voice.class)));}
 void start(){if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=0){requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.POST_NOTIFICATIONS}:new String[]{android.Manifest.permission.RECORD_AUDIO},1);return;}startForegroundService(new Intent(this,Voice.class));}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==1&&g.length>0&&g[0]==0)start();}
 protected void onResume(){super.onResume();timer.post(refresh);}protected void onPause(){timer.removeCallbacks(refresh);super.onPause();}
}
