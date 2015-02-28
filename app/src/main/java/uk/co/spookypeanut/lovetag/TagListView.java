package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class TagListView extends ListView {
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

        Paint line_colour = new Paint();
        line_colour.setColor(getResources().getColor(R.color
                .tag_inactive_background));
        canvas.drawPath(path, line_colour);
        return super.drawChild(canvas, child, drawingTime);
    }
}
