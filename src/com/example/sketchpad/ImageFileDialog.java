package com.example.sketchpad;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Environment;

public class ImageFileDialog {
	private static final String PARENT_DIR = "..";
	private final Activity activity;
	private String[] fileList;
	private String fileEndsWith;
	private File currentPath;
	private boolean selectDirectoryOption;
	private ListenerList<FileSelectedListener> fileListenerList;
	private ListenerList<DirectorySelectedListener> dirListenerList;

	public interface FileSelectedListener {
		void fileSelected(File file);
	}
	public interface DirectorySelectedListener {
		void directorySelected(File directory);
	}
	public interface FireHandler<L> {
		void fireEvent(L listener);
	}
	
	public ImageFileDialog(Activity activity, File path) {
		this.activity = activity;
		fileListenerList = new ListenerList<FileSelectedListener>();
		dirListenerList = new ListenerList<DirectorySelectedListener>();
		if (!path.exists()) {
			path = Environment.getExternalStorageDirectory();
		}
		loadFileList(path);
	}
	
	private void fireFileSelectedEvent(final File file) {
		fileListenerList.fireEvent(new FireHandler<FileSelectedListener>() {
			@Override
			public void fireEvent(FileSelectedListener listener) {
				listener.fileSelected(file);
			}
		});					
	}
	private void fireDirectorySelectedEvent(final File directory) {
		dirListenerList.fireEvent(new FireHandler<DirectorySelectedListener>() {
			@Override
			public void fireEvent(DirectorySelectedListener listener) {
				listener.directorySelected(directory);
			}
		});
	}
	private void loadFileList(File path) {
		this.currentPath = path;
		List<String> r = new ArrayList<String>();
		if (path.exists()) {
			if (path.getParentFile() != null) {
				r.add(PARENT_DIR);
			}
			String[] fileList_ONE = path.list(new ImageFilenameFilter());
			for (String file : fileList_ONE) {
				r.add(file);
			}
		}
		fileList = (String[])r.toArray(new String[] { });
	}
	private File getChosenFile(String fileChosen) {
		if (fileChosen.equals(PARENT_DIR)) {
			return currentPath.getParentFile();
		} else {
			return new File(currentPath, fileChosen);
		}
	}
	public Dialog createFileDialog() {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		builder.setTitle(currentPath.getPath());
		if (selectDirectoryOption) {
			builder.setPositiveButton("Select directory", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fireDirectorySelectedEvent(currentPath);
				}				
			});
		}
		builder.setItems(fileList, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String fileChosen = fileList[which];
				File chosenFile = getChosenFile(fileChosen);
				if (chosenFile.isDirectory()) {
					loadFileList(chosenFile);
					dialog.cancel();
					dialog.dismiss();
					showDialog();
				} else {
					fireFileSelectedEvent(chosenFile);
				}
			}
		});
		dialog = builder.show();
		return dialog;
	}
	public void addFileListener(FileSelectedListener listener) {
		fileListenerList.add(listener);
	}
	public void removeFileListener(FileSelectedListener listener) {
		fileListenerList.remove(listener);
	}
	public void setSelectedDirectoryOption(boolean selectDirectoryOption) {
		this.selectDirectoryOption = selectDirectoryOption;
	}
	public void addDirectoryListener(DirectorySelectedListener listener){
		dirListenerList.add(listener);
	}
	public void removeDirectoryListener(DirectorySelectedListener listener) {
		dirListenerList.remove(listener);
	}
	public void showDialog() {
		createFileDialog().show();
	}
	public void setFileEndsWith(String fileEndsWith) {
		this.fileEndsWith = fileEndsWith != null ?
				            fileEndsWith.toLowerCase(Locale.US) :
				            fileEndsWith;
	}
	
	private class ImageFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String filename) {
			File file = new File(dir, filename);
			if (!file.canRead()) {
				return false;
			} else {
				boolean endsWith = fileEndsWith != null ? 
						           filename.toLowerCase(Locale.US).endsWith(fileEndsWith) :
						           true;		           
			    return endsWith || file.isDirectory();
			}
		}	
	}
	private class ListenerList<L> {
		private List<L> listenerList = new ArrayList<L>();
		
		public void add(L listener) {
			listenerList.add(listener);
		}
		public void fireEvent(FireHandler<L> fireHandler) {
			List<L> copy = new ArrayList<L>(listenerList);
			for (L l : copy) {
				fireHandler.fireEvent(l);
			}
		}
		public void remove(L listener) {
			listenerList.remove(listener);
		}
		public List<L> getListenerList() {
			return listenerList;
		}
	}		
}