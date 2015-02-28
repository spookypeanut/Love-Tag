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
        int top = child.getTop();
        int bottom = child.getBottom();
        int height = bottom - top;
        int width = child.getWidth();
        int pointHeight = (int) (width * 0.1);

        Point a = new Point(0, top);
        Point b = new Point(width - pointHeight, top);
        Point c = new Point(width, top + height / 2);
        Point d = new Point(width - pointHeight, bottom);
        Point e = new Point(0, bottom);

        Path path = new Path();
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(d.x, d.y);
        path.lineTo(e.x, e.y);

        Paint line_colour = new Paint();
        line_colour.setColor(Color.GREEN);
        canvas.drawPath(path, line_colour);
        boolean returnValue = super.drawChild(canvas, child, drawingTime);
        return returnValue;
    }
}
