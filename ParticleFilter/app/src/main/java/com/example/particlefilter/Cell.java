package com.example.particlefilter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.Display;

import org.json.JSONException;
import org.json.JSONObject;

public class Cell {
    public int x;   // Bottom Left X
    public int y;   // Bottom Left Y
    public int length;
    public int height;
    public String topCell,bottomCell, leftCell, rightCell;
    public String cellID;

    public Cell (int x, int y, int length, int height, String top, String bottom, String left, String right) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.length = length;
        this.topCell = top;
        this.bottomCell = bottom;
        this.leftCell = left;
        this.rightCell = right;
    }

    public Cell (String cellID, JSONObject cellData) throws JSONException {
        String temp;
        this.cellID = cellID;
        this.x =  (Integer) cellData.get("x");
        this.y = (Integer) cellData.get("y");
        this.length = (Integer) cellData.get("length");
        this.height = (Integer) cellData.get("height");
        temp = (String) cellData.get("left");
        this.leftCell = temp.equals("none")? null: temp;
        temp = (String) cellData.get("right");
        this.rightCell = temp.equals("none")? null: temp;
        temp = (String) cellData.get("top");
        this.topCell = temp.equals("none")? null: temp;
        temp = (String) cellData.get("bottom");
        this.bottomCell = temp.equals("none")? null: temp;
    }

    public void drawCell (Canvas canvas) {
        ShapeDrawable left =  new ShapeDrawable(new RectShape());
        ShapeDrawable right =  new ShapeDrawable(new RectShape());
        ShapeDrawable top =  new ShapeDrawable(new RectShape());
        ShapeDrawable bottom =  new ShapeDrawable(new RectShape());


        if (this.bottomCell == null) {
            // Draw wall in the bottom
            bottom.setBounds(this.x, y-10, x + length, y);
            bottom.draw(canvas);
        }
        if (this.leftCell == null) {
            // Draw wall in the left
            left.setBounds(this.x - 10, y - height - 10, x, y);
            left.draw(canvas);
        }
        if (this.rightCell == null) {
            // Draw wall in the right
            right.setBounds(this.x + length, y - height -10, x + length + 10, y);
            right.draw(canvas);
        }
        if (this.topCell == null) {
            // Draw wall at the top
            top.setBounds(this.x, y - height - 10, x + length, y - height);
            top.draw(canvas);
        }
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(90);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        canvas.drawText("1", 250, 2050, paint);
        canvas.drawText("2", 530, 2050, paint);
        canvas.drawText("3", 800, 2050, paint);
        canvas.drawText("4", 530, 1830, paint);
        canvas.drawText("5", 530, 1630, paint);
        canvas.drawText("6", 530, 1430, paint);
        canvas.drawText("7", 530, 1230, paint);
        canvas.drawText("8", 530, 1020, paint);
        canvas.drawText("9", 530, 820, paint);
        canvas.drawText("10", 530, 600, paint);
        canvas.drawText("11", 800, 600, paint);
        canvas.drawText("12", 530, 380, paint);
        canvas.drawText("13", 800, 380, paint);
        canvas.drawText("14", 100, 180, paint);
        canvas.drawText("15", 525, 180, paint);
        canvas.drawText("16", 800, 180, paint);
    }


    @Override
    public boolean equals(Object obj) {
        Cell target = (Cell) obj;
        if (this.x != target.x)
            return false;
        if (this.y != target.y)
            return false;
        if (this.length != target.length)
            return false;
        if (this.height != target.height)
            return false;
        return true;
    }
}
