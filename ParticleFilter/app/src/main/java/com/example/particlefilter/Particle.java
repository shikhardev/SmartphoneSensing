package com.example.particlefilter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.view.Display;

import java.util.HashMap;

public class Particle {
    public int x, y;
    public Cell cell;
    public boolean isAlive;
    public float weight;
    public ShapeDrawable p;
    public int oldX, oldY;  // Keeps a track of the last known position. Used when all particles have been destroyed


    public Particle(int x, int y, Cell c) {
        this.x = x;
        this.y = y;
        this.cell = c;
        this.isAlive = true;
        this.weight = 0f;
        p = new ShapeDrawable(new OvalShape());
        p.getPaint().setColor(Color.RED);
        p.setBounds(x, y, x + 10, y + 10);
    }


    public Particle(int x, int y, Cell c, float weight) {
        this.x = x;
        this.y = y;
        this.cell = c;
        this.isAlive = true;
        this.weight = weight;
        p = new ShapeDrawable(new OvalShape());
        p.getPaint().setColor(Color.RED);
        p.setBounds(x, y, x + 10, y + 10);
    }

    public void drawParticle(Canvas canvas) {
        this.p.draw(canvas);
    }

    public void move(int pixelCount, int direction) {
        int tempX = this.x;
        int tempY = this.y;

//        switch (direction) {
//            case 0:
//                this.y -= pixelCount;
//                break;
//            case 90:
//                this.x += pixelCount;
//                break;
//            case 180:
//                this.y += pixelCount;
//                break;
//            case 270:
//                this.x -= pixelCount;
//                break;
//        }

        switch (direction) {
            case 0:
                tempY -= pixelCount;
                break;
            case 90:
                tempX += pixelCount;
                break;
            case 180:
                tempY += pixelCount;
                break;
            case 270:
                tempX -= pixelCount;
                break;
        }

        this.oldX = this.x;
        this.oldY = this.y;

        if ((tempX < 50) || (tempX > 1030) || (tempY < 40) || (tempY > 2150)) {
            this.x = 1;
            this.y = 1;
        }
        else {
            this.x = tempX;
            this.y = tempY;
        }

    }

    public void  validateCell (HashMap<String, Cell> ch) {
        if ((x == 1) || (y == 1)) {
            isAlive = false;
            return;
        }

        if (x < cell.x) {
            if (cell.leftCell == null) {
                isAlive = false;
                return;
            }

            else {
                cell = ch.get(cell.leftCell);
            }
        }

        if (x > (cell.x + cell.length)) {
            if (cell.rightCell == null) {
                isAlive = false;
                return;
            }
            else {
                cell = ch.get(cell.rightCell);
            }
        }

        if (y < cell.y - cell.height) {
            if (cell.topCell == null) {
                isAlive = false;
                return;
            }
            else {
                cell = ch.get(cell.topCell);
            }
        }

        if (y > cell.y) {
            if (cell.bottomCell == null) {
                isAlive = false;
                return;
            }
            else {
                cell = ch.get(cell.bottomCell);
            }
        }
    }

    public boolean isParticleValid () {
        isAlive = false;
        if ((cell.leftCell == null) && (x < cell.x))
            return false;
        if ((cell.rightCell == null) && (x > (cell.x + cell.length)))
            return false;
        if ((cell.topCell == null) && (y < cell.y - cell.height))
            return false;
        if ((cell.bottomCell == null) && (y > cell.y))
            return false;

        isAlive = true;
        return true;
    }
}
