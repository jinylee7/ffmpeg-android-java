package com.jakincorp.commentcon;

import android.Manifest;
import android.Manifest.permission;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int RQST_CODE_CHOOSE_VIDEO = 7001;

    private FFmpeg ffmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        this.requestPermission();
        this.loadFFMpegBinary();
        try {
            Log.v("DeviceVersion", ffmpeg.getDeviceFFmpegVersion());
            Log.v("LibraryVersion", ffmpeg.getLibraryFFmpegVersion());
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }

        File movieDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        Log.d(MainActivity.class.getSimpleName(), movieDir.getAbsolutePath());


    }

    @OnClick(R.id.btnLoadVideo)
    public void onBtnLoadVideoClicked() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        super.startActivityForResult(intent, RQST_CODE_CHOOSE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == RQST_CODE_CHOOSE_VIDEO) {
                Uri uri = intent.getData();
                String originalFileAbsolutePath = getRealPathFromURI(uri);
                File overlayImg = Util.copyBinaryFromAssetsToData(this, "overlay-1.png", "overlay-1.png");
                Log.v(MainActivity.class.getSimpleName(), "Original AbsolutePath" + originalFileAbsolutePath + ", OverlayImage : " + overlayImg.getAbsolutePath());

                File movieDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File outputFile = new File(movieDir, System.currentTimeMillis() + ".mp4");


                String command = "-y -i " + originalFileAbsolutePath + " -i " + overlayImg.getAbsolutePath() + " "
//                        + " -filter_complex [0:v][1:v] overlay=0:0:enable='between(t,0,5)' "
//                        + " -pix_fmt yuv420p -c:a copy "
                        + outputFile.getAbsolutePath();

                String cmd = String.format("-i %s -i %s -filter_complex overlay=0:0:enable='between(t,0.0,5.0) -c:v libx264 -preset ultrafast %s",
                        originalFileAbsolutePath, overlayImg.getAbsolutePath(), outputFile.getAbsolutePath());


                this.execFFmpegBinary(cmd.split(" "));

            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11);
        }
    }

    private void loadFFMpegBinary() {
        try {
            ffmpeg = FFmpeg.getInstance(this);
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("device_not_supported")
                .setMessage("device_not_supported_message")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.e(MainActivity.class.getSimpleName(), "FAILED with output : "+s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.e(MainActivity.class.getSimpleName(), "SUCCESS with output : "+s);
                }

                @Override
                public void onProgress(String s) {
                    Log.e(MainActivity.class.getSimpleName(), "progress : "+s);
                }

                @Override
                public void onStart() {
                    Log.e(MainActivity.class.getSimpleName(), "Started command : ffmpeg ");
                }

                @Override
                public void onFinish() {
                    Log.e(MainActivity.class.getSimpleName(), "Finished command : ffmpeg ");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
            e.printStackTrace();
        }
    }

}
