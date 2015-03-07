package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TagListEntryView extends TextView {
    boolean mActive = false;
    List<Point> mBorderPointList = new ArrayList<>();
    Point mHoleCentre = new Point();
    int mHoleRadius;
    Paint mActiveFillPaint = new Paint();
    Paint mInactiveFillPaint = new Paint();
    int mActiveDrawColour;
    Paint mActiveDrawPaint = new Paint();
    int mInactiveDrawColour;
    Paint mInactiveDrawPaint = new Paint();

    public TagListEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        int left = 0;
        int top = 0;
        int bottom = height - top;
        int right = width - left;

        int pointHeight = (int) (width * 0.1);

        int hole_centre_x = right - pointHeight;
        int hole_centre_y = height / 2;
        mHoleCentre = new Point(hole_centre_x, hole_centre_y);
        // Can't do this based on height, because then there are bigger holes
        // for multi-line tags
        mHoleRadius = width / 50;

        mBorderPointList.add(new Point(left, top));
        mBorderPointList.add(new Point(right - pointHeight, top));
        mBorderPointList.add(new Point(right, top + height / 2));
        mBorderPointList.add(new Point(right - pointHeight, bottom));
        mBorderPointList.add(new Point(left, bottom));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawBackground(canvas);
        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        Point p;
        p = mBorderPointList.get(0);
        path.moveTo(p.x, p.y);
        for (int index=1; index< mBorderPointList.size(); index++) {
            p = mBorderPointList.get(index);
            path.lineTo(p.x, p.y);
        }
        path.moveTo(mHoleCentre.x, mHoleCentre.y);
        path.addCircle(mHoleCentre.x, mHoleCentre.y,
                       mHoleRadius, Path.Direction.CW);
        if (mActive) {
            canvas.drawPath(path, mActiveFillPaint);
            this.setTextColor(mActiveDrawColour);
        } else {
            canvas.drawPath(path, mInactiveFillPaint);
            this.setTextColor(mInactiveDrawColour);
        }
    }
}
