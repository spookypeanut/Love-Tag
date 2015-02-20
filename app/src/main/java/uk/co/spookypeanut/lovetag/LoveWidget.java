package uk.co.spookypeanut.lovetag;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;

public class LoveWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        context.startService(new Intent(context, UpdateService.class));
        /*
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }*/
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class UpdateService extends IntentService {
        private SharedPreferences mPrefs;
        public UpdateService() {
            super("LoveWidget$UpdateService");
        }
        @Override
        public void onCreate() {
            super.onCreate();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, LoveWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            mgr.updateAppWidget(me, buildUpdate(this));
        }
        private RemoteViews buildUpdate(Context context) {
            CharSequence widgetText = context.getString(R.string.appwidget_text);
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.love_widget);
            views.setTextViewText(R.id.loveWidgetLabel, widgetText);
            Intent i = new Intent(this, LoveWidget.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
            views.setOnClickPendingIntent(R.id.loveWidgetButton, pi);

            return views;
        }
    }
}


