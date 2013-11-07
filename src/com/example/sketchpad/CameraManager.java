package com.example.sketchpad;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

public class CameraManager {
	public static final int TAKE_PICTURE = 1;
	private static final String ACTION_CAMERA = "android.media.action.IMAGE_CAPTURE";
	private Context context;
	private Uri imageUri;
	private int cameraId;
	
	public CameraManager(Context context) {
		this.context = context;
	}
	
	public void takeImage() {
		Intent cameraIntent = new Intent(ACTION_CAMERA);
		File photo = new File(Environment.getExternalStorageDirectory(), "Pic.jpg");
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		((Activity)context).startActivityForResult(cameraIntent, TAKE_PICTURE);
	}
	public void findCamera() {
		cameraId = -1;
		int numCams = Camera.getNumberOfCameras();
		for (int i = 0; i < numCams; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				break;
			}
		}
	}
	
	public Uri getImageUri() {
		return imageUri;
	}
}
