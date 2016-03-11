package altitech.chucknorriswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppWidgetProvider {

    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;

    String quote = "";

    private class myURLTask extends AsyncTask<String, Void, String>{

        public String connect(String urlString){

            URL url;
            HttpURLConnection conn;
            InputStream is;
            BufferedReader reader;
            String data;

            try {
                url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                is = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String result = "";

                while ((data = reader.readLine()) != null){
                    result += data + "\n";
                }

                conn.disconnect();

                quote = new JSONObject(result).getJSONObject("value").getString("joke");

            }catch (MalformedURLException e){
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }catch (JSONException e) {
                e.printStackTrace();
            }

            //fix broken quotationmarks and then return quote
            quote = quote.replaceAll("&quot;", "\"");
            quote = quote.replaceAll("&amp;", "&");
            return quote;
        }

        @Override
        protected String doInBackground(String... params) {

            for (int widgetId : getAllIds()) {

                RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_layout);

                remoteViews.setTextViewText(R.id.update,
                        String.valueOf("loading..."));

                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }


            String url = "http://api.icndb.com/jokes/random";

            // take only jokes not longer 157 length
            //to fit in the box
            while (connect(url).length() > 157) {
                //fetch another one
            }

            return null;
        }


        @Override
        protected void onPostExecute(String result) {
            setWidgetText(quote);
        }
    }

    public int[] getAllIds(){
        ComponentName thisWidget = new ComponentName(context, MainActivity.class);
        return appWidgetManager.getAppWidgetIds(thisWidget);
    }

    public void setWidgetText(String text){

        for (int widgetId : getAllIds()) {

            RemoteViews remoteViews;
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Set the text
            remoteViews.setTextViewText(R.id.update, String.valueOf(text));

            // Set onClickListener
            Intent intent = new Intent(context, MainActivity.class);

            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetIds = appWidgetIds;

        //check for connectivity, then start fetching quotes
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()){
            myURLTask task = new myURLTask();
            task.execute();
        }else{
            setWidgetText("No internet connection!");
        }
    }
}