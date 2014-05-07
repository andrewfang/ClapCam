package com.andrewbfang.clapcam;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureResultActivity extends Activity{

    private Bitmap bitmapImage;
    private DrawView dView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        byte[] photo = intent.getByteArrayExtra("PHOTO");
        int whichCamera = intent.getIntExtra("CAMERA", 1);
        this.bitmapImage = this.processImage(photo, whichCamera);

        this.dView = new DrawView(this.getApplicationContext());
        dView.setImageBitmap(this.bitmapImage);
        RelativeLayout.LayoutParams dViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        dViewParams.setMargins(0, 0, 0, 0);
        dView.setLayoutParams(dViewParams);

        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, 0);
        relativeLayout.setLayoutParams(params);

        relativeLayout.addView(dView);

        setContentView(relativeLayout);
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Intent intent = getIntent();
//        byte[] photo = intent.getByteArrayExtra("PHOTO");
//        int whichCamera = intent.getIntExtra("CAMERA", 1);
//        this.bitmapImage = this.processImage(photo, whichCamera);
//
//        ImageView imView = new ImageView(this.getApplicationContext());
//        imView.setImageBitmap(this.bitmapImage);
//        RelativeLayout.LayoutParams imViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//        imViewParams.setMargins(0, 0, 0, 0);
//        imView.setLayoutParams(imViewParams);
//
//        RelativeLayout relativeLayout = new RelativeLayout(this);
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//        params.setMargins(0, 0, 0, 0);
//        relativeLayout.setLayoutParams(params);
//
//        relativeLayout.addView(imView);
//
//        setContentView(relativeLayout);
//    }

    private Bitmap processImage(byte[] photo, int whichCamera) {
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = 0;
        switch (display.getRotation()){
            case Surface.ROTATION_0:
                rotation = 90 + whichCamera * 180;
                break;
            case Surface.ROTATION_90:
                rotation = 0;
                break;
            case Surface.ROTATION_180:
                rotation = 270;
                break;
            case Surface.ROTATION_270:
                rotation = 180;
                break;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
        Matrix m = new Matrix();
        m.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
//        return bitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case (R.id.action_share):
                this.share();
                break;
            case (R.id.action_save):
                this.save();
                Toast.makeText(this.getApplicationContext(), "File saved", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void share() {
        File file = this.save();
        Uri uri = Uri.fromFile(file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "Share!"));
    }

    public File save() {
        File output_file = this.make_file();
//        Bitmap b = this.bitmapImage;
        BitmapDrawable drawable = (BitmapDrawable) dView.getDrawable();
        Bitmap b = drawable.getBitmap();

        try {
            FileOutputStream output_stream = new FileOutputStream(output_file);
            b.compress(Bitmap.CompressFormat.PNG, 90, output_stream);
            output_stream.close();
        } catch (FileNotFoundException e) {
            Log.d("ANDREW", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("ANDREW", "Error accessing file: " + e.getMessage());
        } catch (Exception e) {
            Log.d("ANDREW", "Other:" + e.getMessage());
        }
        return output_file;

    }

    /**
     * Inspired from http://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage
     */
    private File make_file() {
        File file_directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/ClapCam/");
        if (!file_directory.exists()) {
            if(!file_directory.mkdirs()) {
                Log.d("ANDREW", "Error occured: File is null.");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        File output_file;
        String output_file_name="ClapCam_"+ timeStamp +".jpg";
        output_file = new File(file_directory.getPath() + File.separator + output_file_name);
        return output_file;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }
}
