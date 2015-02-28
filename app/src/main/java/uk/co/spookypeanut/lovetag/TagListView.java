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
        boolean returnValue = super.drawChild(canvas, child, drawingTime);

        int width = child.getWidth();
        int height = child.getHeight();
        int top = child.getTop();
        int bottom = child.getBottom();
        int pointHeight = (int) (height * 0.75);

        Point a = new Point(0, top);
        Point b = new Point(width, top);
        Point c = new Point(width, top + height - pointHeight);
        //mPointedHeight is the length of the triangle... in this case we have it dynamic and can be changed.
        Point d = new Point((width/2)+(pointHeight/2),
                top + height - pointHeight);
        Point e = new Point((width/2), top + height);// this is the sharp
        // point of the triangle
        Point f = new Point((width/2)-(pointHeight/2),
                top + height - pointHeight);
        Point g = new Point(0, top + height - pointHeight);

        Path path = new Path();
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(d.x, d.y);
        path.lineTo(e.x, e.y);
        path.lineTo(f.x, f.y);
        path.lineTo(g.x, g.y);

        Paint line_colour = new Paint();
        line_colour.setColor(Color.GREEN);
        canvas.drawPath(path, line_colour);
        return returnValue;
    }
}
