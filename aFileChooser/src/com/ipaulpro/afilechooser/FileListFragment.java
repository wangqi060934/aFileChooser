/* 
 * Copyright (C) 2012 Paul Burke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.ipaulpro.afilechooser;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ipaulpro.afilechooser.utils.FileUtils;

/**
 * Fragment that displays a list of Files in a given path.
 *
 * @version 2012-10-28
 *
 * @author paulburke (ipaulpro)
 *
 */
public class FileListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<List<File>> {

	private static final int LOADER_ID = 0;

	private FileListAdapter mAdapter;
	private String mPath;

	/**
	 * Create a new instance with the given file path.
	 *
	 * @param path The absolute path of the file (directory) to display.
	 * @return A new Fragment with the given file path.
	 */
	public static FileListFragment newInstance(String path) {
		FileListFragment fragment = new FileListFragment();
		Bundle args = new Bundle();
		args.putString(FileChooserActivity.PATH, path);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new FileListAdapter(getActivity());
		mPath = getArguments() != null ? getArguments().getString(
				FileChooserActivity.PATH) : FileChooserActivity.ROOT_PATH;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_directory));
		setListAdapter(mAdapter);
		setListShown(false);

		getLoaderManager().initLoader(LOADER_ID, null, this);

//		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//			@Override
//			public boolean onItemLongClick(AdapterView<?> adapterView, View view,
//					int position, long arg3) {
//					File file = (File) mAdapter.getItem(position);
//					((FileChooserActivity) getActivity()).onFileOrDirSelected(file);
//				return true;
//			}
//		});

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        File file = (File) mAdapter.getItem(position);
        mPath = file.getAbsolutePath();
        ((FileChooserActivity) getActivity()).onFileSelected(file);
    }

	@Override
	public Loader<List<File>> onCreateLoader(int id, Bundle args) {
		return new FileLoader(getActivity(), mPath);
	}

	@Override
	public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
		mAdapter.setListItems(data);

		if (isResumed())
			setListShown(true);
		else
			setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<List<File>> loader) {
		mAdapter.clear();
	}

	private static class FileLoader extends AsyncTaskLoader<List<File>> {

		private static final int FILE_OBSERVER_MASK = FileObserver.CREATE
				| FileObserver.DELETE | FileObserver.DELETE_SELF
				| FileObserver.MOVED_FROM | FileObserver.MOVED_TO
				| FileObserver.MODIFY | FileObserver.MOVE_SELF;

		private FileObserver mFileObserver;

		private List<File> mData;
		private String mPath;

		public FileLoader(Context context, String path) {
			super(context);
			this.mPath = path;
		}

		@Override
		public List<File> loadInBackground() {
			return FileUtils.getFileList(mPath);
		}

		@Override
		public void deliverResult(List<File> data) {
			if (isReset()) {
				onReleaseResources(data);
				return;
			}

			List<File> oldData = mData;
			mData = data;

			if (isStarted())
				super.deliverResult(data);

			if (oldData != null && oldData != data)
				onReleaseResources(oldData);
		}

		@Override
		protected void onStartLoading() {
			if (mData != null)
				deliverResult(mData);

			if (mFileObserver == null) {
				mFileObserver = new FileObserver(mPath, FILE_OBSERVER_MASK) {
					@Override
					public void onEvent(int event, String path) {
						onContentChanged();
					}
				};
			}
			mFileObserver.startWatching();

			if (takeContentChanged() || mData == null)
				forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			onStopLoading();

			if (mData != null) {
				onReleaseResources(mData);
				mData = null;
			}
		}

		@Override
		public void onCanceled(List<File> data) {
			super.onCanceled(data);

			onReleaseResources(data);
		}

		protected void onReleaseResources(List<File> data) {

			if (mFileObserver != null) {
				mFileObserver.stopWatching();
				mFileObserver = null;
			}
		}
	}

	private class FileListAdapter extends BaseAdapter {

		private List<File> mFiles = new ArrayList<File>();
		private final LayoutInflater mInflater;

		public FileListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public ArrayList<File> getListItems() {
			return (ArrayList<File>) mFiles;
		}

		public void setListItems(List<File> files) {
			this.mFiles = files;
			notifyDataSetChanged();
		}

		@Override
	    public int getCount() {
			return mFiles.size();
		}

		public void add(File file) {
			mFiles.add(file);
			notifyDataSetChanged();
		}

		public void clear() {
			mFiles.clear();
			notifyDataSetChanged();
		}

		@Override
	    public Object getItem(int position) {
			return mFiles.get(position);
		}

		@Override
	    public long getItemId(int position) {
			return position;
		}

		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder = null;

			if (row == null) {
				row = mInflater.inflate(R.layout.file, parent, false);
				holder = new ViewHolder(row);
				row.setTag(holder);
			} else {
				// Reduce, reuse, recycle!
				holder = (ViewHolder) row.getTag();
			}

			// Get the file at the current position
			final File file = (File) getItem(position);

			// Set the TextView as the file name
			holder.nameView.setText(file.getName());

			// If the item is not a directory, use the file icon
			holder.iconView.setImageResource(file.isDirectory() ? R.drawable.ic_folder
					: R.drawable.ic_file);

			return row;
		}

		 class ViewHolder {
			TextView nameView;
			ImageView iconView;

			ViewHolder(View row) {
				nameView = (TextView) row.findViewById(R.id.file_name);
				iconView = (ImageView) row.findViewById(R.id.file_icon);
			}
		}
	}
}