package com.example.sketchpad;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
	private Context context;
	private Path drawPath;
	private Paint drawPaint;
	private Paint canvasPaint;
	private Canvas drawCanvas;
	private Bitmap canvasBitmap;
	private int previousPaintColor;
	private int paintColor;
	private float brushSize;
	private float eraserSize;
	private float lastBrushSize;
	private boolean isErasing = false;
	private boolean isImageLoaded = false;
	private List<PaintPathPair> undoList = null;
	private List<PaintPathPair> currentMoveList = null;
	private List<PaintPathPair> moveList = null;
	
	public DrawingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.moveList = new ArrayList<PaintPathPair>();
		this.undoList = new ArrayList<PaintPathPair>();
		this.currentMoveList = new ArrayList<PaintPathPair>();
		this.canvasPaint = new Paint(Paint.DITHER_FLAG);
		setupDrawing();
	}
	
	private void clearBrushes() {
		moveList.clear();
		undoList.clear();
		currentMoveList.clear();
	}
	private void setupDrawing() {
		drawPath = new Path();
		drawPaint = new Paint();
		
		brushSize = getResources().getInteger(R.integer.default_brush_size);
		lastBrushSize = brushSize;
		
		drawPaint.setColor(paintColor);
		drawPaint.setAntiAlias(true);
		drawPaint.setStrokeWidth(brushSize);
		drawPaint.setStyle(Paint.Style.STROKE);
		drawPaint.setStrokeJoin(Paint.Join.ROUND);
		drawPaint.setStrokeCap(Paint.Cap.ROUND);
	}
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		drawCanvas = new Canvas(canvasBitmap);
	}
	@Override
	protected void onDraw(Canvas canvas) {
		if (isImageLoaded) {
			canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);	
		}
		drawPaint.setColor(paintColor);
		for (PaintPathPair pair : currentMoveList) {
			canvas.drawPath(pair.getPath(), pair.getPaint());
		}
		for (PaintPathPair pair : moveList) {
			canvas.drawPath(pair.getPath(), pair.getPaint());	
		}
	}
	public void startNewDrawing() {
		setBackgroundColor(getResources().getColor(R.color.white));
		drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
		clearBrushes();
		isImageLoaded = false;
		invalidate();
	}
	public void undo() {
		if (moveList.size() > 0) {
			undoList.add(moveList.remove(moveList.size() - 1));
			invalidate();	
		}
	}
	public void redo() {
		if (undoList.size() > 0) {
			moveList.add(undoList.remove(undoList.size() - 1));
			invalidate();
		}
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float touchX = event.getX();
		float touchY = event.getY();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				drawPath.moveTo(touchX, touchY);
				break;
			case MotionEvent.ACTION_MOVE:
				drawPath.lineTo(touchX, touchY);
				currentMoveList.add(new PaintPathPair(drawPaint, drawPath));
				break;
			case MotionEvent.ACTION_UP:
				drawPath.lineTo(touchX, touchY);
				drawCanvas.drawPath(drawPath, drawPaint);
				moveList.add(new PaintPathPair(new Paint(drawPaint), drawPath));
				drawPath = new Path();
				currentMoveList.clear();
				break;
			default:
				return false;
		}
		invalidate();
		return true;
	}
	
	private void setErasing(boolean erasing) {
		this.isErasing = erasing;
		int colorToSet = 0;
		previousPaintColor = drawPaint.getColor();
		if (previousPaintColor <= 0) {
			colorToSet = context.getResources().getColor(R.color.brown);
		} else {
			colorToSet = previousPaintColor;
		}
		if (isErasing) {
			drawPaint.setColor(context.getResources().getColor(R.color.white));
			drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		} else {
			drawPaint.setColor(colorToSet);
			drawPaint.setXfermode(null);
		}
	}
	public void setColor(String newColor) {
		this.previousPaintColor = drawPaint.getColor();
		paintColor = Color.parseColor(newColor);
		invalidate();
	}
	public float getBrushSize() {
		return brushSize;
	}
	public void setBrushSize(float newSize) {
		brushSize = newSize;
		drawPaint.setStrokeWidth(brushSize);
		setErasing(false);
	}
	public float getEraserSize() {
		return eraserSize;
	}
	public void setEraserSize(float newSize) {
		eraserSize = newSize;
		drawPaint.setStrokeWidth(eraserSize);
		setErasing(true);
	}
	public void setLastBrushSize(float lastBrushSize) {
		this.lastBrushSize = lastBrushSize;
	}
	public void setBackgroundImage(Bitmap image) {
		isImageLoaded = true;
		clearBrushes();
		canvasBitmap = image;
		drawCanvas.drawBitmap(image, new Matrix(), null);
		invalidate();
	}
	public float getLastBrushSize() {
		return lastBrushSize;
	}	
}
