package com.paintapp.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.asksira.bsimagepicker.BSImagePicker;
import com.asksira.bsimagepicker.Utils;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

public class PaintActivity extends AppCompatActivity implements View.OnClickListener, BSImagePicker.OnSingleImageSelectedListener {

    private int PERMISSION_REQUEST_CODE = 1;

    private DrawingView drawView;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn, colorBtn, backgroundBtn;
    AlertDialog.Builder newDialog, saveDialog;
    ColorPickerDialog cpg;
    SeekBar sizebar;
    TextView maxtext;
    SharedPreferences pre;
    SharedPreferences.Editor editor;
    AlertDialog.Builder builder;
    LayoutInflater inflater;
    View bgview;
    AlertDialog bgdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        drawView = (DrawingView) findViewById(R.id.drawing);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        eraseBtn = (ImageButton) findViewById(R.id.erase_btn);
        newBtn = (ImageButton) findViewById(R.id.new_btn);
        saveBtn = (ImageButton) findViewById(R.id.save_btn);
        colorBtn = (ImageButton) findViewById(R.id.color_btn);
        backgroundBtn = (ImageButton) findViewById(R.id.background_btn);
        newDialog = new AlertDialog.Builder(this);
        saveDialog = new AlertDialog.Builder(this);
        sizebar = (SeekBar) findViewById(R.id.sizebar);
        maxtext = (TextView) findViewById(R.id.maxtext);
        pre = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pre.edit();
        builder = new AlertDialog.Builder(this);
        inflater = getLayoutInflater();
        bgview = inflater.inflate(R.layout.background_dialog, null);
        builder.setView(bgview)
                .setTitle("Set Background");
        bgdialog = builder.create();
        //Bitmap btm = BitmapFactory.decodeResource(getResources(), R.drawable.eraser);
        //Drawable drb = new BitmapDrawable(getResources(), btm);
        //drawView.setBackground(drb);

        //Bitmap btm = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        //Drawable drb = new BitmapDrawable(getResources(), btm);
        //drawView.setBackgroundResource(R.drawable.img);

        checkSharedPreferences();

        drawBtn.setOnClickListener(this);
        eraseBtn.setOnClickListener(this);
        newBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        colorBtn.setOnClickListener(this);
        backgroundBtn.setOnClickListener(this);
        sizebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxtext.setText(Integer.toString(sizebar.getProgress() + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editor.putInt("mMySeekBarProgress", sizebar.getProgress()).apply();
                drawView.setBrushSize(sizebar.getProgress() + 1);
            }
        });
    }

    public void cbgClicked(View view){
        cpg = new ColorPickerDialog(PaintActivity.this, Color.parseColor("#ff6600"));
        cpg.setAlphaSliderVisible(true);
        cpg.setHexValueEnabled(true);
        cpg.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {
                drawView.setBackgroundColor(i);
            }
        });
        cpg.show();
        bgdialog.dismiss();
    }

    public void cmbgClicked(View view) {
        BSImagePicker pickerDialog = new BSImagePicker.Builder("com.asksira.imagepickersheetdemo.fileprovider")
                .hideCameraTile()
                .build();
        pickerDialog.show(getSupportFragmentManager(), "picker");
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.draw_btn){
                    drawView.setBrushSize(sizebar.getProgress() + 1);
                    drawView.setErase(false);
        }
        else if(view.getId()==R.id.erase_btn){
                    drawView.setErase(true);
                    drawView.setBrushSize(sizebar.getProgress() + 1);
        }
        else if(view.getId() == R.id.new_btn){
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing? Current drawing will be lost!");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                    drawView.startNew();
                    drawView.setBackgroundColor(Color.WHITE);
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("No", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();
        }
        else if(view.getId() == R.id.save_btn){
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                   if( Build.VERSION.SDK_INT >= 23 && !checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        requestStoragePermission();
                   }
                   drawView.setDrawingCacheEnabled(true);
                   Bitmap source = drawView.getDrawingCache();
                   saveImage(source);
                   Toast.makeText(PaintActivity.this, "Image saved!", Toast.LENGTH_SHORT).show();// and if not saved?
                   drawView.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton("No", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();
        }
        else if(view.getId() == R.id.color_btn){
            String color = pre.getString("mycolor", "#ff6600");
            cpg = new ColorPickerDialog(PaintActivity.this, Color.parseColor(color));
            drawView.setColor(color);
            cpg.setAlphaSliderVisible(true);
            cpg.setHexValueEnabled(true);
            cpg.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                @Override
                public void onColorChanged(int i) {
                    String newcolor = "#" + Integer.toHexString(i);
                    drawView.setColor(newcolor);
                    editor.putString("mycolor", newcolor).apply();
                }
            });

            cpg.show();
        }
        else if(view.getId() == R.id.background_btn){
            bgdialog.show();
        }
    }

    private boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            new AlertDialog.Builder(this).setTitle("Permission needed")
                    .setMessage("Allow the app to save images to gallery")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(PaintActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void saveImage(Bitmap finalBitmap){
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/PaintApp");
        myDir.mkdirs();
        String fname = "Image-" + UUID.randomUUID().toString() + ".jpg";// better save with date;
        File file = new File(myDir, fname);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + myDir + "/" + fname)));
        if(file.exists()) file.delete(); // it looks strange, make it better;
        try{
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkSharedPreferences(){
        int progress = pre.getInt("mMySeekBarProgress", 9);
        String color = pre.getString("mycolor", "#ff6600");

        sizebar.setProgress(progress);
        maxtext.setText(Integer.toString(progress + 1));
        drawView.setBrushSize(sizebar.getProgress() + 1);
        drawView.setColor(color);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSingleImageSelected(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawView.setBackground(drawable);
            bgdialog.dismiss();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
