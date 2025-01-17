package com.dappervision.wearscript;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

public class ScriptView extends WebView implements SurfaceHolder.Callback {
    private static final String TAG = "ScriptView";
    private final BackgroundService context;
    private LiveCard liveCard;
    private SurfaceHolder holder;
    private final Handler handler;
    private long drawFrequency;

    ScriptView(final BackgroundService context) {
        super(context);
        this.context = context;
	clearCache(true);
        setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                String msg = cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId();
                Log.d("WearScriptWebView", msg);
                context.log("WebView: " + msg);
                return true;
            }
        });
        handler = new Handler();
    }

    public void liveCardPublish(boolean nonSilent, long drawFrequency) {
        if (liveCard != null)
            return;
        this.drawFrequency = drawFrequency;
        liveCard = TimelineManager.from(context).getLiveCard("myid");
        Log.d(TAG, "Publishing LiveCard");
        liveCard.enableDirectRendering(true).getSurfaceHolder().addCallback(this);
        liveCard.setNonSilent(nonSilent);

        Intent intent = new Intent(context, MenuActivity.class);
        liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));

        liveCard.publish();
        Log.d(TAG, "Done publishing LiveCard");
    }

    public void liveCardUnpublish() {
        if (liveCard != null) {
            liveCard.getSurfaceHolder().removeCallback(this);
            liveCard.unpublish();
        }
        liveCard = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Nothing to do here.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        this.holder = holder;
        update();
    }

    public void update() {
        if (drawFrequency < 0 || liveCard == null)
            return;
        handler.postDelayed(new Runnable() {
            public void run() {
                if (drawFrequency < 0 || liveCard == null)
                    return;
                draw2();
                update();
            }
        }, drawFrequency);
    }

    public void onDestroy() {
        if (liveCard != null && liveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");
            liveCardUnpublish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        this.holder = null;
    }

    public void draw2() {
        if (liveCard == null)
            return;
        Log.d(TAG, "Drawing");
        Canvas canvas;
        try {
            canvas = holder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
            // Tell the view where to draw.
            View v = context.getActivityView();
            int measuredWidth = View.MeasureSpec.makeMeasureSpec(
                    canvas.getWidth(), View.MeasureSpec.EXACTLY);
            int measuredHeight = View.MeasureSpec.makeMeasureSpec(
                    canvas.getHeight(), View.MeasureSpec.EXACTLY);

            v.measure(measuredWidth, measuredHeight);
            v.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            v.draw(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
