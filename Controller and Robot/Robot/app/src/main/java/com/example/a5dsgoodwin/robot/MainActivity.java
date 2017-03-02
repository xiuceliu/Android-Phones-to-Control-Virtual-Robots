package com.example.a5dsgoodwin.robot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private TextView tvState;
    private ImageButton btnSwitch;
    private ImageView ivDirection;
    private SurfaceView cameraView;
    private SurfaceHolder cameraHolder;
    private MediaRecorder mediarecorder = null;
    private Camera camera = null;

    private ServerSocket serverSocket = null;
    final static int PORT = 9876;
    private Socket socket = null;
    private BufferedReader reader = null;
    private DataOutputStream writer = null;
    final static int MAXBYTESLEN = 1024;
    private String command = null;
    boolean robotOn = false, previewOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setElementListener();
    }

    public void init(){
        tvState = (TextView)findViewById(R.id.tv_state);
        cameraView = (SurfaceView)findViewById(R.id.sv_video);
        btnSwitch = (ImageButton)findViewById(R.id.btn_switch);
        ivDirection = (ImageView) findViewById(R.id.iv_direction);
        cameraHolder = cameraView.getHolder();
        cameraHolder.addCallback(this);
    }

    public void setElementListener() {
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread serverThread;
                if(!robotOn){
                    serverThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                serverSocket = new ServerSocket(PORT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                while (true) {
                                    try {
                                        socket = serverSocket.accept();
                                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                        writer = new DataOutputStream(socket.getOutputStream());
                                        socketHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                tvState.setText("遥控器已连接...");
                                            }
                                        });
                                        Message message;
                                        while ((command = reader.readLine()) != null) {
                                            message = socketHandler.obtainMessage();
                                            message.obj = command;
                                            message.sendToTarget();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        closeCamera();
                                        try {
                                            socket.close();
                                            reader.close();
                                        } catch (IOException e) {

                                        } finally {
                                            if(robotOn){
                                                socketHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        tvState.setText("遥控器未连接...");
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    });
                    serverThread.start();
                    robotOn = true;
                    btnSwitch.setBackgroundResource(R.mipmap.switchoff);
                    tvState.setText("机器人已启动...");
                }else{
                    robotOn = false;
                    closeCamera();
                    try {
                        socket.close();
                        reader.close();
                        serverSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        btnSwitch.setBackgroundResource(R.mipmap.switchon);
                        tvState.setText("机器人未启动...");
                    }
                }
            }
        });
    }

    private Handler socketHandler = new Handler(){
        @Override
        public void handleMessage(android.os.Message msg){
            switch (msg.obj.toString()){
                case("up"):
                    tvState.setText("up...");
                    ivDirection.setBackgroundResource(R.mipmap.up);
                    break;
                case("left"):
                    tvState.setText("left...");
                    ivDirection.setBackgroundResource(R.mipmap.left);
                    break;
                case("right"):
                    tvState.setText("right...");
                    ivDirection.setBackgroundResource(R.mipmap.right);
                    break;
                case("down"):
                    tvState.setText("down...");
                    ivDirection.setBackgroundResource(R.mipmap.down);
                    break;
                case("stop"):
                    tvState.setText("stop...");
                    ivDirection.setBackgroundResource(R.mipmap.transparent);
                    break;
                case("startRecord"):
                    tvState.setText("start record...");
                    startVideo();
                    break;
                case("stopRecord"):
                    tvState.setText("stop record...");
                    stopVideo();
                    break;
                case("photo"):
                    tvState.setText("take photo...");
                    takePhoto();
                    break;
                case("startPreview"):
                    tvState.setText("startPreview...");
                    openCamera();
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewFormat(ImageFormat.JPEG);
                    camera.setPreviewCallback(MainActivity.this);
                    break;
                case("stopPreview"):
                    tvState.setText("stopPreview...");
                    closeCamera();
                    try {
                        writer.writeInt(-1);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void openCamera(){
        try{
            camera = Camera.open();
            camera.setPreviewDisplay(cameraHolder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void closeCamera(){
        try{
            camera.setPreviewCallback(null);
            camera.setOneShotPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startVideo(){
        if(mediarecorder != null || camera != null){
            return;
        }
        openCamera();
        camera.unlock();
        mediarecorder = new MediaRecorder();
        mediarecorder.setCamera(camera);
        mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediarecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediarecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/video.mp4");
        try {
            mediarecorder.prepare();
            mediarecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopVideo(){
        if (mediarecorder == null || camera == null) {
            return;
        }
        mediarecorder.stop();
        mediarecorder.release();
        mediarecorder = null;
        closeCamera();
    }

    public void takePhoto() {
        if(camera != null){
            return;
        }
        openCamera();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.autoFocus(new Camera.AutoFocusCallback(){
            @Override
            public void onAutoFocus(boolean success, Camera camera){
                camera.takePicture(null, null, pictureCallback);
            }
        });
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera){
            String photoFilePath = Environment.getExternalStorageDirectory().getPath() + "/photo.jpg";
            try{
                closeCamera();

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Matrix matrix = new Matrix();
                matrix.setRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                FileOutputStream fOutputStream = new FileOutputStream(new File(photoFilePath));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream);
                fOutputStream.flush();
                fOutputStream.close();

                sendFile(photoFilePath);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public void sendFile(String filePath){
        try {
            File file = new File(filePath);
            writer.writeLong(file.length());
            writer.flush();

            FileInputStream fInputStream = new FileInputStream(filePath);
            byte[] bytes = new byte[MAXBYTESLEN];

            int length = 0;
            while (length != -1) {
                length = fInputStream.read(bytes, 0, bytes.length);
                writer.write(bytes, 0, length);
                writer.flush();
            }
            fInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            //transform the original data to jpeg bytes
            int format = camera.getParameters().getPreviewFormat();
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;
            YuvImage image = new YuvImage(data, format, width, height, null);
            ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 100, outstream);
            byte[] bytes = outstream.toByteArray();
            //transmit the data
            int frameLength = bytes.length;
            writer.writeInt(frameLength);
            writer.flush();
            int index = 0, lenth;
            while(index < frameLength){
                if(MAXBYTESLEN <= frameLength - index)
                    lenth = MAXBYTESLEN;
                else
                    lenth = frameLength - index;
                writer.write(bytes, index, lenth);
                index += lenth;
            }
            writer.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {    }
}


