package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TagListEntryView extends TextView {
    boolean mActive = false;
    List<Point> mPointList = new ArrayList<>();
    Paint mActiveFillPaint = new Paint();
    Paint mInactiveFillPaint = new Paint();
    int mActiveDrawColour;
    Paint mActiveDrawPaint = new Paint();
    int mInactiveDrawColour;
    Paint mInactiveDrawPaint = new Paint();

    public TagListEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        String tag = "Love&Tag.TagListEntryView";
        Log.d(tag, "Constructor");
        init();
    }

    private void init() {
        Resources r = getResources();
        int col_ref;
        col_ref = R.color.tag_active_background;
        mActiveFillPaint.setColor(r.getColor(col_ref));
        col_ref = R.color.tag_inactive_background;
        mInactiveFillPaint.setColor(r.getColor(col_ref));
        mActiveDrawColour = r.getColor(R.color.tag_active_line);
        mActiveDrawPaint.setColor(mActiveDrawColour);
        mInactiveDrawColour = r.getColor(R.color.tag_inactive_line);
        mInactiveDrawPaint.setColor(mInactiveDrawColour);
    }

    @Override
    protected void onSizeChanged(int width, int height, int old_w, int old_h) {
        double x_scale = 1;
        double y_scale = 1;
        int x_diff = (int) (((1 - x_scale) / 2) * width);
        int y_diff = (int) (((1 - y_scale) / 2) * height);
        int top = 0 + y_diff;
        int bottom = height - y_diff;
        int left = 0 + x_diff;
        int right = width - x_diff;

        int pointHeight = (int) (width * 0.1);

        mPointList.add(new Point(left, top));
        mPointList.add(new Point(right - pointHeight, top));
        mPointList.add(new Point(right, top + height / 2));
        mPointList.add(new Point(right - pointHeight, bottom));
        mPointList.add(new Point(left, bottom));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas) {
        String tag = "Love&Tag.TagListEntryView.drawBackground";
        Log.d(tag, "Starting");

        Path path = new Path();

        Point p;
        p = mPointList.get(0);
        path.moveTo(p.x, p.y);
        for (int index=1; index<mPointList.size(); index++) {
            p = mPointList.get(index);
            path.lineTo(p.x, p.y);
        }
        if (mActive) {
            canvas.drawPath(path, mActiveFillPaint);
            this.setTextColor(mActiveDrawColour);
        } else {
            canvas.drawPath(path, mInactiveFillPaint);
            this.setTextColor(mInactiveDrawColour);
        }
    }
}
