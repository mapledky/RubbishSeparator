package com.maple.smartcan.view.AnimationButton;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;


public class DefaultStatus implements ButtonStatus {
    //分别表示处于默认状态内部的四个子状态
    private static final int Status_Default = 0;
    private static final int Status_Touch = 1;
    private static final int Status_Up = 2;
    private static final int Status_Next = 3;
    //刷新width时的渐变量以及时间间距
    private static final int Delay_Next = 500;
    private static final int Delay_Frush = 10;
    private static final int Pixel_Frush = 8;
    //按钮对象
    private AnimationButton button;
    //按钮对象的长宽与中点坐标（长宽为绘制的长宽，而不是控件的长宽）
    private int width;
    private int height;
    private int centerX;
    private int centerY;
    //子状态变量
    private int status = Status_Default;
    private Handler handler = new Handler();

    public DefaultStatus(AnimationButton button, int width, int height) {
        this.button = button;
        this.width = width;
        this.height = height;
        this.centerX = width / 2;
        this.centerY = height / 2;
    }

    @Override
    public Status getStatus() {
        return Status.Default;
    }

    @Override
    public boolean onTouchEvent(MotionEvent mEvent) {
        switch (mEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //按下时，切换到按下子状态
                if(status == Status_Default){
                    status = Status_Touch;
                    button.invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //抬起时，或者移除控件时，切换到抬起子状态
                if(status == Status_Touch){
                    status = Status_Up;
                    button.invalidate();
                    //过500ms延迟后开始进行伸缩变化
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //切换到next子状态
                            if(status == Status_Up){
                                status = Status_Next;
                            }
                            if(status == Status_Next){
                                //若长宽不一致，则继续渐变，否则改变状态
                                if (width >= height) {
                                    width -= Pixel_Frush;
                                    button.invalidate();
                                    handler.postDelayed(this, Delay_Frush);
                                }else{
                                    button.changeStatus(Status.Progress, width, height, centerX, centerY);
                                }
                            }
                        }
                    }, Delay_Next);
                    //响应监听器
                    AnimationButton.OnAnimationButtonClickListener listener = button.getOnAnimationButtonClickListener();
                    if(listener != null){
                        listener.onClick();
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onDraw(Canvas mCanvas, Paint mPaint) {
        switch (status) {
            case Status_Default:
                onDrawDefault(mCanvas, mPaint);
                break;
            case Status_Touch:
                onDrawTouch(mCanvas, mPaint);
                break;
            case Status_Up:
                onDrawUp(mCanvas, mPaint);
                break;
            case Status_Next:
                onDrawNext(mCanvas, mPaint);
                break;
        }
    }

    /**
     * 绘制边框，分为空心和实心两种
     *
     * @param mCanvas 画布
     * @param mPaint  画笔
     * @param style   空心或者实心
     * @param padding 边框补白
     */
    private void drawRound(Canvas mCanvas, Paint mPaint, Paint.Style style, int padding) {
        mPaint.setColor(button.getColorBase());
        int stroke = padding;
        if (style == Paint.Style.STROKE) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(button.getStroke());
            stroke += button.getStroke() / 2;
        } else {
            mPaint.setStyle(Paint.Style.FILL);
        }
        //绘制边框
        mCanvas.drawRoundRect(new RectF(stroke, stroke, width - stroke, height - stroke), button.getRound(), button.getRound(), mPaint);
    }

    /**
     * 画文字，有字体大小和颜色的区别
     *
     * @param mCanvas   画布
     * @param mPaint    画笔
     * @param textSize  字体大小
     * @param textColor 字体颜色
     */
    private void drawText(Canvas mCanvas, Paint mPaint, int textSize, int textColor) {
        mPaint.setColor(textColor);
        mPaint.setStrokeWidth(button.getStrokeText());
        mPaint.setTextSize(textSize);
        Paint.FontMetrics metrics = mPaint.getFontMetrics();
        int textWidth = (int) mPaint.measureText(button.getText());
        int baseLine = (int) (height / 2 + (metrics.bottom - metrics.top) / 2 - metrics.bottom);
        mCanvas.drawText(button.getText(), (width - textWidth) / 2, baseLine, mPaint);
    }

    /**
     * 绘制默认状态的按钮
     *
     * @param mCanvas
     * @param mPaint
     */
    private void onDrawDefault(Canvas mCanvas, Paint mPaint) {
        drawRound(mCanvas, mPaint, Paint.Style.STROKE, 0);
        //绘制居中文字
        drawText(mCanvas, mPaint, button.getTextSize(), button.getColorBase());
    }

    /**
     * 绘制按下状态的按钮
     *
     * @param mCanvas
     * @param mPaint
     */
    private void onDrawTouch(Canvas mCanvas, Paint mPaint) {
        drawRound(mCanvas, mPaint, Paint.Style.FILL, button.getStroke());
        //绘制文字，字体要变化
        drawText(mCanvas, mPaint, button.getTextSizeTouch(), Color.WHITE);
    }

    /**
     * 绘制抬起状态的按钮
     *
     * @param mCanvas
     * @param mPaint
     */
    private void onDrawUp(Canvas mCanvas, Paint mPaint) {
        drawRound(mCanvas, mPaint, Paint.Style.FILL, 0);
        drawText(mCanvas, mPaint, button.getTextSize(), Color.WHITE);
    }

    /**
     * 绘制进入下一状态的按钮
     *
     * @param mCanvas
     * @param mPaint
     */
    private void onDrawNext(Canvas mCanvas, Paint mPaint) {
        mPaint.setColor(button.getColorBase());
        mPaint.setStyle(Paint.Style.FILL);
        //绘制边框
        if (width >= height) {
            mCanvas.drawRoundRect(new RectF(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2),
                    button.getRound(), button.getRound(), mPaint);
            //绘制文字
            mPaint.setColor(Color.WHITE);
            mPaint.setStrokeWidth(button.getStrokeText());
            mPaint.setTextSize(button.getTextSize());
            Paint.FontMetrics metrics = mPaint.getFontMetrics();
            int textWidth = (int) mPaint.measureText(button.getText());
            int baseLine = (int) (centerY + (metrics.bottom - metrics.top) / 2 - metrics.bottom);
            mCanvas.drawText(button.getText(), centerX - textWidth / 2, baseLine, mPaint);
        } else {
            mCanvas.drawOval(new RectF(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2), mPaint);
        }
    }
}