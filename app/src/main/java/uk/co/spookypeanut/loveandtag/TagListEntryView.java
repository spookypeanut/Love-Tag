package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TagListEntryView extends TextView {
    final int mDropShadowRadius = 2;
    final int mDropShadowDistance = 4;
    boolean mActive = false;
    List<Point> mBorderPointList = new ArrayList<>();
    Point mHoleCentre = new Point();
    int mHoleRadius;
    Paint mActiveFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mInactiveFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int mActiveDrawColour;
    Paint mActiveDrawPaint = new Paint();
    int mInactiveDrawColour;
    Paint mInactiveDrawPaint = new Paint();

    public TagListEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void drawBackground(Canvas canvas) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        Point p = mBorderPointList.get(0);
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

    private void init() {
        Resources r = getResources();
        mActiveFillPaint.setColor(r.getColor(R.color.lastfm_red ));
        mActiveFillPaint.setShadowLayer(mDropShadowRadius, 0,
                mDropShadowDistance, r.getColor(R.color.shadow_grey));
        setLayerType(LAYER_TYPE_SOFTWARE, mActiveFillPaint);
        mInactiveFillPaint.setColor(r.getColor(R.color.pale_lastfm_red));
        mInactiveFillPaint.setShadowLayer(mDropShadowRadius, 0,
                mDropShadowDistance, r.getColor(R.color.shadow_grey));
        setLayerType(LAYER_TYPE_SOFTWARE, mActiveFillPaint);
        mActiveDrawColour = Color.WHITE;
        mActiveDrawPaint.setColor(mActiveDrawColour);
        mInactiveDrawColour = Color.WHITE;
        mInactiveDrawPaint.setColor(mInactiveDrawColour);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawBackground(canvas);
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int width, int height, int old_w, int old_h) {
        // This formula gives us an offset of 4 on a tiny screen,
        // and 10 on a Nexus 5
        final int offset = 2 + (int) (1.0 * height / 18);
        int left = offset;
        int top = offset;
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
        mBorderPointList.add(new Point(hole_centre_x, top));
        mBorderPointList.add(new Point(right, hole_centre_y));
        mBorderPointList.add(new Point(hole_centre_x, bottom));
        mBorderPointList.add(new Point(left, bottom));
    }
}
