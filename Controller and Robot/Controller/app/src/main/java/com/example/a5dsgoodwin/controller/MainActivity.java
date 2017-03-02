package com.example.a5dsgoodwin.controller;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.util.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity {
    private Socket socket = null;
    private BufferedWriter writer = null;
    private DataInputStream reader = null;
    private Handler handler = new Handler();
    private Button btnConnect, btnDisconnect, btnPhoto, btnVideo, btnPreview;
    private ImageButton btnUp, btnLeft, btnRight, btnDown, btnVoice;
    private ImageView ivDisplay;
    private EditText etIP;
    boolean keepConnected = false, keepPreview = false, isRecording = false;
    final static int PORT = 9876;
    final static int MAXBYTESLEN = 1024;

    private SpeechRecognizer recognizer;
    private RecognizerDialog recognizerDialog;
    private HashMap<String, String> resultsMap = new LinkedHashMap<String, String>();
    private SharedPreferences sharedPreferences;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SpeechUtility.createUtility(MainActivity.this, "appid=" + getString(R.string.app_id));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setElementListener();
        initVoiceRecognitionModule();
    }

    public void init(){
        etIP = (EditText)findViewById(R.id.et_ip);
        btnConnect = (Button)findViewById(R.id.btn_connect);
        btnDisconnect = (Button)findViewById(R.id.btn_disconnect);
        btnLeft = (ImageButton)findViewById(R.id.btn_left);
        btnRight = (ImageButton)findViewById(R.id.btn_right);
        btnUp = (ImageButton)findViewById(R.id.btn_up);
        btnDown = (ImageButton)findViewById(R.id.btn_down);
        btnPhoto = (Button)findViewById(R.id.btn_photo);
        btnVideo = (Button)findViewById(R.id.btn_video);
        btnPreview = (Button)findViewById(R.id.btn_preview);
        btnVoice = (ImageButton)findViewById(R.id.btn_voice);
        ivDisplay = (ImageView)findViewById(R.id.iv_display);
    }

    public void setElementListener() {

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keepConnected = true;
                socketConnect();
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keepConnected = false;
            }
        });

        btnUp.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    try {
                        writer.write("up\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    try {
                        writer.write("stop\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    try {
                        writer.write("left\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    try {
                        writer.write("stop\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    try {
                        writer.write("right\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    try {
                        writer.write("stop\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        btnDown.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    try {
                        writer.write("down\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    try {
                        writer.write("stop\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    writer.write("photo\n");
                    writer.flush();
                    receivePhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRecording){
                    try {
                        writer.write("startRecord\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        btnPhoto.setEnabled(false);
                        btnVideo.setBackgroundResource(R.mipmap.videostop);
                        isRecording = true;
                    }
                }else{
                    try {
                        writer.write("stopRecord\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        btnPhoto.setEnabled(true);
                        btnVideo.setBackgroundResource(R.mipmap.videostart);
                        isRecording = false;
                    }
                }
            }
        });

        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!keepPreview){
                    try {
                        writer.write("startPreview\n");
                        writer.flush();
                        keepPreview = true;
                        receiveFrame();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        btnPreview.setBackgroundResource(R.mipmap.stoppreview);
                    }
                }else{
                    try {
                        writer.write("stopPreview\n");
                        writer.flush();
                        keepPreview = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        btnPreview.setBackgroundResource(R.mipmap.startpreview);
                    }
                }
            }
        });

        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultsMap.clear();
                setParam();
                recognizerDialog.setListener(new RecognizerDialogListener() {
                    @Override
                    public void onResult(RecognizerResult results, boolean isLast) {
                        String resultString = getResult(results);
                        if (!keepConnected) {
                            Toast.makeText(MainActivity.this, "尚未连接机器人", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        switch(resultString){
                            case("向前"):
                                try {
                                    writer.write("up\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case("向后"):
                                try {
                                    writer.write("down\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case("向左"):
                                try {
                                    writer.write("left\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case("向右"):
                                try {
                                    writer.write("right\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case("停止"):
                                try {
                                    writer.write("stop\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    @Override
                    public void onError(SpeechError error) {
                        Toast.makeText(MainActivity.this, error.getPlainDescription(true), Toast.LENGTH_SHORT).show();
                    }
                });
                recognizerDialog.show();
                Toast.makeText(MainActivity.this, getString(R.string.text_begin), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void initVoiceRecognitionModule(){
        InitListener initListener = new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Toast.makeText(MainActivity.this, "初始化失败，错误码：" + code, Toast.LENGTH_SHORT).show();
                }
            }
        };
        recognizer = SpeechRecognizer.createRecognizer(MainActivity.this, initListener);
        recognizerDialog = new RecognizerDialog(MainActivity.this, initListener);
        sharedPreferences = getSharedPreferences("com.iflytek.setting", Activity.MODE_PRIVATE);
    }

    public void socketConnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ip = etIP.getText().toString();
                if(!ip.matches("(\\d{1,3}.){3}\\d{1,3}")){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "IP地址不合法", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                try {
                    socket = new Socket(ip, PORT);
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    reader = new DataInputStream(socket.getInputStream());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                            etIP.setEnabled(false);
                            btnConnect.setEnabled(false);
                            btnDisconnect.setEnabled(true);
                        }
                    });
                    while(keepConnected);
                } catch (Exception e) {

                } finally {
                    try {
                        socket.close();
                        writer.close();
                        reader.close();
                    } catch (IOException e) {

                    } finally {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
                                etIP.setEnabled(true);
                                etIP.requestFocus();
                                btnConnect.setEnabled(true);
                                btnDisconnect.setEnabled(false);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void receiveFile(final String filePath){
        try {
            long totalLength = reader.readLong();
            FileOutputStream fOutputStream = new FileOutputStream(filePath);
            byte[] bytes = new byte[MAXBYTESLEN];
            int length;
            while (totalLength > 0) {
                length = reader.read(bytes);
                fOutputStream.write(bytes, 0, length);
                totalLength -= length;
            }
            fOutputStream.flush();
            fOutputStream.close();
        } catch (Exception e) {

        }
    }

    public void receivePhoto(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String filePath = Environment.getExternalStorageDirectory().getPath() + "/photo.jpg";
                receiveFile(filePath);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = null;
                        try{
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(filePath)));
                        }catch (Exception e){

                        } finally {
                            ivDisplay.setImageBitmap(bitmap);
                        }
                    }
                });
            }
        }).start();
    }

    public void receiveFrame(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true){
                        //read frame data
                        int totalLength = 0;
                        totalLength = reader.readInt();
                        if(totalLength == -1)
                            break;
                        byte[] bytes = new byte[totalLength];
                        int length = 0;
                        while (length < totalLength) {
                            length += reader.read(bytes, length, totalLength - length);
                        }
                        //decode the data and display
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, totalLength);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(90);
                        final Bitmap bitmapRot = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ivDisplay.setImageBitmap(bitmapRot);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        resultsMap.put(sn, text);
        String resultString = "";
        for (String key : resultsMap.keySet()) {
            resultString += resultsMap.get(key);
        }
        return resultString;
    }

    /**
     * 参数设置
     */
    public void setParam() {
        recognizer.setParameter(SpeechConstant.PARAMS, null);// 清空参数
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);// 设置听写引擎
        recognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");// 设置返回结果格式
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置语言
        recognizer.setParameter(SpeechConstant.VAD_BOS, sharedPreferences.getString("iat_vadbos_preference", "4000"));// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        recognizer.setParameter(SpeechConstant.VAD_EOS, sharedPreferences.getString("iat_vadeos_preference", "1000"));// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        recognizer.setParameter(SpeechConstant.ASR_PTT, sharedPreferences.getString("iat_punc_preference", "0"));// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        recognizer.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        recognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/iat.wav");// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.destroy();
    }
}
