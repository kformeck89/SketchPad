package com.example.sketchpad;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Drawing drawing;
	private DrawingView drawView;
	private CameraManager camMan;
	private ImageButton btnCurrPaint;
	private ImageButton btnDraw;
	private ImageButton btnErase;
	private ShareActionProvider shareActionProvider;
	private float smallBrush;
	private float mediumBrush;
	private float largeBrush;
	private boolean hasCamera;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		drawView = (DrawingView)findViewById(R.id.drawing);
		camMan = new CameraManager(this);
		drawing = new Drawing(this, drawView);
		btnCurrPaint = (ImageButton)((LinearLayout)findViewById(R.id.paint_colors)).getChildAt(0);
		btnDraw = (ImageButton)findViewById(R.id.btnDraw);
		btnErase = (ImageButton)findViewById(R.id.btnErase);
		smallBrush = getResources().getInteger(R.integer.small_size);
		mediumBrush = getResources().getInteger(R.integer.medium_size);
		largeBrush = getResources().getInteger(R.integer.large_size);
		
		drawView.setBrushSize(mediumBrush);
		drawView.setColor(btnCurrPaint.getTag().toString());
		btnCurrPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));		
		btnDraw.setOnClickListener(drawClickListener);
		btnErase.setOnClickListener(eraseClickListener);
		
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			hasCamera = true;
			//cameraId =  camMan.findCamera();
		} else {
			hasCamera = false;
		}
	}
	public void paintClicked(View view) {
		drawView.setErasing(false);
		if (view != btnCurrPaint) {
			ImageButton imgView = (ImageButton)view;
			String color = imgView.getTag().toString();
			
			drawView.setColor(color);
			drawView.setBrushSize(drawView.getLastBrushSize());
			
			imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
			btnCurrPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
			btnCurrPaint = (ImageButton)view;
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case CameraManager.TAKE_PICTURE:
				if (resultCode == Activity.RESULT_OK) {
					getContentResolver().notifyChange(camMan.getImageUri(), null);
					try {
						Bitmap bmp = MediaStore.Images.Media.getBitmap(
								getContentResolver(), camMan.getImageUri());
						drawView.setBackgroundImage(drawing.scaleImage(bmp));
						bmp = null;
					} catch (Exception ex) {
						Toast.makeText(
								this,
								"Failed to load the image.",
								Toast.LENGTH_SHORT)
								.show();
					}
				}
		}
	}
	
	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		shareActionProvider = (ShareActionProvider)menu.findItem(R.id.menu_share).getActionProvider();
		shareActionProvider.setShareIntent(new Intent(Intent.ACTION_SEND).setType("image/png"));
		shareActionProvider.setOnShareTargetSelectedListener(shareClickListener);
		return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_take_picture).setEnabled(hasCamera);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_new:
				drawing.newDrawing();
				break;
			case R.id.menu_save:
				drawing.saveDrawing();
				break;
			case R.id.menu_change_background:
				drawing.changeBackground();
				break;
			case R.id.menu_take_picture:
				camMan.takeImage();
				break;
			case R.id.menu_set_wallpaper:
				drawing.setAsWallpaper();
				break;
		}
		return true;
	}
	
	private OnClickListener drawClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {			
			final Dialog brushSizeDialog = new Dialog(MainActivity.this);
			brushSizeDialog.setTitle("Brush Size:");
			brushSizeDialog.setContentView(R.layout.brush_chooser);
			
			ImageButton btnSmall = (ImageButton)brushSizeDialog.findViewById(R.id.small_brush);
			ImageButton btnMedium = (ImageButton)brushSizeDialog.findViewById(R.id.medium_brush);
			ImageButton btnLarge = (ImageButton)brushSizeDialog.findViewById(R.id.large_brush);
			
			btnSmall.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setBrushSize(smallBrush);
					brushSizeDialog.dismiss();
				}
			});
			btnMedium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setBrushSize(mediumBrush);
					brushSizeDialog.dismiss();
				}
			});
			btnLarge.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setBrushSize(largeBrush);
					brushSizeDialog.dismiss();
				}
			});
			
			brushSizeDialog.show();
		}
	};
	private OnClickListener eraseClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			final Dialog eraserSizeDialog = new Dialog(MainActivity.this);
			eraserSizeDialog.setTitle("Eraser Size: ");
			eraserSizeDialog.setContentView(R.layout.brush_chooser);
			
			ImageButton btnSmall = (ImageButton)eraserSizeDialog.findViewById(R.id.small_brush);
			ImageButton btnMedium = (ImageButton)eraserSizeDialog.findViewById(R.id.medium_brush);
			ImageButton btnLarge = (ImageButton)eraserSizeDialog.findViewById(R.id.large_brush);
			
			btnSmall.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setEraserSize(smallBrush);
					eraserSizeDialog.dismiss();
				}
			});
			btnMedium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setEraserSize(mediumBrush);
					eraserSizeDialog.dismiss();
				}
			});
			btnLarge.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					drawing.setEraserSize(largeBrush);
					eraserSizeDialog.dismiss();
				}
			});
			
			eraserSizeDialog.show();
		}
	};
	private OnShareTargetSelectedListener shareClickListener = new OnShareTargetSelectedListener() {
		@Override
		public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
			Intent sendImageIntent = new Intent(Intent.ACTION_SEND).setType("image/png");
			File image = drawing.writeDrawingToDisk(false);
			try {
			    if (image != null) {
			    	sendImageIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(image.getPath()));	
			    }
				shareActionProvider.setShareIntent(sendImageIntent);	
			} catch (Exception ex) {
				Toast.makeText(
						getApplicationContext(),
						"There was an issue sharing this image.",
						Toast.LENGTH_SHORT)
						.show();
			} finally {
				image.delete();
			}
			
			return false;
		}		
	};
}
