package home.jarvis;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

final class Chat {
    private static final String ALIAS = "JarvisGemini";
    private final JSONArray history = new JSONArray();

    static synchronized Cipher cipher(int mode, byte[] iv) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, 3)
                .setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            generator.generateKey();
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        if (iv == null) cipher.init(mode, store.getKey(ALIAS, null));
        else cipher.init(mode, store.getKey(ALIAS, null), new GCMParameterSpec(128, iv));
        return cipher;
    }

    static void save(Context ctx, String key) throws Exception {
        Cipher c = cipher(Cipher.ENCRYPT_MODE, null);
        String record = Base64.encodeToString(c.getIV(), 2) + "." +
            Base64.encodeToString(c.doFinal(key.getBytes("UTF-8")), 2);
        ctx.getSharedPreferences("jarvis", 0).edit().putString("credential", record).apply();
    }

    static JSONObject turn(String role, String text) throws JSONException {
        return new JSONObject().put("role", role).put("parts",
            new JSONArray().put(new JSONObject().put("text", text)));
    }

    String ask(Context ctx, String question) throws Exception {
        String record = ctx.getSharedPreferences("jarvis", 0).getString("credential", "");
        if (record.isEmpty()) return "Open Jarvis and tap Set Gemini key first, sir.";
        String[] pair = record.split("[.]");
        String key = new String(cipher(Cipher.DECRYPT_MODE, Base64.decode(pair[0], 2))
            .doFinal(Base64.decode(pair[1], 2)), "UTF-8");
        if (question.contains(key)) return "That contains your key. Nothing sent.";
        JSONArray contents = new JSONArray(history.toString()).put(turn("user", question));
        JSONObject payload = new JSONObject().put("contents", contents)
            .put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject()
            .put("text","You are Jarvis, Bhavyendra's assistant. Address him as sir. Reply briefly for speech in English or Telugu as requested. Keep the previous reply language for follow-ups. You can discuss and draft but cannot perform external actions."))));
        HttpsURLConnection conn = (HttpsURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent").openConnection();
        try {
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000); conn.setReadTimeout(30000);
            conn.setRequestMethod("POST"); conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.setRequestProperty("x-goog-api-key",key);
            try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes("UTF-8"));}
            int status=conn.getResponseCode();
            if(status!=200)return "Gemini request failed with HTTP "+status+".";
            StringBuilder body=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))){
                String line; while((line=reader.readLine())!=null)body.append(line);
            }
            JSONArray parts=new JSONObject(body.toString()).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts");
            StringBuilder answer=new StringBuilder();
            for(int i=0;i<parts.length();i++){
                JSONObject part=parts.getJSONObject(i);
                if(!part.optBoolean("thought")) answer.append(part.optString("text"));
            }
            String text=answer.toString().trim().replace(key,"[redacted]");
            if(text.isEmpty())return "No answer came through, sir.";
            history.put(turn("user",question)).put(turn("model",text));
            while(history.length()>12)history.remove(0);
            return text;
        } finally {conn.disconnect();}
    }
}
