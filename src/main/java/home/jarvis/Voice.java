package home.jarvis;
import android.app.*;
import android.os.*;
import android.content.*;
import android.speech.*;
import android.speech.tts.*;
import java.util.*;
public class Voice extends Service implements RecognitionListener {
 static volatile String state="Stopped";
 SpeechRecognizer mic; TextToSpeech voice; boolean awake,closed; String failure;
 Handler main=new Handler(Looper.getMainLooper());
 public IBinder onBind(Intent i){return null;}
 public int onStartCommand(Intent i,int f,int id){
 if(i!=null&&"stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
 NotificationManager manager=getSystemService(NotificationManager.class);
 manager.createNotificationChannel(new NotificationChannel("voice","Jarvis microphone",NotificationManager.IMPORTANCE_LOW));
 PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,Voice.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE);
 startForeground(1,new Notification.Builder(this,"voice").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Jarvis microphone is on").addAction(android.R.drawable.ic_delete,"Stop",stop).setOngoing(true).build());
 if(voice!=null)return START_NOT_STICKY;
 if(!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){fail("On-device speech recognition is unavailable on this phone.");return START_NOT_STICKY;}
 mic=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);mic.setRecognitionListener(this);
 voice=new TextToSpeech(this,s->{if(closed)return;if(s!=TextToSpeech.SUCCESS){fail("Speech output unavailable");return;}voice.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){main.post(()->listen());}public void onError(String id){main.post(()->fail("Speech output failed"));}});listen();});return START_NOT_STICKY;}
 void fail(String message){failure=message;stopSelf();}
 void listen(){if(closed)return;state=awake?"Listening. Ask for the time, or say go to sleep.":"Standby. Say wake up Jarvis.";
 mic.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-GB"));}
 public void onResults(Bundle b){ArrayList<String> words=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);String q=words==null||words.isEmpty()?"":words.get(0).toLowerCase(Locale.ROOT).replaceAll("[.!?,]","").trim();
 String reply;if(!awake){if(!q.equals("wake up jarvis")){main.postDelayed(this::listen,350);return;}awake=true;reply="Ready, sir.";}
 else if(q.equals("go to sleep")){awake=false;reply="Standing by.";}else if(q.contains("time"))reply="It is "+android.text.format.DateFormat.getTimeFormat(this).format(new Date());else reply="I heard "+q;
 state=reply;if(voice.speak(reply,TextToSpeech.QUEUE_FLUSH,null,"reply")==TextToSpeech.ERROR)fail("Speech output unavailable");}
 public void onError(int e){if(e==6||e==7){main.postDelayed(this::listen,500);}else fail("Recognition stopped (code "+e+"). Check installed English speech data.");}
 public void onDestroy(){closed=true;main.removeCallbacksAndMessages(null);if(mic!=null)mic.destroy();if(voice!=null)voice.shutdown();state=failure==null?"Stopped":failure;super.onDestroy();}
 public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){} public void onPartialResults(Bundle b){} public void onEvent(int e,Bundle b){}
}
