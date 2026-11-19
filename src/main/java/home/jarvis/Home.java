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
 Button settings=new Button(this);settings.setText("Set Gemini key");page.addView(settings);
 settings.setOnClickListener(v->{
 EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(129);input.setSaveEnabled(false);
 new AlertDialog.Builder(this).setTitle("Gemini key").setMessage("After wake-up, questions and recent replies go to Gemini. Standby stays on device.")
 .setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(dialog,which)->{
 String key=input.getText().toString().trim();input.setText("");
 if(key.isEmpty())return;
 try{Chat.save(this,key);Toast.makeText(this,"Key saved",0).show();}
 catch(Exception error){Toast.makeText(this,"Could not save key",0).show();}
 }).show();});
 Button enable=new Button(this);enable.setText("Enable screen lock");page.addView(enable);
 enable.setOnClickListener(v->{
 android.app.admin.DevicePolicyManager policy=getSystemService(android.app.admin.DevicePolicyManager.class);
 ComponentName admin=new ComponentName(this,LockAdmin.class);
 if(policy.isAdminActive(admin)){Toast.makeText(this,"Screen lock enabled",0).show();return;}
 try{startActivity(new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
 .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,admin)
 .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Allow Jarvis to lock the screen when you ask."));}
 catch(ActivityNotFoundException ex){Toast.makeText(this,"Screen lock permission unavailable",0).show();}
 });
 Button start=new Button(this);start.setText("Start listening");page.addView(start);start.setOnClickListener(v->start());
 Button stop=new Button(this);stop.setText("Stop");page.addView(stop);stop.setOnClickListener(v->stopService(new Intent(this,Voice.class)));}
 void start(){if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=0){requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.POST_NOTIFICATIONS}:new String[]{android.Manifest.permission.RECORD_AUDIO},1);return;}startForegroundService(new Intent(this,Voice.class));}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==1&&g.length>0&&g[0]==0)start();}
 protected void onResume(){super.onResume();timer.post(refresh);}protected void onPause(){timer.removeCallbacks(refresh);super.onPause();}
}
