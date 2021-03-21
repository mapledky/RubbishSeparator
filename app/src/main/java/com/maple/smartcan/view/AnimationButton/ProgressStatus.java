package com.maple.smartcan.view.AnimationButton;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;

public class ProgressStatus implements ButtonStatus {
    //转圈的子状态
    private static final int Status_Once = 0;
    private static final int Status_Twice = 1;
    //转圈的渐变量
    private static final int Delay_Progress = 10;
    private static final int Angle_Progress = 5;
    private static final int Angle_Default = -90;
    private static final int Andle_Full = 270;

    private AnimationButton button;
    private int width;
    private int height;
    private int centerX;
    private int centerY;
    private int radius;
    private int status = Status_Once;
    //当前的进度
    private float progress = Angle_Default;
    private Handler handler = new Handler();

    public ProgressStatus(AnimationButton button, int width, int height, int centerX, int centerY) {
        this.button = button;
        this.width = width;
        this.height = height;
        this.centerX = centerX;
        this.centerY = centerY;
        //绘制起点是Stroke的中点，若不减去这个值，则onDraw时，会不完整。
        this.radius = (width - button.getStrokeProgress())  / 2;

        startProgress();
    }

    /**
     * 开始递归转动进度条
     */
    private void startProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(progress >= Andle_Full){
                    //如果是手动结束模式
                    if(button.getMode() == AnimationButton.Mode.Hand_Finish && button.isProgressStop()){
                        //改变状态
                        button.changeStatus(Status.Finish, width, height, centerX, centerY);
                        return;
                    }else{
                        if(status == Status_Once){
                            status = Status_Twice;
                        }else if(status == Status_Twice){
                            //如果是自动结束模式，则在第二次进度结束时改变状态
                            if(button.getMode() == AnimationButton.Mode.Auto_Finish){
                                //改变状态
                                button.changeStatus(Status.Finish, width, height, centerX, centerY);
                                return;
                            }else{
                                status = Status_Once;
                            }
                        }
                        //重置进度
                        progress = Angle_Default;
                    }
                }
                progress += Angle_Progress;
                button.invalidate();
                handler.postDelayed(this, Delay_Progress);
            }
        }, Delay_Progress);
    }

    @Override
    public Status getStatus() {
        return Status.Progress;
    }

    @Override
    public boolean onTouchEvent(MotionEvent mEvent) {
        return false;
    }

    @Override
    public void onDraw(Canvas mCanvas, Paint mPaint) {
        if(status == Status_Once){
            //绘制灰色背景
            onDrawCircle(mCanvas, mPaint, button.getColorBack());
            //绘制绿色进度
            onDrawArc(mCanvas, mPaint, button.getColorBase(), Angle_Default, progress);
        }else if(status == Status_Twice){
            //绘制绿色背景
            onDrawCircle(mCanvas, mPaint, button.getColorBase());
            //绘制灰色进度
            onDrawArc(mCanvas, mPaint, button.getColorBack(), Angle_Default, progress);
        }
    }

    /**
     * 画一整个圆作为背景
     * @param mCanvas 画布
     * @param mPaint 画笔
     * @param color 颜色
     */
    private void onDrawCircle(Canvas mCanvas, Paint mPaint, int color){
        mPaint.setColor(color);
        mPaint.setStrokeWidth(button.getStrokeProgress());
        mPaint.setStyle(Paint.Style.STROKE);
        mCanvas.drawCircle(centerX, centerY, radius, mPaint);
    }

    /**
     * 画一端圆弧
     * @param mCanvas 画布
     * @param mPaint 画笔
     * @param color 颜色
     * @param start 开始角度
     * @param stop 结束角度
     */
    private void onDrawArc(Canvas mCanvas, Paint mPaint, int color, float start, float stop){
        mPaint.setColor(color);
        mPaint.setStrokeWidth(button.getStrokeProgress());
        mPaint.setStyle(Paint.Style.STROKE);
        //第三个参数是扫过的角度，起点0默认为右边
        mCanvas.drawArc(new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                start, stop - start, false, mPaint);
    }
}