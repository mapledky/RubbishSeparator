package com.maple.smartcan.view.AnimationButton;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.maple.smartcan.R;


public class AnimationButton extends View {

    private static int Color_Base = Color.rgb(24, 204, 149);
    private static int Color_Back = Color.rgb(153, 153, 153);
    private static int Stroke = 3;
    private static int Stroke_Text = 0;
    private static int Stroke_Progress = 10;
    private static int Text_Size = 30;
    private static int Text_Size_Touch = 25;
    private static int Round = 30;
    private static String Text = "";

    private Mode mode = Mode.Auto_Finish;
    private Context context;
    private int maxWidth;
    private int maxHeight;
    private int colorBase = Color_Base;
    private int colorBack = Color_Back;
    private int stroke = Stroke;
    private int strokeText = Stroke_Text;
    private int strokeProgress = Stroke_Progress;
    private int textSize = Text_Size;
    private int textSizeTouch = Text_Size_Touch;
    private int round = Round;
    private String text = Text;
    //是否停止进度条，由外界设置
    private boolean isProgressStop = false;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ButtonStatus status;
    private OnAnimationButtonClickListener listener;

    public Mode getMode() {
        return mode;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getTextSizeTouch() {
        return textSizeTouch;
    }

    public void setTextSizeTouch(int textSizeTouch) {
        this.textSizeTouch = textSizeTouch;
    }

    public int getStrokeProgress() {
        return strokeProgress;
    }

    public void setStrokeProgress(int strokeProgress) {
        this.strokeProgress = strokeProgress;
    }

    public int getColorBase() {
        return colorBase;
    }

    public void setColorBase(int colorBase) {
        this.colorBase = colorBase;
    }

    public int getColorBack() {
        return colorBack;
    }

    public void setColorBack(int colorBack) {
        this.colorBack = colorBack;
    }

    public int getStroke() {
        return stroke;
    }

    public void setStroke(int stroke) {
        this.stroke = stroke;
    }

    public int getStrokeText() {
        return strokeText;
    }

    public void setStrokeText(int strokeText) {
        this.strokeText = strokeText;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public AnimationButton(Context context) {
        super(context);
    }

    public AnimationButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimationButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (status != null&&listener.check()) {
            return status.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (status != null) {
            status.onDraw(canvas, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (maxWidth != 0 && maxHeight != 0) {
            status = new DefaultStatus(this, maxWidth, maxHeight);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 改变整体状态
     *
     * @param s      改变的状态
     * @param width  目前的宽度
     * @param height 目前的高度
     */
    public void changeStatus(Status s, int width, int height, int centerX, int centerY) {
        switch (s) {
            case Default:
                break;
            case Progress:
                //改变状态，进入进度条状态
                status = new ProgressStatus(this, width, height, centerX, centerY);
                invalidate();
                break;
            case Finish:
                //进入结束状态
                status = new FinishStatus(this, width, height, centerX, centerY);
                invalidate();
                break;
        }
    }

    /**
     * 外界设置停止进度条
     */
    public void stopProgress(){
        this.isProgressStop = true;
    }

    /**
     * 检查是否进度条结束
     * @return
     */
    public boolean isProgressStop(){
        return isProgressStop;
    }

    public enum Mode{
        Auto_Finish,
        Hand_Finish
    }

    public interface OnAnimationButtonClickListener{
        void onClick();
        void finish();
        boolean check();
    }

    public void setOnAnimationButtonClickListener(OnAnimationButtonClickListener listener){
        this.listener = listener;
    }

    public OnAnimationButtonClickListener getOnAnimationButtonClickListener(){
        return listener;
    }
}