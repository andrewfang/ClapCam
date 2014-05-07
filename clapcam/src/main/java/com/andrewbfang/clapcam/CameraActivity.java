package com.andrewbfang.clapcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.filters.LowPassSP;

public class CameraActivity extends ActionBarActivity implements Camera.PictureCallback, SurfaceHolder.Callback {

    private Camera camera;
    private boolean previewing;
    private int whichCamera;
    private Boolean cameraConfigured = false;
    private SurfaceHolder previewHolder;
    private CountDownTimer countDown;
    private int surfaceWidth;
    private int surfaceHeight;

    public void capture() {

        if (this.countDown != null) {
            this.countDown.cancel();
        }

        this.countDown = new CountDownTimer(6000, 1000) {
            TextView countDownText = (TextView) findViewById(R.id.countdown);
            public void onTick(long timeLeft) {
                countDownText.setText("" + Math.round((timeLeft-1) / 1000));
            }
            public void onFinish() {
                Log.d("ANDREW", "Taking picture......");
                if (previewing) {
                    countDownText.setText("");
                    CameraActivity.this.camera.takePicture(shutterCallback, null, CameraActivity.this);
                }
            }
        }.start();
    }

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            manager.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }
    };

    @Override
    public void onPictureTaken(byte[] photo, Camera cam) {
        Intent intent = new Intent(this, CaptureResultActivity.class);
        intent.putExtra("PHOTO", photo);
        intent.putExtra("CAMERA", this.whichCamera);
        startActivity(intent);
        finish();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //TODO
        if (this.camera != null) {
            this.camera.stopPreview();
            this.camera.setPreviewCallback(null);
            this.camera.release();
            this.camera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //TODO;
        this.setupPreview(width, height);
        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }


    private void setupPreview(int width, int height) {
        try {
            this.camera.setDisplayOrientation(setCameraDisplayOrientation(this, this.whichCamera));
            this.camera.setPreviewDisplay(this.previewHolder);
        } catch (IOException e) {
            Log.d("ANDREW", "Null Preview display");
            e.printStackTrace();
            System.exit(1);
        }

//        if (!this.cameraConfigured) {
        if (true) {
            Camera.Parameters parameters = this.camera.getParameters();
            Camera.Size size = this.setBestSize(width, height, parameters);
            if (size != null) {
                Log.d("ANDREW", "We chose: " + size.height + ":" + size.width);
                parameters.setPreviewSize(size.width, size.height);
                parameters.setPictureFormat(PixelFormat.JPEG);
                parameters.setPictureSize(size.width, size.height);
                this.camera.setParameters(parameters);
            } else {
                Log.d("ANDREW", "size is null");
            }
        }

        this.camera.startPreview();
        this.previewing = true;
    }

    /**
     * Inspired from http://stackoverflow.com/questions/19577299/android-camera-preview-stretched
     */
    private Camera.Size setBestSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size size = null;
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        final double TOL = .1;
        double targetRatio;
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            targetRatio = (double) height/ width;
        } else {
            targetRatio = (double) width / height;
        }
        double smallestDiff = Double.MAX_VALUE;

        if (parameters.getSupportedPreviewSizes() == null) {
            Log.d("ANDREW", "parameters are null");
            return null;
        }

        for (Camera.Size s : parameters.getSupportedPreviewSizes()) {
            double ratio;
            if (rotation == 90 || rotation == 270) {
                ratio = (double) s.width / s.height;
            } else {
                ratio = (double) s.height/ s.width;
            }
//            if (Math.abs(ratio - targetRatio) > TOL) continue;
            if (Math.abs(ratio - targetRatio) < smallestDiff) {
                size = s;
                smallestDiff = Math.abs(ratio - targetRatio);
            }
        }
        return size;
    }

    public void switchCamera() {
        try {
            this.whichCamera = (this.whichCamera +1) %2;
            this.camera.stopPreview();
            this.camera.release();
            this.camera = Camera.open(this.whichCamera);
            setCameraDisplayOrientation(this, this.whichCamera);
            this.setupPreview(this.surfaceWidth, this.surfaceHeight);
            this.camera.startPreview();

        } catch (Exception e) {
            //no op
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        this.whichCamera =  Camera.getNumberOfCameras() - 1;

        this.previewHolder = ((SurfaceView) findViewById(R.id.camera_preview)).getHolder();
        this.previewHolder.addCallback(this);

        this.toast = Toast.makeText(this, "Tap to start listening..", Toast.LENGTH_LONG);
        this.toast.show();

        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.buffer = new short[bufferSize];
        this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        be.hogent.tarsos.dsp.AudioFormat format = new be.hogent.tarsos.dsp.AudioFormat(SAMPLE_RATE, 16, 1, true, true);
        this.lowPassFilter = new LowPassSP(1000f, SAMPLE_RATE);
        this.audioEvent = new AudioEvent(format, this.buffer.length);
    }

    @Override
    protected void onPause() {
        this.camera.stopPreview();
        this.camera.release();
        this.camera = null;
        this.previewing = false;
        this.recorder.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.camera == null) {
            this.camera = Camera.open(this.whichCamera);
        }
        if (this.recorder == null) {
            this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }
//        if (this.cameraConfigured) {
        this.previewing = true;
        this.setupPreview(this.surfaceWidth, this.surfaceHeight);
        this.camera.startPreview();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case (R.id.action_about):
                /* Shows the about message */
                new AlertDialog.Builder(this)
                        .setTitle(R.string.action_about)
                        .setMessage(R.string.about)
                        .setPositiveButton(R.string.about_confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // okay
                            }
                        })
                        .show();
                break;
            case (R.id.action_switch_camera):
                this.switchCamera();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * To set the camera display orientation.
     * From developer.android.com/references/android/hardware/Camera.html
     */
    public static int setCameraDisplayOrientation(Activity activity, int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    static final int SAMPLE_RATE = 8000;
    private int bufferSize;
    private short[] buffer;
    private AudioRecord recorder;
    private LowPassSP lowPassFilter;
    private boolean recording;
    private AudioEvent audioEvent;
    private boolean heardOneClap;
    private long timeOfLastClap;
    private Toast toast;

    public void listen(View view) {

        Button listenButton = (Button) findViewById(R.id.camera_click);
        listenButton.setClickable(false);
        listenButton.setVisibility(View.INVISIBLE);
        ImageButton cancelButton = (ImageButton) findViewById(R.id.camera_cancel);
        cancelButton.setClickable(true);
        cancelButton.setVisibility(View.VISIBLE);

        if (this.recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }
        this.recorder.startRecording();

        this.toast.setText("Listening...");
        this.toast.setDuration(Toast.LENGTH_LONG);
        this.toast.show();

        this.recording = true;
        Thread recordingThread = new Thread(new Runnable() {
           @Override
           public void run() {
               CameraActivity.this.getAudioData();
           }
        });

        recordingThread.start();
    }

    public void cancel(View view) {
        try {
            this.countDown.cancel();
            TextView countDownText = (TextView) findViewById(R.id.countdown);
            countDownText.setText("");
        } catch (Exception e) {
            //no op
        }

        if (this.recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            this.recorder.stop();
        }

        this.recording = false;
        Log.d("ANDREW", "logging cancelled");

        this.toast.setText("Stopping...");
        this.toast.setDuration(Toast.LENGTH_LONG);
        this.toast.show();

        Button listenButton = (Button) findViewById(R.id.camera_click);
        listenButton.setClickable(true);
        listenButton.setVisibility(View.VISIBLE);

        ImageButton cancelButton = (ImageButton) findViewById(R.id.camera_cancel);
        cancelButton.setClickable(false);
        cancelButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Inspired from http://stackoverflow.com/questions/12660719/android-voice-recording-voice-with-background-noice-issue
     */
    private void getAudioData() {
        int read = 0;
        while (this.recording) {
            read = this.recorder.read(this.buffer, 0, this.bufferSize);
//            Log.d("ANDREW", this.buffer[this.buffer.length-1] + ":" + this.buffer[this.buffer.length -2]);
            processSound();
            this.buffer = new short[bufferSize];
        }
    }

    private void processSound() {
        this.audioEvent.setFloatBufferWithShortBuffer(buffer);
        this.lowPassFilter.process(audioEvent);
        float[] filteredBuffer = audioEvent.getFloatBuffer();
        Arrays.sort(filteredBuffer);

//        Log.d("ANDREW", String.format("%f", filteredBuffer[filteredBuffer.length-1]));

        final double THRESHOLD = .08;
        final long TIME_LOWER_BOUND = 100;
        final long TIME_UPPER_BOUND = 1500;

        if (filteredBuffer[filteredBuffer.length -1] > THRESHOLD) {
            if (this.heardOneClap) {
                if (System.currentTimeMillis() - this.timeOfLastClap < TIME_UPPER_BOUND) {
                    if (System.currentTimeMillis() - this.timeOfLastClap > TIME_LOWER_BOUND) {
                        Log.d("ANDREW", "HIT!");
                        this.runOnUiThread(new Runnable() {
                            public void run() {
                                CameraActivity.this.capture();
                            }
                        });
                        this.heardOneClap = false;
                    }
                } else {
                    this.heardOneClap = false;
                }
            } else {
                this.heardOneClap = true;
                this.timeOfLastClap = System.currentTimeMillis();
            }
        }
    }
}
