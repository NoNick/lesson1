package ru.ifmo.md.lesson1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

/**
 * Created by thevery on 11/09/14.
 */
class WhirlView extends SurfaceView implements Runnable {
    int[] field;
    int[] field2;
    int[] swapMatrix;
    int width = 240;
    int height = 320;
    float scaleX = 1, scaleY = 1;
    final int MAX_COLOR = 10;
    int[] color;
    int[] palette = {0xFFFF0000, 0xFF800000, 0xFF808000, 0xFF008000, 0xFF00FF00, 0xFF008080, 0xFF0000FF, 0xFF000080, 0xFF800080, 0xFFFFFFFF};
    SurfaceHolder holder;
    Thread thread = null;
    volatile boolean running = false;
    Paint paint = new Paint();
    private static final int nThreads = 4;
    Thread[] t = new Thread[nThreads];

    public WhirlView(Context context) {
        super(context);
        holder = getHolder();
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ignore) {}
    }

    public void run() {
        while (running) {
            if (holder.getSurface().isValid()) {
                long startTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();
                try {
                    updateField();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                onDraw(canvas);
                holder.unlockCanvasAndPost(canvas);
                long finishTime = System.nanoTime();
                Log.i("TIME", "Circle: " + (finishTime - startTime) / 1000000);
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        scaleX = (float) w / width;
        scaleY = (float) h / height;
        initField();
    }

    void initField() {
        field = new int[width * height];
        field2 = new int[width * height];
        color = new int[width * height];
        Random rand = new Random();
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                field[x + y * width] = rand.nextInt(MAX_COLOR);
            }
        }
    }

    void updateField() throws InterruptedException {
        final int diffX[] = {0, 1, 0, 1};
        final int diffY[] = {0, 0, 1, 1};

        for (int i = 0; i < nThreads; i++) {
            final int finalI = i;
            t[i] = new Thread() {
                @Override
                public void run() {
                    for (int x = diffX[finalI] * width / 2; x < (diffX[finalI] + 1) * width / 2; x++) {
                        for (int y = diffY[finalI] * height / 2; y < (diffY[finalI] + 1) * height / 2; y++) {

                            field2[x + y * width] = field[x + y * width];
                            int cellColor = (field[x + y * width] + 1);
                            if (cellColor >= MAX_COLOR)
                                cellColor -= MAX_COLOR;

                            discoloration:
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    int x2 = x + dx;
                                    if (x2 < 0)
                                        x2 += width;
                                    if (x2 >= width)
                                        x2 -= width;
                                    int y2 = y + dy;
                                    if (y2 < 0)
                                        y2 += width;
                                    if (y2 >= height)
                                        y2 -= height;
                                    if (cellColor == field[x2 + y2 * width]) {
                                        field2[x + y * width] = field[x2 + y2 * width];
                                        break discoloration;
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        for (int i = 0; i < nThreads; i++) {
            t[i].start();
        }
        for (int i = 0; i < nThreads; i++) {
            t[i].join();
        }

        swapMatrix = field;
        field = field2;
        field2 = swapMatrix;
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < height * width; i++) {
            color[i] = palette[field[i]];
        }

        canvas.scale(scaleX, scaleY);
        canvas.drawBitmap(color, 0, width, 0, 0, width, height, false, paint);
    }
}