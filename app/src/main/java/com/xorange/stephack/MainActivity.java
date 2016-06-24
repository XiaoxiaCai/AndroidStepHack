package com.xorange.stephack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private TextView tv_show_step;// 步数
    private Button btn_start;// 开始按钮
    private Button btn_stop;// 停止按钮
    private int total_step = 0;   //走的总步数
    private Thread thread;  //定义线程对象
    private  long startTimer = 0;// 开始时间
    private long timer = 0;// 运动时间
    private  long tempTime = 0;
    private Intent service;

    // 当创建一个新的Handler实例时, 它会绑定到当前线程和消息的队列中,开始分发数据
    // Handler有两个作用, (1) : 定时执行Message和Runnalbe 对象
    // (2): 让一个动作,在不同的线程中执行.

    Handler handler = new Handler() {// Handler对象用于更新当前步数,定时发送消息，调用方法查询数据用于显示？？？？？？？？？？
        //主要接受子线程发送的数据, 并用此数据配合主线程更新UI
        //Handler运行在主线程中(UI线程中), 它与子线程可以通过Message对象来传递数据,
        //Handler就承担着接受子线程传过来的(子线程用sendMessage()方法传递Message对象，(里面包含数据)
        //把这些消息放入主线程队列中，配合主线程进行更新UI。

        @Override                  //这个方法是从父类/接口 继承过来的，需要重写一次
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);        // 此处可以更新UI

            /*
            如果读出来是 0，那就说明是新的一天，然后重新开始，stop，置为 0，继续开始。然后继续存数据。
             */
            SharedPreferences stepSp = MainActivity.this.getSharedPreferences("stepSp", Context.MODE_PRIVATE);
            String date = getTodayDate();
            /*
            为什么不行？当我第一次读，看见是 0，存入了0，第二次还是0，存入 1 就行。
             */
            if (stepSp.getString(date, "0").equals("0")) {
                stopService(service);
                StepDetector.CURRENT_SETP = 1;
                startService(service);
            }
            countStep();          //调用步数方法
            SharedPreferences.Editor editor = stepSp.edit();
            editor.putString(date, total_step + "");
            editor.commit();
            tv_show_step.setText(stepSp.getString(date, 0 + ""));// 显示当前步数
        }
    };

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * 实际的步数
     */
    private void countStep() {
        if (StepDetector.CURRENT_SETP % 2 == 0) {
            total_step = StepDetector.CURRENT_SETP;
        } else {
            total_step = StepDetector.CURRENT_SETP +1;
        }

        total_step = StepDetector.CURRENT_SETP;
    }

    @Override
    protected void onResume() {
        super.onResume();
        addView();
    }

    private void addView() {
        tv_show_step = (TextView) this.findViewById(R.id.show_step);
        btn_start = (Button) this.findViewById(R.id.start);
        btn_stop = (Button) this.findViewById(R.id.stop);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        service = new Intent(this, StepCounterService.class);
        if (thread == null) {

            thread = new Thread() {// 子线程用于监听当前步数的变化

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    super.run();
                    int temp = 0;
                    while (true) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (StepCounterService.FLAG) {
                            Message msg = new Message();
                            if (temp != StepDetector.CURRENT_SETP) {
                                temp = StepDetector.CURRENT_SETP;
                            }
                            if (startTimer != System.currentTimeMillis()) {
                                timer = tempTime + System.currentTimeMillis()
                                        - startTimer;
                            }
                            handler.sendMessage(msg);// 通知主线程
                        }
                    }
                }
            };
            thread.start();
        }
    }
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                Log.d("stephit", "start");
                startService(service);
                btn_start.setEnabled(false);
                btn_stop.setEnabled(true);
                btn_stop.setText("暂停");
                startTimer = System.currentTimeMillis();
                tempTime = timer;
                break;

            case R.id.stop:
                Log.d("stephit", "stop");
                stopService(service);
                if (StepCounterService.FLAG && StepDetector.CURRENT_SETP > 0) {
                    btn_stop.setText("取消");
                } else {
                    StepDetector.CURRENT_SETP = 0;
                    tempTime = timer = 0;

                    btn_stop.setText("暂停");
                    btn_stop.setEnabled(false);

                    tv_show_step.setText("0");
                    handler.removeCallbacks(thread);
                }
                btn_start.setEnabled(true);
                break;
        }
    }
}
