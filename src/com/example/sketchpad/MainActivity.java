package com.example.sketchpad;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final String LAST_BRUSH_WEIGHT = "last_brush_weight";
	private final String LAST_ERASER_WEIGHT = "last_eraser_weight";
	private final int minBrushEraserSize = 1;
	private Drawing drawing;
	private DrawingView drawView;
	private CameraManager camMan;
	private ImageButton btnCurrPaint;
	private ImageButton btnDraw;
	private ImageButton btnErase;
	private ShareActionProvider shareActionProvider;
	private SharedPreferences sharedPrefs;
	private int maxBrushEraserSize;
	private boolean hasCamera;
	private enum Utensil { Brush, Eraser };
	
	private void adjustBrushAndEraserSize(int currentSize, final Dialog dialog, final Utensil utensil) {
		dialog.setContentView(R.layout.brush_chooser);
		final SeekBar barBrushWeight = (SeekBar)dialog.findViewById(
				R.id.barBrushWeight);
		Button btnAccept = (Button)dialog.findViewById(
				R.id.btnAccept);
		Button btnClose = (Button)dialog.findViewById(
				R.id.btnClose);
		final ImageView brushSizeIndicator = (ImageView)dialog.findViewById(
				R.id.imgBrushWeightDisplay);
		final TextView txtPixelSize = (TextView)dialog.findViewById(
				R.id.txtPixels);
		
		maxBrushEraserSize = barBrushWeight.getMax();
		barBrushWeight.setProgress(currentSize);
		brushSizeIndicator.getLayoutParams().width = currentSize;
		brushSizeIndicator.getLayoutParams().height = currentSize;
		txtPixelSize.setText(currentSize + "px");
		
		barBrushWeight.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress <= maxBrushEraserSize && progress >= minBrushEraserSize) {
					txtPixelSize.setText(progress + "px");
					brushSizeIndicator.getLayoutParams().height = progress;
					brushSizeIndicator.getLayoutParams().width = progress;	
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});	
		btnAccept.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				float weight = Float.parseFloat(Integer.toString(barBrushWeight.getProgress()));
				if (utensil == Utensil.Brush) {
					drawing.setBrushSize(weight);
					sharedPrefs.edit()
					           .putFloat(LAST_BRUSH_WEIGHT, weight)
					           .apply();	
				} else {
					drawing.setEraserSize(weight);
					sharedPrefs.edit()
					           .putFloat(LAST_ERASER_WEIGHT, weight)
					           .apply();
				}
				dialog.dismiss();
			}
		});
		btnClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		drawView = (DrawingView)findViewById(R.id.drawing);
		camMan = new CameraManager(this);
		drawing = new Drawing(this, drawView);
		btnCurrPaint = (ImageButton)((LinearLayout)findViewById(R.id.paint_colors)).getChildAt(0);
		btnDraw = (ImageButton)findViewById(R.id.btnDraw);
		btnErase = (ImageButton)findViewById(R.id.btnErase);
		
		drawing.setEraserSize(sharedPrefs.getFloat(LAST_ERASER_WEIGHT, 12));
		drawing.setBrushSize(sharedPrefs.getFloat(LAST_BRUSH_WEIGHT, 12));
		drawView.setColor(btnCurrPaint.getTag().toString());
		btnCurrPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));		
		btnDraw.setOnClickListener(drawClickListener);
		btnErase.setOnClickListener(eraseClickListener);
		
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			hasCamera = true;
		} else {
			hasCamera = false;
		}
	}
	public void paintClicked(View view) {
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
			int brushSize = (int)drawing.getBrushSize();
			final Dialog brushSizeDialog = new Dialog(MainActivity.this);
			brushSizeDialog.setTitle("Brush Size: ");
			adjustBrushAndEraserSize(brushSize, brushSizeDialog, Utensil.Brush);
		}
	};	
	private OnClickListener eraseClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			int eraserSize = (int)drawing.getEraserSize();
			final Dialog eraserSizeDialog = new Dialog(MainActivity.this);	
			eraserSizeDialog.setTitle("Eraser Size: " );			
			adjustBrushAndEraserSize(eraserSize, eraserSizeDialog, Utensil.Eraser);
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
