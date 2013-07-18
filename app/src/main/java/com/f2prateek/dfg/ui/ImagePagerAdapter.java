/*
 * Copyright 2013 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.f2prateek.dfg.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.f2prateek.dfg.util.BitmapUtils;
import com.f2prateek.dfg.util.Ln;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class ImagePagerAdapter extends PagerAdapter {

  private static LruCache<String, Bitmap> mMemoryCache;
  private final List<Uri> imageUris;
  private final Context context;

  public ImagePagerAdapter(Context context, List<Uri> imageUris) {
    this.context = context;
    this.imageUris = imageUris;

    buildImageCache();
  }

  private static void buildImageCache() {
    if (mMemoryCache != null) {
      return;
    }
    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 8;

    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
      @Override
      protected int sizeOf(String key, Bitmap bitmap) {
        // The cache size will be measured in kilobytes rather than
        // number of items.
        return bitmap.getByteCount() / 1024;
      }
    };
  }

  public static boolean cancelPotentialWork(Uri data, ImageView imageView) {
    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

    if (bitmapWorkerTask != null) {
      final Uri bitmapData = bitmapWorkerTask.data;
      if (bitmapData.compareTo(data) != 0) {
        // Cancel previous task
        bitmapWorkerTask.cancel(true);
      } else {
        // The same work is already in progress
        return false;
      }
    }
    // No task associated with the ImageView, or an existing task was cancelled
    return true;
  }

  private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
    if (imageView != null) {
      final Drawable drawable = imageView.getDrawable();
      if (drawable instanceof AsyncDrawable) {
        final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
        return asyncDrawable.getBitmapWorkerTask();
      }
    }
    return null;
  }

  @Override public int getCount() {
    return imageUris.size();
  }

  @Override public boolean isViewFromObject(View view, Object object) {
    return (view == object);
  }

  @Override public View instantiateItem(ViewGroup container, int position) {
    ImageView imageView = new ImageView(context);
    loadBitmap(imageUris.get(position), imageView);
    return imageView;
  }

  public void loadBitmap(Uri imageUri, ImageView imageView) {
    final String imageKey = imageUri.toString();

    final Bitmap bitmap = getBitmapFromMemCache(imageKey);
    if (bitmap != null) {
      imageView.setImageBitmap(bitmap);
    } else {
      if (cancelPotentialWork(imageUri, imageView)) {
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, task);
        imageView.setImageDrawable(asyncDrawable);
        task.execute(imageUri);
      }
    }
  }

  public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
    if (getBitmapFromMemCache(key) == null) {
      mMemoryCache.put(key, bitmap);
    }
  }

  public Bitmap getBitmapFromMemCache(String key) {
    return mMemoryCache.get(key);
  }

  static class AsyncDrawable extends BitmapDrawable {
    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

    public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
      super(res, bitmap);
      bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
    }

    public BitmapWorkerTask getBitmapWorkerTask() {
      return bitmapWorkerTaskReference.get();
    }
  }

  class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private Uri data = null;

    public BitmapWorkerTask(ImageView imageView) {
      // Use a WeakReference to ensure the ImageView can be garbage collected
      imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Uri... params) {
      final Bitmap bitmap;
      try {
        bitmap = BitmapUtils.decodeUri(context.getContentResolver(), params[0]);
      } catch (IOException e) {
        e.printStackTrace();
        Ln.e(e);
        throw new RuntimeException("Couldn't open image");
      }
      addBitmapToMemoryCache(params[0].toString(), bitmap);
      return bitmap;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
      if (isCancelled()) {
        bitmap = null;
      }

      if (imageViewReference != null && bitmap != null) {
        final ImageView imageView = imageViewReference.get();
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (this == bitmapWorkerTask && imageView != null) {
          imageView.setImageBitmap(bitmap);
        }
      }
    }
  }
}
