package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TagListEntryView extends TextView {

    public TagListEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        String tag = "Love&Tag.TagListEntryView";
        Log.d(tag, "Constructor");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas) {
        String tag = "Love&Tag.TagListEntryView.drawBackground";
        Log.d(tag, "Starting");
        double xScale = 0.95;
        double yScale = 0.9;

        int height = getHeight();
        int width = getWidth();
        int top = 0;
        int bottom = height;
        int left = 0;
        int right = width;

        int pointHeight = (int) (width * 0.1);

        Point a = new Point(left, top);
        Point b = new Point(right - pointHeight, top);
        Point c = new Point(right, top + height / 2);
        Point d = new Point(right - pointHeight, bottom);
        Point e = new Point(left, bottom);

        Path path = new Path();
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(d.x, d.y);
        path.lineTo(e.x, e.y);

        Paint fill_paint = new Paint();
        Paint line_paint = new Paint();
        int fill_ref;
        int line_ref;
        if (true) {
            fill_ref = R.color.tag_active_background;
            line_ref = R.color.tag_active_line;
        } else {
            fill_ref = R.color.tag_inactive_background;
            line_ref = R.color.tag_inactive_line;
        }
        Resources R = getResources();
        fill_paint.setColor(R.getColor(fill_ref));
        line_paint.setColor(R.getColor(line_ref));
        canvas.drawPath(path, fill_paint);
    }
}
