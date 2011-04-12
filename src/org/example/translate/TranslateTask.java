package org.example.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class TranslateTask implements Runnable{
	private static final String TAG = "TranslateTask";
	private static final String GOOGLE_TRANSLATE_URL = "https://ajax.googleapis.com/ajax/services/language/translate";
	private final Translate translate;
	private final String original, from, to;
	

	public TranslateTask(Translate translate, String original, String from, String to) {
		Log.d(TAG,"TrTask created");
		this.translate = translate;
		this.original = original;
		this.from = from;
		this.to = to;
	}
	
	@Override
	public void run() {
		Log.d(TAG,"TrTask run");
		String trans = doTranslate(original, from, to);
		translate.setTranslated(trans);
		
		String retrans = doTranslate(trans, to, from);
		translate.setRetranslated(retrans);
		
	}
	
	private String doTranslate(String original, String from, String to){
		String result = translate.getResources().getString(R.string.translation_error);
		Log.d(TAG,result);
		HttpURLConnection con = null;
		Log.d(TAG, "doTranslate");
		
		try{
			if (Thread.interrupted())
				throw new InterruptedException();
			
			//RESTful
			String q = URLEncoder.encode(original, "UTF-8");
			URL url = new URL(
					GOOGLE_TRANSLATE_URL + "?v=1.0&q=" + q + "&langpair=%7C" + to);
			Log.d(TAG, "Called API: "+url.toString());
			con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(10000);
			con.setConnectTimeout(15000);
			con.setRequestMethod("GET");
			con.setDoInput(true);
			
			con.connect();
			
			if (Thread.interrupted())
				throw new InterruptedException();
			
			
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(con.getInputStream(), "UTF-8"));
			String payload = reader.readLine();
			reader.close();
			
			JSONObject jsonObject = new JSONObject(payload);
			result = jsonObject.getJSONObject("responseData")
			.getString("detectedSourceLanguage")
			.replace("&#39;", "'")
			.replace("&amp;", "&");
			result += " :";
			result += jsonObject.getJSONObject("responseData")
				.getString("translatedText")
				.replace("&#39;", "'")
				.replace("&amp;", "&");
			
			if (Thread.interrupted())
				throw new InterruptedException();
			
		} catch (IOException e){
			Log.e(TAG, "IOException", e);
		} catch (JSONException e){
			Log.e(TAG, "JSONException", e);
		} catch (InterruptedException e){
			Log.e(TAG,"Interrupted Exception", e);
			result = translate.getResources().getString(R.string.translation_interrupted);
		} finally {
			if (con != null){
				con.disconnect();
			}
		}
		
		Log.d(TAG, " -> returned " + result);
		return result;
	}
	
}
