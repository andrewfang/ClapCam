package com.andrewbfang.clapcam;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * To handle drawing
 */
public class DrawView extends ImageView {

    private Path path;
    private Paint paint;
    private int stroke_width;
    private ArrayList<Path> path_history;

    public DrawView(Context context) {
        super(context);
        this.stroke_width = 20;
        this.path = new Path();
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.preparePaint();
        this.path_history = new ArrayList<Path>();
    }

    private void preparePaint(){
        this.paint.setStrokeWidth(this.stroke_width);
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setColor(Color.BLACK);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawPath(this.path, this.paint);
//        for (Path p : this.path_history) {
//            canvas.drawPath(p, this.paint);
//        }
    }

    private void undo(View view) {
        this.path_history.remove(this.path_history.size() -1);
        this.invalidate();
    }

    private float current_x;
    private float current_y;

    private void touch_down(float x, float y) {
//        this.path = new Path();
//        this.path.moveTo(x,y);
//
//        this.current_x = x;
//        this.current_y = y;
    }

    private void touch_move(float x, float y) {
        this.path.lineTo(x, y);
        this.current_x = x;
        this.current_y = y;
    }

    private void touch_up(float x, float y) {
        this.path.lineTo(x, y);
        this.path_history.add(this.path);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motion_event) {
        float x = motion_event.getX();
        float y = motion_event.getY();

        switch (motion_event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.touch_down(x, y);
                this.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                this.touch_move(x, y);
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                this.touch_up(x, y);
                this.invalidate();
                break;
        }
        return true;
    }

    public void set_width(int width) {
        this.stroke_width = width;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.preparePaint();
    }

    public void clear() {
        this.path = new Path();
        this.path_history.clear();
        invalidate();
    }

    /**
     * Returns the stroke width (for number picker default setting)
     */
    public int get_stroke_width() {
        return this.stroke_width;
    }
}
