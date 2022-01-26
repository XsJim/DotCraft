package com.sxs.dotcraft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    // 手指不在点上
    private static final int STATE_IDLE = 0;
    // 手指在点上
    private static final int STATE_WAITING_DRAG = 1;
    // 手指在点上且水平拖动
    private static final int STATE_HORIZONTAL_DRAG = 2;
    // 手指在点上且垂直拖动
    private static final int STATE_VERTICAL_DRAG = 3;
    // 某些没有状态的情况
    private static final int STATELESS = -1;

    // 当前手指状态变量
    private int state = STATE_IDLE;

    // 最后记录的手指位置
    private float prevMotionX;
    private float prevMotionY;

    private int touchDotIndex = -1;

    // 上一次是水平拖动还是垂直拖动
    private int prevHV = STATELESS;

    // 用于使滑动看起来连续的备用点
    private ImageView backupDot;

    // 一个点的最大滑动距离，限制为 1 格
    private float MAX_TRANSLATION;

    private static final int MOVE_RIGHT = 0;
    private static final int MOVE_LEFT = 1;
    private static final int MOVE_UP = 2;
    private static final int MOVE_DOWN = 3;

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

    // 用于显分数的文本视图
    private TextView scoreView;
    // 用于记录分数的变量
    private int score;

    // 用于保存键值对数据的对象
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEdit;

    // 小鸭子当前的位置
    private boolean duckAtRight = true;

    // 震动器
    private Vibrator vibrator;

    // 震动效果

    // 点击
    private final VibrationEffect effect_click = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    // 双击
    private final VibrationEffect effect_double_click = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);

    // 重击
    private final VibrationEffect effect_heavy_click = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK);

    // 轻击
    private final VibrationEffect effect_tick = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);

    // 鸭子
    private final VibrationEffect effect_duck = VibrationEffect.createWaveform(new long[] {10, 20, 30, 40, 100, 50, 20, 10}, -1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shapeRingBlue = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_ring_blue, null);
        shapeDotBlue = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_dot_blue, null);
        shapeDotGreen = ResourcesCompat.getDrawable(getResources(), R.drawable.shape_dot_green, null);

        backupDot = findViewById(R.id.backupDot);

        // 加载键值存储器
        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPrefEdit = sharedPref.edit();

        // 初始化震动器
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        // 加载并显示分数
        scoreView = findViewById(R.id.score);
        setScore(sharedPref.getInt(getString(R.string.saved_high_score_key), 0));

        // 初始化圈、点对应的数组
        fillRings(rings);
        fillDots(dots);

        // 初始化棋盘布置
        fillTables();

        // 打乱棋盘
        restart(null);
    }

    // 用户交互
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            // 第一次按下按钮
            case MotionEvent.ACTION_DOWN: {
                // 更新手指位置
                prevMotionX = event.getRawX();
                prevMotionY = event.getRawY();

                // 更新当前状态
                state = STATE_IDLE;
                // 判断手指位置是否有点，首先获取手指处的点的下标
                touchDotIndex = getTouchDotIndex(prevMotionX, prevMotionY);

                if (touchDotIndex != -1) {
                    state = STATE_WAITING_DRAG;

                    // 触发一次点击震动
                    vibrator.vibrate(effect_click);
                }

                break;
            }
            // 手指开始拖动
            case MotionEvent.ACTION_MOVE: {
                // 当前手指位置
                float currentMotionX = event.getRawX();
                float currentMotionY = event.getRawY();

                // 手指拖动，游戏产生反馈，仅当手指已经按下过，且按下的位置有点，即当前处于等待拖动状态
                if (state == STATE_WAITING_DRAG) {

                    // 和之前位置的差
                    float diffX = currentMotionX - prevMotionX;
                    float diffY = currentMotionY - prevMotionY;

                    // 通过比较 x, y 方向移动的距离，来得到手指是水平移动还是垂直移动
                    if (Math.abs(diffX) < Math.abs(diffY)) {
                        // 垂直移动
                        state = STATE_VERTICAL_DRAG;
                    } else {
                        // 水平移动
                        state = STATE_HORIZONTAL_DRAG;
                    }

                    // 开始将动作反馈到视图
                    // 从按下某点，并开始拖动后，直到手指松开，这一行或者一列点都应该在一个轴上移动，水平或垂直
                    if (prevHV == state || prevHV == STATELESS) {
                        // 如果本次按下后还没有移动，那么将这次拖动定为标准
                        prevHV = state;

                        if (backupDot.getVisibility() == View.INVISIBLE) {
                            backupDot.setVisibility(View.VISIBLE);
                        }
                        if (state == STATE_HORIZONTAL_DRAG) {
                            // 水平拖动一行
                            horizontalDragging(touchDotIndex / 3, diffX);
                        } else {
                            // 垂直拖动一列
                            verticalDragging(touchDotIndex % 3, diffY);
                        }
                    }
                    // 否则这次拖动和本次按下并拖动的标准不同，那这次移动是无效的
                    // 但是最后的手指位置应该更新

                    // 更新之前位置
                    prevMotionX = currentMotionX;
                    prevMotionY = currentMotionY;

                    // 状态改回等待拖动状态
                    state = STATE_WAITING_DRAG;
                }

                break;
            }
            // 手指抬起
            case MotionEvent.ACTION_UP: {

                // 水平拖动或者垂直拖动
                // 首先计算本次拖动据起始点最终的距离
                // 然后将视图复位到拖动之前
                // 之后改变实际的棋盘数据
                // 然后改变视图

                if (prevHV == STATE_HORIZONTAL_DRAG) {
                    horizontalDragEND(touchDotIndex/3);
                } else if (prevHV == STATE_VERTICAL_DRAG) {
                    verticalDragEND(touchDotIndex%3);
                }

                state = STATE_IDLE;
                prevHV = STATELESS;
                break;
            }

            default:
        }

        // 事件已经被处理
        return true;
    }

    // 清除分数
    public void clearScore(View view) {
        setScore(0);

        // 触发一次双震动
        vibrator.vibrate(effect_double_click);
    }

    // 点击小鸭子
    public void duck(View view) {
        vibrator.vibrate(effect_duck);
        ObjectAnimator animation;
        if (duckAtRight) {
            animation = ObjectAnimator.ofFloat(view, "translationX", 230);
        } else {
            animation = ObjectAnimator.ofFloat(view, "translationX", -230f);
        }

        duckAtRight = !duckAtRight;

        animation.setDuration(500);
        animation.start();
    }

    /**
     * 用传入的 score 替代当前的全局 score ，这个方法同时更新视图、并更新持久存储以及其他于分数相关联的内容，所有修改 score 都应该使用此方法
     * @param score 全局 score 的更新目标
     */
    public void setScore(int score) {
        this.score = score;

        // 将分数更新到分数视图
        scoreView.setText(String.valueOf(score));

        // 将分数更新到内部储存

        sharedPrefEdit.putInt(getString(R.string.saved_high_score_key), score);
        sharedPrefEdit.commit();
    }

    /**
     * 判断当前点、环数据是否算是通关
     * @return 通关则返回 true
     */
    public boolean isPass() {
        // 所有的更新机制，都保证了存放蓝点、环下标的数组是从小到大排列的
        for (int i = 0; i < m; i++) {
            if (currentDotIx[i] != currentRingIx[i]) return false;
        }

        return true;
    }

    /**
     * 手指抬起后，进行相关行的数据、视图变更
     * @param rowNum 被拖动的行号
     */
    public void horizontalDragEND(int rowNum) {
        // 首先得到这行的三个点
        ImageView leftDotView = dots[rowNum*3];
        ImageView midDotView = dots[rowNum*3+1];
        ImageView rightDotView = dots[rowNum*3+2];

        // 得到滑动的距离，这个距离已经被拖动方法进行了限制，只能是左右一格内
        float translationX = leftDotView.getTranslationX();

        // 点全部回退到原位
        leftDotView.setTranslationX(0);
        midDotView.setTranslationX(0);
        rightDotView.setTranslationX(0);

        backupDot.setTranslationX(0);
        backupDot.setVisibility(View.INVISIBLE);
        // 判断滑动距离是否大于格子的一半
        if (translationX >= (float)2.625*backupDot.getWidth()/2) {
            // 应该向右滑动
            // 更新数据和视图
            updateDots(MOVE_RIGHT, rowNum);

        } else if (translationX <= -(float)2.625*backupDot.getWidth()/2) {
            // 应该向左滑动
            // 更新数据和视图
            updateDots(MOVE_LEFT, rowNum);
        }
    }

    /**
     * 手指抬起后，进行相关列的数据、视图变更
     * @param column 被拖动的列号
     */
    public void verticalDragEND(int column) {
        // 首先得到这行的三个点
        ImageView topDotView = dots[column];
        ImageView midDotView = dots[column+3];
        ImageView bottomDotView = dots[column+6];

        // 得到滑动的距离，这个距离已经被拖动方法进行了限制，只能是左右一格内
        float translationY = topDotView.getTranslationY();

        // 点全部回退到原位
        topDotView.setTranslationY(0);
        midDotView.setTranslationY(0);
        bottomDotView.setTranslationY(0);

        backupDot.setTranslationY(0);
        backupDot.setVisibility(View.INVISIBLE);

        // 判断滑动距离是否大于格子的一半
        if (translationY >= (float)2.625*backupDot.getWidth()/2) {
            // 应该向下滑动
            // 更新数据和视图
            updateDots(MOVE_DOWN, column);

        } else if (translationY <= -(float)2.625*backupDot.getWidth()/2) {
            // 应该向上滑动
            // 更新数据和视图
            updateDots(MOVE_UP, column);
        }
    }

    /**
     * 给定移动方向及移动的行号或列号，该方法将首先修改数据，然后更新视图
     * @param move 移动方向
     * @param num 被移动行的行数或列的列数
     */
    public void updateDots(int move, int num) {
        // 创建一个当前棋盘的映射
        boolean[] map = new boolean[n];

        for (int i = 0; i < m; i++) {
            map[currentDotIx[i]] = true;
        }
        switch (move) {
            case MOVE_RIGHT: {
                boolean a = map[num*3];
                boolean b = map[num*3+1];
                boolean c = map[num*3+2];

                map[num*3] = c;
                map[num*3+1] = a;
                map[num*3+2] = b;

                break;
            }
            case MOVE_LEFT: {
                boolean a = map[num*3];
                boolean b = map[num*3+1];
                boolean c = map[num*3+2];

                map[num*3] = b;
                map[num*3+1] = c;
                map[num*3+2] = a;
                break;
            }
            case MOVE_UP: {
                boolean a = map[num];
                boolean b = map[num+3];
                boolean c = map[num+6];

                map[num] = b;
                map[num+3] = c;
                map[num+6] = a;
                break;
            }
            case MOVE_DOWN: {
                boolean a = map[num];
                boolean b = map[num+3];
                boolean c = map[num+6];

                map[num] = c;
                map[num+3] = a;
                map[num+6] = b;
                break;
            }
        }

        // 更新数据
        for (int i = 0, j = 0; i < n && j < m; i++) {
            if (map[i]) {
                currentDotIx[j++] = i;
            }
        }

        // 更新视图
        updateDotsView();

        // 移动之后来个小型震动效果
        vibrator.vibrate(effect_tick);

        // 判断是否胜利
        if (isPass()) {
            // 胜利之后要干啥呢

            // 先来点非常高级的效果
            // 一次加重的点击震动
            vibrator.vibrate(effect_heavy_click);

            // 加上分数
            setScore(score+1);

            // 再来个新的棋盘
            restart(null);
        }
    }

    // 根据当前蓝点的位置记录来更新棋盘
    public void updateDotsView() {
        // 首先，将棋盘所有点全部变为深色
        for (int i = 0; i < n; i++) {
            dots[i].setBackground(shapeDotGreen);
        }

        // 然后，让蓝色点出现
        for (int i = 0; i < m; i++) {
            dots[currentDotIx[i]].setBackground(shapeDotBlue);
        }
    }

    public float getValidTranslation(float translation) {
        if (MAX_TRANSLATION == 0) {
            // 2.625*backupDot.getWidth() 就是一个点的宽度加上两个点之间的距离的总长
            MAX_TRANSLATION = (float)2.625*backupDot.getWidth();
        }
        if (translation > MAX_TRANSLATION) {
            return MAX_TRANSLATION;
        }
        return Math.max(translation, -MAX_TRANSLATION);
    }

    /**
     * 水平拖动一行点
     * @param rowNum 将要拖动的行号
     * @param dist 拖动的距离
     */
    public void horizontalDragging(int rowNum, float dist) {
        // 首先得到这行的三个点
        ImageView leftDotView = dots[rowNum*3];
        ImageView midDotView = dots[rowNum*3+1];
        ImageView rightDotView = dots[rowNum*3+2];

        // 计算要滑动后的相对位置，滑动后的位置最大为初始位置的左右一格内
        float translationX = getValidTranslation(leftDotView.getTranslationX() + dist);

        // 开始滑动
        leftDotView.setTranslationX(translationX);
        midDotView.setTranslationX(translationX);
        rightDotView.setTranslationX(translationX);

        // 在边界处补上一个点，看起来左右循环
        if (translationX > 0) {
            // 向右滑
            backupDot.setTranslationX(-(float)2.625*backupDot.getWidth()+translationX);

            backupDot.setTranslationY(rowNum*backupDot.getHeight() + (float)1.625*rowNum*backupDot.getHeight());

            backupDot.setBackground(rightDotView.getBackground());
        } else {
            // 向左滑
            backupDot.setTranslationX(3*backupDot.getWidth()+3*(float)1.625*backupDot.getWidth()+translationX);

            backupDot.setTranslationY(rowNum*backupDot.getHeight() + (float)1.625*rowNum*backupDot.getHeight());

            backupDot.setBackground(leftDotView.getBackground());
        }
    }

    /**
     * 垂直拖动一列点
     * @param column 将要拖动的列号
     * @param dist 拖动的距离
     */
    public void verticalDragging(int column, float dist) {
        // 首先得到这列的三个点
        ImageView topDotView = dots[column];
        ImageView midDotView = dots[column+3];
        ImageView bottomDotView = dots[column+6];

        // 计算要滑动的距离
        float translationY = getValidTranslation(topDotView.getTranslationY() + dist);

        // 开始滑动
        topDotView.setTranslationY(translationY);
        midDotView.setTranslationY(translationY);
        bottomDotView.setTranslationY(translationY);

        // 在边界处补上一个点，看起来左右循环
        if (translationY > 0) {
            // 向下滑
            backupDot.setTranslationY(-(float)2.625*backupDot.getWidth()+translationY);

            backupDot.setTranslationX(column*backupDot.getHeight() + (float)1.625*column*backupDot.getHeight());

            backupDot.setBackground(bottomDotView.getBackground());
        } else {
            // 向上滑
            backupDot.setTranslationY(3*backupDot.getWidth()+3*(float)1.625*backupDot.getWidth()+translationY);

            backupDot.setTranslationX(column*backupDot.getHeight() + (float)1.625*column*backupDot.getHeight());

            backupDot.setBackground(topDotView.getBackground());
        }
    }

    // 得到传入坐标所对应的点的下标，如果这个位置上没有点，那么该方法将返回 -1
    // 2022-1-23 加大判断范围（修改为判断环所在区域），更容易拖动
    public int getTouchDotIndex(float x, float y) {
        for (int i = 0; i < n; i++) {
            ImageView ring = rings[i];

            // 得到视图左上角在屏幕上的位置
            int[] outLocation = new int[2];
            ring.getLocationOnScreen(outLocation);

            // 左边界对应于 x 轴的位置
            int left = outLocation[0];

            // 上边界对应于 y 轴的位置
            int top = outLocation[1];

            // 进而通过这个视图的宽、高来得到该视图所在的右边、下边
            int right = left + ring.getWidth();
            int bottom = top + ring.getHeight();

            // 判断传入位置是否在视图所在矩形内部

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return i;
            }
        }

        return -1;
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

        // 更新棋盘后来两次震动
        vibrator.vibrate(effect_double_click);
    }

    // 获取页面上的圈并依次放入数组
    private void fillRings(ImageView[] rings) {
        rings[0] = findViewById(R.id.ring1);
        rings[1] = findViewById(R.id.ring2);
        rings[2] = findViewById(R.id.ring3);
        rings[3] = findViewById(R.id.ring4);
        rings[4] = findViewById(R.id.ring5);
        rings[5] = findViewById(R.id.ring6);
        rings[6] = findViewById(R.id.ring7);
        rings[7] = findViewById(R.id.ring8);
        rings[8] = findViewById(R.id.ring9);
    }

    // 获取页面上的点并依次放入数组
    private void fillDots(ImageView[] dots) {
        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);
        dots[4] = findViewById(R.id.dot5);
        dots[5] = findViewById(R.id.dot6);
        dots[6] = findViewById(R.id.dot7);
        dots[7] = findViewById(R.id.dot8);
        dots[8] = findViewById(R.id.dot9);
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