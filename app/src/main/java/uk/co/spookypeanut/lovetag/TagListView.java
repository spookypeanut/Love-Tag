package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class TagListView extends ListView {
    boolean mActive = false;

    public TagListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        double xscale = 0.95;
        double yscale = 0.9;

        int vmidy = (child.getBottom() + child.getTop()) / 2;
        int vheight = child.getBottom() - child.getTop();
        int vmidx = (child.getRight() + child.getLeft()) / 2;
        int vwidth = child.getRight() - child.getLeft();

        int height = (int) (vheight * yscale);
        int width = (int) (vwidth * xscale);
        int top = vmidy - height / 2;
        int bottom = vmidy + height / 2;
        int left = vmidx - width / 2;
        int right = vmidx + width / 2;

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
        if (mActive) {
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
        return super.drawChild(canvas, child, drawingTime);
    }
}
