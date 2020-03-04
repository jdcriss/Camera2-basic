package com.example.jdc.cam8;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;

import com.example.jdc.cam8.utils.CheckPermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "testtt";
    private String CameraId = "0";
    private Size previewSize;
    private Size mCaptureSize;
    private TextureView mTextureView;
    private CameraDevice mDevice;
    private ImageReader mImageReader;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private Surface surface;

    private Button button_capture,button_album,button_distance,button_exposure,button_gain;
    private SeekBar seekBar_distance,seekBar_exposure,seekBar_gain;
    private int FLAG_DISTANCE=0,FLAG_EXPOSURE=0,FLAG_GAIN=0;
    private int SELECT_PHOTO = 1;

    private float distance;

    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide(); //before AppCompat.V7 use "getActionBar()".
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        initPermission();
        initUI();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        openCamera();
    }

    private void initPermission() {
        String[] permissions = CheckPermissionUtils.checkPermission(this);
        if (permissions.length == 0) {

        } else {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    private void initUI() {
        mTextureView = findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        seekBar_gain = findViewById(R.id.seekBar_gain);
        seekBar_exposure = findViewById(R.id.seekBar_exposure);
        seekBar_distance = findViewById(R.id.seekBar_distance);
        seekBar_distance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distance = (float)progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    mCameraCaptureSession.stopRepeating();
//                    mBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, distance);
                    Log.d(TAG, "onStopTrackingTouch: "+distance);
                    mDevice.createCaptureSession(Arrays.asList(surface, setupImageReader()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureRequest = mBuilder.build();
                            mCameraCaptureSession = session;
                            try {
                                mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, cameraHandler());
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, cameraHandler());

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        button_album = findViewById(R.id.button_album);
        button_album.setOnClickListener(clickListener);
        button_capture = findViewById(R.id.button_capture);
        button_capture.setOnClickListener(clickListener);
        button_distance = findViewById(R.id.button_distance);
        button_distance.setOnClickListener(clickListener);
        button_exposure = findViewById(R.id.button_exposure);
        button_exposure.setOnClickListener(clickListener);
        button_gain =findViewById(R.id.button_gain);
        button_gain.setOnClickListener(clickListener);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id){
                case R.id.button_capture:
                    lockFocus();
                    break;
                case R.id.button_distance:
                    if(FLAG_DISTANCE==1){
                        seekBar_distance.setVisibility(View.INVISIBLE);
                        FLAG_DISTANCE=0;
                    }else{
                        seekBar_distance.setVisibility(View.VISIBLE);
                        FLAG_DISTANCE=1;
                        
                    }
                    break;
                case R.id.button_exposure:
                    if(FLAG_EXPOSURE==1){
                        seekBar_exposure.setVisibility(View.INVISIBLE);
                        FLAG_EXPOSURE=0;
                    }else{
                        seekBar_exposure.setVisibility(View.VISIBLE);
                        FLAG_EXPOSURE=1;
//                        FLAG_GAIN=0;
//                        FLAG_DISTANCE=0;
                    }
                    break;
                case R.id.button_gain:
                    if(FLAG_GAIN==1){
                        seekBar_gain.setVisibility(View.INVISIBLE);
                        FLAG_GAIN=0;
                    }else{
                        seekBar_gain.setVisibility(View.VISIBLE);
                        FLAG_GAIN=1;
                    }
                    break;
                case R.id.button_album:
                    Intent intent = new Intent();
                    intent.setType("image/*");// 开启Pictures画面Type设定为image
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, SELECT_PHOTO);
                    break;
            }
        }
    };

    public void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(CameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), mTextureView.getWidth(), mTextureView.getHeight());
            mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(CameraId, mCameraCallback, cameraHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mDevice = null;
        }
    };

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        surface = new Surface(surfaceTexture);
        try {
            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mBuilder.addTarget(surface);
            mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            mBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
//            mBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            mBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            Log.d(TAG, "startPreview: 111111");
            mDevice.createCaptureSession(Arrays.asList(surface, setupImageReader()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureRequest = mBuilder.build();
                    mCameraCaptureSession = session;
                    try {
                        Log.d(TAG, "onConfigured: 22222");
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, cameraHandler());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, cameraHandler());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void lockFocus() {
        try {
            mBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.capture(mCaptureRequest, mCaptureCallback, cameraHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void capture() {
        try {
            CaptureRequest.Builder mCaptureBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
//            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
//            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    unlockFocus();
                }
            };
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, cameraHandler());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            mBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, cameraHandler());
        } catch (CameraAccessException e) {

        }
    }

    public void closeCamera() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mDevice != null) {
            mDevice.close();
            mDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            capture();
        }
    };

    private Surface setupImageReader() {
        mImageReader = ImageReader.newInstance(3264, 2448, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                cameraHandler().post(new imageSaver(mImageReader.acquireNextImage()));
            }
        }, null);

        return mImageReader.getSurface();
    }

    private Handler cameraHandler() {
        Handler mCameraHandler;
        HandlerThread mCameraThread;
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        Log.d(TAG, "cameraHandler: 3333");
        return mCameraHandler;
    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    public static class imageSaver implements Runnable { //创建照片保存的线程
        private Image mImage;
        public Uri uri;

        public imageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String path = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
            File mImageFile = new File(path);
            if (!mImageFile.exists()) {
                mImageFile.mkdir();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = path + "IMG_" + timeStamp + ".jpg";
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(fileName);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mImage.close(); //must close.
            }
            uri = Uri.fromFile(new File(fileName));

        }
    }
}


