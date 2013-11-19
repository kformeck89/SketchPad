package com.example.sketchpad;

import android.graphics.Paint;
import android.graphics.Path;

public class PaintPathPair {
	private Paint paint = null;
	private Path path = null;
	
	public PaintPathPair() { }
	public PaintPathPair(Paint paint, Path path) {
		this.paint = paint;
		this.path = path;
	}
	
	public Paint getPaint() {
		return paint;
	}
	public void setPaint(Paint paint) {
		this.paint = paint;
	}
	public Path getPath() {
		return path;
	}
	public void setPath(Path path) {
		this.path = path;
	}
	
}
