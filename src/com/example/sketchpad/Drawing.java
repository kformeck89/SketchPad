package com.example.sketchpad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

public class Drawing {
	private static final String FILE_TYPE = ".jpg";
	private Context context;
	private DrawingView drawView;
	
	public Drawing(Context context, DrawingView drawView) {
		this.context = context;
		this.drawView = drawView;
	}
	
	private void putImageIntoGallery(File image) {
		try {
			MediaStore.Images.Media.insertImage(
					context.getContentResolver(), 
					image.getPath(), 
					"lastest_drawing", 
					"A drawing done in SketchPad!");
		} catch (FileNotFoundException e) {
			Toast.makeText(
					context,
					"The file could not be added to the gallery.",
					Toast.LENGTH_SHORT)
					.show();
		}
	}
	public void newDrawing() {
		AlertDialog.Builder newDrawingDialog = new AlertDialog.Builder(context);
		
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
	public void saveDrawing() {
		AlertDialog.Builder saveDialog = new AlertDialog.Builder(context);
		
		saveDialog.setTitle("Save Drawing");
		saveDialog.setMessage("Save drawing to device Gallery?");
		saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File image = writeDrawingToDisk(true);
				if (image != null) {
					Toast.makeText(
							context,
							"Drawing saved to the gallery!",
							Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(
							context,
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
	public void changeBackground() {
		AlertDialog.Builder changeBg = new AlertDialog.Builder(context);
	
		changeBg.setTitle("Change Drawing Background");
		changeBg.setMessage("Load an image or a static color?");
		changeBg.setPositiveButton("Load Image", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ImageFileDialog imgDialog = new ImageFileDialog(
						(Activity) context, 
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
									context,
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
						context,
						0xFF4488CC,
						colorSelectedListener);
				colorPicker.setTitle("Pick a Color");
				colorPicker.show();
			}
		});
		changeBg.show();
	}
	public void setAsWallpaper() {
		File image = writeDrawingToDisk(false);
		Bitmap bmp = BitmapFactory.decodeFile(image.getPath());
		WallpaperManager wpMgr = WallpaperManager.getInstance(context);
		try {
			wpMgr.setBitmap(bmp);
		} catch (IOException e) {
			Toast.makeText(
					context,
					"There was an issue setting the image as the wallpaper.",
					Toast.LENGTH_SHORT)
					.show();
		}
	}
	public File writeDrawingToDisk(boolean putIntoGallery) {
		drawView.setDrawingCacheEnabled(true);
		File pictureFileDir = new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES),
						"SketchPad");
		if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
			Toast.makeText(
					context,
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
					context,
					"There was an issue saving the image.",
					Toast.LENGTH_SHORT)
					.show();
		}		
		if (putIntoGallery) {
			putImageIntoGallery(pictureFile);
		}
		
		return pictureFile;
	}
	public Bitmap scaleImage(Bitmap bmp) {
		double lengthToWidthRatio = (((double)bmp.getHeight()) / (double)bmp.getWidth());
		double scaledWidth = ((double)drawView.getHeight()) / lengthToWidthRatio; 
		return Bitmap.createScaledBitmap(bmp, (int)scaledWidth, drawView.getHeight(), false);
	}

	public float getBrushSize() {
		return drawView.getBrushSize();
	}
	public void setBrushSize(float brushSize) {
		drawView.setBrushSize(brushSize);
		drawView.setLastBrushSize(brushSize);
	}
	public float getEraserSize() {
		return drawView.getEraserSize();
	}
	public void setEraserSize(float eraserSize) {
		drawView.setEraserSize(eraserSize);
	}
}
