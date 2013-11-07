package com.example.sketchpad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
	private static final String FILE_TYPE = ".jpg";
	private static final String ACTION_CAMERA = "android.media.action.IMAGE_CAPTURE";
	private static final int TAKE_PICTURE = 1;
	private DrawingView drawView;
	private ImageButton btnCurrPaint;
	private ImageButton btnDraw;
	private ImageButton btnErase;
	private ShareActionProvider shareActionProvider;
	private Uri imageUri;
	private int cameraId;
	private float smallBrush;
	private float mediumBrush;
	private float largeBrush;
	private boolean hasCamera;
	
	private void setBrushSize(float brushSize) {
		drawView.setErasing(false);
		drawView.setBrushSize(brushSize);
		drawView.setLastBrushSize(brushSize);
	}
	private void setEraserSize(float eraserSize) {
		drawView.setErasing(true);
		drawView.setBrushSize(eraserSize);
	}
	private void newDrawing() {
		AlertDialog.Builder newDrawingDialog = new AlertDialog.Builder(MainActivity.this);
		
		newDrawingDialog.setTitle("New Drawing");
		newDrawingDialog.setMessage("Start new drawing (you will lose the current drawing)?");
		newDrawingDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				drawView.startNewDrawing();
				dialog.dismiss();
			}
		});
		newDrawingDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		newDrawingDialog.show();
	}
	private void saveDrawing() {
		AlertDialog.Builder saveDialog = new AlertDialog.Builder(MainActivity.this);
		
		saveDialog.setTitle("Save Drawing");
		saveDialog.setMessage("Save drawing to device Gallery?");
		saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File image = writeDrawingToDisk(true);
				if (image != null) {
					Toast.makeText(
							getApplicationContext(),
							"Drawing saved to the gallery!",
							Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(
							getApplicationContext(),
							"Oops! Image could not be saved.",
							Toast.LENGTH_SHORT)
							.show();
				}
				
				drawView.destroyDrawingCache();
			}
		});
		saveDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		saveDialog.show();
	}
	private void changeBackground() {
		AlertDialog.Builder changeBg = new AlertDialog.Builder(MainActivity.this);
	
		changeBg.setTitle("Change Drawing Background");
		changeBg.setMessage("Load an image or a static color?");
		changeBg.setPositiveButton("Load Image", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ImageFileDialog imgDialog = new ImageFileDialog(
						MainActivity.this, 
						new File(Environment.getExternalStoragePublicDirectory(
								     Environment.DIRECTORY_PICTURES).getPath()));
				imgDialog.setFileEndsWith(FILE_TYPE);
				imgDialog.addFileListener(new ImageFileDialog.FileSelectedListener() {
					@Override
					public void fileSelected(File file) {
						try {	
							drawView.setBackgroundImage(
									BitmapFactory.decodeFile(file.getPath()));
						} catch (Exception ex) {
							Toast.makeText(
									MainActivity.this,
									"There was an issue importing the selected file.",
									Toast.LENGTH_SHORT)
									.show();
						}
					}	
				});
				imgDialog.showDialog();				
			}
		});
		changeBg.setNegativeButton("Static Color", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				HSVColorPickerDialog.OnColorSelectedListener colorSelectedListener =
						new HSVColorPickerDialog.OnColorSelectedListener() {	
							@Override
							public void colorSelected(Integer color) {
								drawView.setBackgroundColor(color);
							}
						};
				HSVColorPickerDialog colorPicker = new HSVColorPickerDialog(
						MainActivity.this,
						0xFF4488CC,
						colorSelectedListener);
				colorPicker.setTitle("Pick a Color");
				colorPicker.show();
			}
		});
		changeBg.show();
	}
	private void takeImage() {
		Intent cameraIntent = new Intent(ACTION_CAMERA);
		File photo = new File(Environment.getExternalStorageDirectory(), "Pic.jpg");
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		startActivityForResult(cameraIntent, TAKE_PICTURE);
	}
	private void setAsWallpaper() {
		File image = writeDrawingToDisk(false);
		Bitmap bmp = BitmapFactory.decodeFile(image.getPath());
		WallpaperManager wpMgr = WallpaperManager.getInstance(this);
		try {
			wpMgr.setBitmap(bmp);
		} catch (IOException e) {
			Toast.makeText(
					getApplicationContext(),
					"There was an issue setting the image as the wallpaper.",
					Toast.LENGTH_SHORT)
					.show();
		}
	}
	private void putImageIntoGallery(File image) {
		try {
			MediaStore.Images.Media.insertImage(
					getContentResolver(), 
					image.getPath(), 
					"lastest_drawing", 
					"A drawing done in SketchPad!");
		} catch (FileNotFoundException e) {
			Toast.makeText(
					getApplicationContext(),
					"The file could not be added to the gallery.",
					Toast.LENGTH_SHORT)
					.show();
		}
	}
	private int findCamera() {
		int foundId = -1;
		int numCams = Camera.getNumberOfCameras();
		for (int i = 0; i < numCams; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				foundId = i;
				break;
			}
		}
		return foundId;
	}
	private File writeDrawingToDisk(boolean putIntoGallery) {
		drawView.setDrawingCacheEnabled(true);
		File pictureFileDir = new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES),
						"SketchPad");
		if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
			Toast.makeText(
					this,
					"Can't create directory to save the image.",
					Toast.LENGTH_SHORT)
					.show();
			return null;
		}
		
		String filename = pictureFileDir.getPath() + File.separator + "latest_drawing.png";
		File pictureFile = new File(filename);
		Bitmap bitmap = drawView.getDrawingCache();
		try {
			pictureFile.createNewFile();
			FileOutputStream oStream = new FileOutputStream(pictureFile);
			bitmap.compress(CompressFormat.PNG, 100, oStream);
			oStream.flush();
			oStream.close();
		} catch (IOException e) {
			Toast.makeText(
					getApplicationContext(),
					"There was an issue saving the image.",
					Toast.LENGTH_SHORT)
					.show();
		}		
		if (putIntoGallery) {
			putImageIntoGallery(pictureFile);
		}
		
		return pictureFile;
	}
	private Bitmap scaleImage(Bitmap bmp) {
		double lengthToWidthRatio = (((double)bmp.getHeight()) / (double)bmp.getWidth());
		double scaledWidth = ((double)drawView.getHeight()) / lengthToWidthRatio; 
		return Bitmap.createScaledBitmap(bmp, (int)scaledWidth, drawView.getHeight(), false);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			hasCamera = true;
			cameraId = findCamera();
		} else {
			hasCamera = false;
		}
		
		drawView = (DrawingView)findViewById(R.id.drawing);
		btnCurrPaint = (ImageButton)((LinearLayout)findViewById(R.id.paint_colors)).getChildAt(0);
		btnDraw = (ImageButton)findViewById(R.id.btnDraw);
		btnErase = (ImageButton)findViewById(R.id.btnErase);
		smallBrush = getResources().getInteger(R.integer.small_size);
		mediumBrush = getResources().getInteger(R.integer.medium_size);
		largeBrush = getResources().getInteger(R.integer.large_size);
		
		drawView.setBrushSize(mediumBrush);
		btnCurrPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));		
		btnDraw.setOnClickListener(drawClickListener);
		btnErase.setOnClickListener(eraseClickListener);
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
			case TAKE_PICTURE:
				if (resultCode == Activity.RESULT_OK) {
					getContentResolver().notifyChange(imageUri, null);
					try {
						Bitmap bmp = MediaStore.Images.Media.getBitmap(
								getContentResolver(), imageUri);
						drawView.setBackgroundImage(scaleImage(bmp));
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
				newDrawing();
				break;
			case R.id.menu_save:
				saveDrawing();
				break;
			case R.id.menu_change_background:
				changeBackground();
				break;
			case R.id.menu_take_picture:
				takeImage();
				break;
			case R.id.menu_set_wallpaper:
				setAsWallpaper();
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
					setBrushSize(smallBrush);
					brushSizeDialog.dismiss();
				}
			});
			btnMedium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					setBrushSize(mediumBrush);
					brushSizeDialog.dismiss();
				}
			});
			btnLarge.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					setBrushSize(largeBrush);
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
					setEraserSize(smallBrush);
					eraserSizeDialog.dismiss();
				}
			});
			btnMedium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					setEraserSize(mediumBrush);
					eraserSizeDialog.dismiss();
				}
			});
			btnLarge.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					setEraserSize(largeBrush);
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
			File image = writeDrawingToDisk(false);
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
