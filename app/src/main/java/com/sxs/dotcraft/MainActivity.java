package com.sxs.dotcraft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final int n = 9;
    private final int m = 3;
    private final ImageView[] rings = new ImageView[n];
    private final ImageView[] dots = new ImageView[n];

    private final int[] currentRingIx = new int[]{0, 2, 7};
    private final int[] currentDotIx = new int[]{0, 2, 7};

    private final List<int[]> tables = new ArrayList<>();

    private Drawable shapeRingBlue;
    private Drawable shapeDotBlue;
    private Drawable shapeDotGreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shapeRingBlue = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_ring_blue, null);
        shapeDotBlue = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_dot_blue, null);
        shapeDotGreen = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_dot_green, null);

        // 初始化圈、点对应的数组
        fillRings(rings);
        fillDots(dots);

        // 初始化棋盘布置
        fillTables();
    }

    // 重新布置棋盘
    public void restart(View view) {
        Random random = new Random();
        // 获取三个棋盘位置，这三个位置组成的应该是一个新棋盘
        int[] prevRingIx = new int[m];
        F1: while (true) {
            int[] table = tables.get(random.nextInt(tables.size()));
            for (int i = 0; i < m; i++) {
                if (table[i] != currentRingIx[i]) {
                    for (int j = 0; j < m; j++) {
                        prevRingIx[j] = currentRingIx[j];
                        currentRingIx[j] = table[j];
                    }
                    break F1;
                }
            }
        }

        // 获取三个棋子的位置，这三个棋子生成后，应该保障还需要移动棋子才能获胜
        int[] prevDotIx = new int[m];
        F2: while (true) {
            int[] table = tables.get(random.nextInt(tables.size()));
            for (int i = 0; i < m; i++) {
                // 要保障点的新位置不是标准答案，所以至少要有一个点和当前环的位置不同
                if (table[i] != currentRingIx[i]) {
                    for (int j = 0; j < m; j++) {
                        prevDotIx[j] = currentDotIx[j];
                        currentDotIx[j] = table[j];
                    }
                    break F2;
                }
            }
        }
        // 渲染棋盘、棋子

        for (int i = 0; i < 3; i++) {
            // 将之前位置的环去掉
            rings[prevRingIx[i]].setBackground(null);
            // 将之前位置的点变为深色
            dots[prevDotIx[i]].setBackground(shapeDotGreen);
        }

        for (int i = 0; i < 3; i++) {
            // 新位置放上环
            rings[currentRingIx[i]].setBackground(shapeRingBlue);
            // 将新位置的点变为浅色
            dots[currentDotIx[i]].setBackground(shapeDotBlue);
        }
    }

    // 获取页面上的圈并依次放入数组
    private void fillRings(ImageView[] rings) {
        rings[0] = (ImageView) findViewById(R.id.ring1);
        rings[1] = (ImageView) findViewById(R.id.ring2);
        rings[2] = (ImageView) findViewById(R.id.ring3);
        rings[3] = (ImageView) findViewById(R.id.ring4);
        rings[4] = (ImageView) findViewById(R.id.ring5);
        rings[5] = (ImageView) findViewById(R.id.ring6);
        rings[6] = (ImageView) findViewById(R.id.ring7);
        rings[7] = (ImageView) findViewById(R.id.ring8);
        rings[8] = (ImageView) findViewById(R.id.ring9);
    }

    // 获取页面上的点并依次放入数组
    private void fillDots(ImageView[] dots) {
        dots[0] = (ImageView) findViewById(R.id.dot1);
        dots[1] = (ImageView) findViewById(R.id.dot2);
        dots[2] = (ImageView) findViewById(R.id.dot3);
        dots[3] = (ImageView) findViewById(R.id.dot4);
        dots[4] = (ImageView) findViewById(R.id.dot5);
        dots[5] = (ImageView) findViewById(R.id.dot6);
        dots[6] = (ImageView) findViewById(R.id.dot7);
        dots[7] = (ImageView) findViewById(R.id.dot8);
        dots[8] = (ImageView) findViewById(R.id.dot9);
    }

    // 初始化棋盘可能的布置列表，便于置换随机切换布局
    private void fillTables() {
        // 共 9 个点，随机选取三个点，共有 C(3,9) = 84 种方式

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int z = j + 1; z < n; z++) {
                    tables.add(new int[] {i, j, z});
                }
            }
        }
    }
}