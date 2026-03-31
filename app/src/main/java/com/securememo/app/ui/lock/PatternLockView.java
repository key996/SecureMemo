package com.securememo.app.ui.lock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 3x3 图案密码绘制 View
 *
 * 功能：
 *   - 绘制 9 个圆点（3x3 网格）
 *   - 手指滑动连接圆点，记录路径
 *   - 支持设置模式（首次设置）和验证模式
 *   - 回调接口通知外部图案完成
 */
public class PatternLockView extends View {

    // ==================== 常量 ====================
    private static final int GRID_SIZE = 3;
    private static final int TOTAL_DOTS = GRID_SIZE * GRID_SIZE;

    // 颜色
    private static final int COLOR_DOT_NORMAL = Color.parseColor("#BDBDBD");
    private static final int COLOR_DOT_SELECTED = Color.parseColor("#1976D2");
    private static final int COLOR_LINE = Color.parseColor("#1976D2");
    private static final int COLOR_ERROR = Color.parseColor("#F44336");

    // ==================== 画笔 ====================
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ==================== 状态 ====================
    private float[] dotX = new float[TOTAL_DOTS];
    private float[] dotY = new float[TOTAL_DOTS];
    private boolean[] dotSelected = new boolean[TOTAL_DOTS];
    private List<Integer> selectedDots = new ArrayList<>();

    private float touchX, touchY;
    private boolean isDrawing = false;
    private boolean isError = false;

    // 回调接口
    public interface OnPatternCompleteListener {
        void onPatternComplete(String pattern);
    }

    private OnPatternCompleteListener listener;

    // ==================== 构造函数 ====================
    public PatternLockView(Context context) {
        super(context);
        init();
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dotPaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        innerDotPaint.setStyle(Paint.Style.FILL);
    }

    // ==================== 布局计算 ====================
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cellW = w / (float) GRID_SIZE;
        float cellH = h / (float) GRID_SIZE;
        for (int i = 0; i < TOTAL_DOTS; i++) {
            int row = i / GRID_SIZE;
            int col = i % GRID_SIZE;
            dotX[i] = cellW * col + cellW / 2f;
            dotY[i] = cellH * row + cellH / 2f;
        }
    }

    // ==================== 绘制 ====================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int lineColor = isError ? COLOR_ERROR : COLOR_LINE;
        int selectedColor = isError ? COLOR_ERROR : COLOR_DOT_SELECTED;

        // 绘制已连接的线段
        linePaint.setColor(lineColor);
        linePaint.setAlpha(180);
        if (selectedDots.size() > 1) {
            for (int i = 0; i < selectedDots.size() - 1; i++) {
                int from = selectedDots.get(i);
                int to = selectedDots.get(i + 1);
                canvas.drawLine(dotX[from], dotY[from], dotX[to], dotY[to], linePaint);
            }
        }

        // 绘制手指到最后一个点的动态线
        if (isDrawing && selectedDots.size() > 0) {
            int last = selectedDots.get(selectedDots.size() - 1);
            linePaint.setAlpha(120);
            canvas.drawLine(dotX[last], dotY[last], touchX, touchY, linePaint);
        }

        // 绘制圆点
        float dotRadius = Math.min(getWidth(), getHeight()) / (GRID_SIZE * 2.5f);
        float innerRadius = dotRadius * 0.4f;

        for (int i = 0; i < TOTAL_DOTS; i++) {
            if (dotSelected[i]) {
                // 选中状态：外圈半透明 + 内实心
                dotPaint.setColor(selectedColor);
                dotPaint.setAlpha(40);
                canvas.drawCircle(dotX[i], dotY[i], dotRadius, dotPaint);

                innerDotPaint.setColor(selectedColor);
                innerDotPaint.setAlpha(255);
                canvas.drawCircle(dotX[i], dotY[i], innerRadius, innerDotPaint);
            } else {
                // 未选中状态：灰色实心小圆
                dotPaint.setColor(COLOR_DOT_NORMAL);
                dotPaint.setAlpha(255);
                canvas.drawCircle(dotX[i], dotY[i], innerRadius, dotPaint);
            }
        }
    }

    // ==================== 触摸处理 ====================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                isDrawing = true;
                handleTouch(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                touchY = event.getY();
                handleTouch(event.getX(), event.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isDrawing = false;
                if (selectedDots.size() >= 4) {
                    // 至少连接 4 个点才算有效图案
                    if (listener != null) {
                        listener.onPatternComplete(getPatternString());
                    }
                } else if (selectedDots.size() > 0) {
                    // 点数不足，显示错误
                    showError();
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * 检测手指是否经过某个圆点
     */
    private void handleTouch(float x, float y) {
        float threshold = Math.min(getWidth(), getHeight()) / (GRID_SIZE * 2.0f);
        for (int i = 0; i < TOTAL_DOTS; i++) {
            if (!dotSelected[i]) {
                float dx = x - dotX[i];
                float dy = y - dotY[i];
                if (Math.sqrt(dx * dx + dy * dy) < threshold) {
                    dotSelected[i] = true;
                    selectedDots.add(i);
                    invalidate();
                    break;
                }
            }
        }
    }

    // ==================== 公共方法 ====================

    /**
     * 将选中的点序列转换为字符串（如 "0-1-2-5-8"）
     */
    public String getPatternString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedDots.size(); i++) {
            if (i > 0) sb.append("-");
            sb.append(selectedDots.get(i));
        }
        return sb.toString();
    }

    /**
     * 重置图案（清除所有选中状态）
     */
    public void reset() {
        selectedDots.clear();
        for (int i = 0; i < TOTAL_DOTS; i++) dotSelected[i] = false;
        isError = false;
        invalidate();
    }

    /**
     * 显示错误状态（变红）
     */
    public void showError() {
        isError = true;
        invalidate();
        // 1 秒后自动重置
        postDelayed(this::reset, 1000);
    }

    public void setOnPatternCompleteListener(OnPatternCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 保持正方形
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                            MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }
}
