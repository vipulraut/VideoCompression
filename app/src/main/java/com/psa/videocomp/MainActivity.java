package com.psa.videocomp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;


import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.net.URISyntaxException;

public class MainActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE_VID = 2;
    private static final int RESQUEST_TAKE_VIDEO = 200;
    private static final int TYPE_VIDEO = 2;


    Uri capturedUri = null;
    Uri compressUri = null;

    TextView picDescription;
    private ImageView videoImageView;
    LinearLayout compressionMsg;
    VideoView simpleVideoView;
    MediaController mediaController ;
    static String originalFileInfo ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoImageView = (ImageView)findViewById(R.id.videoImageView);
        compressionMsg = (LinearLayout) findViewById(R.id.compressionMsg);
        picDescription = (TextView) findViewById(R.id.pic_description);

        videoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions(TYPE_VIDEO);
            }
        });

        simpleVideoView = (VideoView) findViewById(R.id.simpleVideoView);
        mediaController = new MediaController(this);

    }

    /**
     * Request Permission for writing to External Storage in 6.0 and up
     */
    private void requestPermissions(int mediaType){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE_VID);
       }
        else{
            dispatchTakeVideoIntent();
      }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            try {
                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
                takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(takeVideoIntent, RESQUEST_TAKE_VIDEO);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

      if (requestCode == RESQUEST_TAKE_VIDEO && resultCode == RESULT_OK){
            if (data.getData() != null) {
                //create destination directory
                File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName() + "/media/videos");
                if (f.mkdirs() || f.isDirectory()){
                    Uri contactUri = data.getData();
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(this,contactUri);
                    String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = this.getContentResolver().query(contactUri, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    File originalFile = new File(filePath);

                    float length = originalFile.length() / 1024f; // Size in KB
                    String value;
                    if(length >= 1024)
                        value = length/1024f+" MB";
                    else
                        value = length+" KB";
                    String res;
                    int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                    if(rotation==90 || rotation==180){
                        res = height + "x" + width;
                    }else{
                        res = width + "x" + height;
                    }
                    originalFileInfo = "\n Resolution: "+res + "\n Size:"+ value+"\n ";
                    new VideoCompressAsyncTask(this).execute(data.getData().toString(), f.getPath());
                }
                //compress and output new video specs

            }
        }

    }

    class VideoCompressAsyncTask extends AsyncTask<String, String, String> {

        Context mContext;

        public VideoCompressAsyncTask(Context context){
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            compressionMsg.setVisibility(View.VISIBLE);
            picDescription.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... paths) {
            String filePath = null;
            try {

                filePath = SiliCompressor.with(mContext).compressVideo(Uri.parse(paths[0]), paths[1],1280,720,700000);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return  filePath;

        }


        @Override
        protected void onPostExecute(String compressedFilePath) {
            super.onPostExecute(compressedFilePath);
            File imageFile = new File(compressedFilePath);
            float length = imageFile.length() / 1024f; // Size in KB
            String value;
            if(length >= 1024)
                value = length/1024f+" MB";
            else
                value = length+" KB";
            //   String text = String.format(Locale.US, "%s\nName: %s\nSize: %s", getString(R.string.video_compression_complete), imageFile.getName(), value);
            // String text = "Compressed File: "value;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            // Uri uri = Uri.parse(compressedFilePath);
            retriever.setDataSource(compressedFilePath);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            float duration = Float.parseFloat(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000;

            String res;
            int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            if(rotation==90 || rotation==180){
                res = height + "x" + width;
            }else{
                res = width + "x" + height;
            }


            String finalInfo = "Duration :"+duration +" Sec" +"\nOriginal File: "+ originalFileInfo+"" +
                    "\nCompressed File: "+"\n Resolution :" +res+"\n Size :"+value;
            compressionMsg.setVisibility(View.GONE);
            picDescription.setVisibility(View.VISIBLE);
            picDescription.setText(finalInfo);


            simpleVideoView.setVisibility(View.VISIBLE);
            simpleVideoView.setVideoURI(Uri.parse(compressedFilePath));
            simpleVideoView.setMediaController(mediaController);
            simpleVideoView.start();
            Log.i("Silicompressor", "Path: "+compressedFilePath);
        }
    }

}
