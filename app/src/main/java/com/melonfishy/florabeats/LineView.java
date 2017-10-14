package com.melonfishy.florabeats;
/**
 * Created by bakafish on 8/27/17.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class LineView extends View {
    Paint paint = new Paint();

    private int initx, inity, finalx, finaly, initID, finalID;

    private void init(@ColorRes int color) {
        paint.setColor(ContextCompat.getColor(getContext(), color));
        paint.setStrokeWidth(10.0f);
    }

    public LineView(Context context, int x0, int y0, int x1, int y1, int id0, int id1,
                    @ColorRes int color) {
        super(context);
        initx = x0;
        inity = y0;
        finalx = x1;
        finaly = y1;
        initID = id0;
        finalID = id1;
        init(color);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawLine(initx, inity, finalx, finaly, paint);
    }

    public int getInitID() {
        return initID;
    }

    public int getFinalID() {
        return finalID;
    }
}
