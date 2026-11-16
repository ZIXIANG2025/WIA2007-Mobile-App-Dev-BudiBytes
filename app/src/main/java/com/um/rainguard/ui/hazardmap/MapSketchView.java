package com.um.rainguard.ui.hazardmap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class MapSketchView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<ReportItem> mapReports = new ArrayList<>();

    public MapSketchView(Context context) {
        super(context);
        init();
    }

    public MapSketchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapSketchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.rgb(235, 244, 246));
    }

    public void setMapReports(List<ReportItem> reports) {
        this.mapReports.clear();
        if (reports != null) {
            this.mapReports.addAll(reports);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();

        // Canvas Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(203, 213, 225));
        paint.setStrokeWidth(dp(1));
        canvas.drawRoundRect(1, 1, width - 1, height - 1, dp(16), dp(16), paint);

        // Main road curve
        paint.setColor(Color.rgb(196, 218, 217));
        paint.setStrokeWidth(dp(16));
        paint.setStyle(Paint.Style.STROKE);
        Path road = new Path();
        road.moveTo(0, height * .42f);
        road.cubicTo(width * .35f, height * .28f, width * .55f, height * .68f, width, height * .45f);
        canvas.drawPath(road, paint);

        // Dashed branch road
        paint.setStrokeWidth(dp(6));
        paint.setColor(Color.rgb(180, 205, 204));
        paint.setPathEffect(new DashPathEffect(new float[]{dp(8), dp(6)}, 0));
        Path dashRoad = new Path();
        dashRoad.moveTo(0, height * .22f);
        dashRoad.lineTo(width * .45f, height * .52f);
        canvas.drawPath(dashRoad, paint);
        paint.setPathEffect(null);

        // Map Pins & Tooltip
        float[][] positions = {
                {.32f, .32f}, {.65f, .42f}, {.48f, .55f}, {.25f, .72f}, {.72f, .70f}
        };

        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < mapReports.size() && i < positions.length; i++) {
            ReportItem item = mapReports.get(i);
            float px = width * positions[i][0];
            float py = height * positions[i][1];

            // Outer glow circle
            paint.setColor(UIUtils.riskColor(item.risk));
            canvas.drawCircle(px, py, dp(16), paint);

            // Inner symbol drop circle
            paint.setColor(Constants.WHITE);
            canvas.drawCircle(px, py, dp(6), paint);

            // Draw tooltip label for main pin
            if (i == 0) {
                String labelText = item.place.length() > 16 ? item.place.substring(0, 16) + "..." : item.place;
                paint.setTextSize(dp(11));
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                float textWidth = paint.measureText(labelText);

                float boxWidth = textWidth + dp(20);
                float boxHeight = dp(24);
                float boxLeft = px - boxWidth / 2f;
                float boxTop = py - dp(34);

                paint.setColor(Constants.WHITE);
                canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, dp(12), dp(12), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.rgb(203, 213, 225));
                paint.setStrokeWidth(dp(1));
                canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, dp(12), dp(12), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(15, 23, 42));
                canvas.drawText(labelText, boxLeft + dp(10), boxTop + dp(16), paint);
            }
        }

        // Map Legend overlay box at bottom left
        float legendLeft = dp(14);
        float legendBottom = height - dp(14);
        float legendWidth = dp(110);
        float legendHeight = dp(62);
        float legendTop = legendBottom - legendHeight;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Constants.WHITE);
        canvas.drawRoundRect(legendLeft, legendTop, legendLeft + legendWidth, legendBottom, dp(10), dp(10), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(226, 232, 240));
        paint.setStrokeWidth(dp(1));
        canvas.drawRoundRect(legendLeft, legendTop, legendLeft + legendWidth, legendBottom, dp(10), dp(10), paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(10));

        // Legend item 1: High risk
        paint.setColor(Constants.DANGER);
        canvas.drawCircle(legendLeft + dp(12), legendTop + dp(14), dp(4), paint);
        paint.setColor(Color.rgb(15, 23, 42));
        canvas.drawText("High risk", legendLeft + dp(22), legendTop + dp(17), paint);

        // Legend item 2: Medium risk
        paint.setColor(Constants.WARNING);
        canvas.drawCircle(legendLeft + dp(12), legendTop + dp(31), dp(4), paint);
        paint.setColor(Color.rgb(15, 23, 42));
        canvas.drawText("Medium risk", legendLeft + dp(22), legendTop + dp(34), paint);

        // Legend item 3: Low risk
        paint.setColor(Constants.SUCCESS);
        canvas.drawCircle(legendLeft + dp(12), legendTop + dp(48), dp(4), paint);
        paint.setColor(Color.rgb(15, 23, 42));
        canvas.drawText("Low risk", legendLeft + dp(22), legendTop + dp(51), paint);
    }

    private int dp(int value) {
        return UIUtils.dp(getContext(), value);
    }
}
