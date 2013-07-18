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

package com.f2prateek.dfg.core.tape;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.f2prateek.dfg.AppConstants;
import com.f2prateek.dfg.core.UnmatchedDimensionsException;
import com.f2prateek.dfg.model.Device;
import com.f2prateek.dfg.util.BitmapUtils;
import com.f2prateek.dfg.util.Ln;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewDFG {

  private final Context context;
  private final Device device;
  private final boolean withShadow;
  private final boolean withGlare;

  public NewDFG(Context context, Device device, boolean withShadow, boolean withGlare) {
    this.context = context;
    this.device = device;
    this.withShadow = withShadow;
    this.withGlare = withGlare;
  }

  /**
   * Checks if screenshot matches the aspect ratio of the device.
   *
   * @param device The Device to frame.
   * @param screenshot The screenshot to frame.
   * @return "port" if matched to portrait and "land" if matched to landscape
   * @throws com.f2prateek.dfg.core.UnmatchedDimensionsException If it could not match any
   * orientation to the device.
   */
  private static String checkDimensions(Device device, Bitmap screenshot)
      throws UnmatchedDimensionsException {
    float aspect1 = (float) screenshot.getHeight() / (float) screenshot.getWidth();
    float aspect2 = (float) device.getPortSize()[1] / (float) device.getPortSize()[0];

    if (aspect1 == aspect2) {
      return "port";
    } else if (aspect1 == 1 / aspect2) {
      return "land";
    }

    Ln.e("Screenshot height=%d, width=%d. Device height=%d, width=%d. Aspect1=%f, Aspect2=%f",
        screenshot.getHeight(), screenshot.getWidth(), device.getPortSize()[1],
        device.getPortSize()[0], aspect1, aspect2);
    throw new UnmatchedDimensionsException(device, screenshot.getHeight(), screenshot.getWidth());
  }

  /**
   * Check if the orientation is portrait
   *
   * @param orientation Orientation to check.
   * @return true if orientation is portrait
   */
  private static boolean isPortrait(String orientation) {
    return (orientation.compareTo("port") == 0);
  }

  /**
   * Generate the frame.
   *
   * @param imageUri Uri to the screenshot file.
   */
  public Uri generateFrame(Uri imageUri) throws IOException, UnmatchedDimensionsException {
    Ln.i("Generating for %s. %s and %s from file %s.", device.getName(),
        withGlare ? " With glare " : " Without glare ",
        withShadow ? " with shadow " : " without shadow ", imageUri.toString());
    return generateFrame(BitmapUtils.decodeUri(context.getContentResolver(), imageUri));
  }

  /**
   * Generate the frame.
   *
   * @param screenshot Screenshot to use.
   */
  private Uri generateFrame(Bitmap screenshot) throws UnmatchedDimensionsException, IOException {
    String orientation = checkDimensions(device, screenshot);

    final Bitmap background =
        BitmapUtils.decodeResource(context, device.getBackgroundString(orientation));
    final Bitmap glare = BitmapUtils.decodeResource(context, device.getGlareString(orientation));
    final Bitmap shadow = BitmapUtils.decodeResource(context, device.getShadowString(orientation));

    Canvas frame;
    if (withShadow) {
      frame = new Canvas(shadow);
      frame.drawBitmap(background, 0f, 0f, null);
    } else {
      frame = new Canvas(background);
    }

    final int[] offset;
    if (isPortrait(orientation)) {
      screenshot =
          Bitmap.createScaledBitmap(screenshot, device.getPortSize()[0], device.getPortSize()[1],
              false);
      offset = device.getPortOffset();
    } else {
      screenshot =
          Bitmap.createScaledBitmap(screenshot, device.getPortSize()[1], device.getPortSize()[0],
              false);
      offset = device.getLandOffset();
    }
    frame.drawBitmap(screenshot, offset[0], offset[1], null);

    if (withGlare) {
      frame.drawBitmap(glare, 0f, 0f, null);
    }

    ImageMetadata imageMetadata = prepareMetadata();
    // Save the screenshot to the MediaStore
    ContentValues values = new ContentValues();
    ContentResolver resolver = context.getContentResolver();
    values.put(MediaStore.Images.ImageColumns.DATA, imageMetadata.imageFilePath);
    values.put(MediaStore.Images.ImageColumns.TITLE, imageMetadata.imageFileName);
    values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageMetadata.imageFileName);
    values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.DATE_ADDED, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    OutputStream out = resolver.openOutputStream(imageUri);
    if (withShadow) {
      shadow.compress(Bitmap.CompressFormat.PNG, 100, out);
    } else {
      background.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    out.flush();
    out.close();

    screenshot.recycle();
    background.recycle();
    glare.recycle();
    shadow.recycle();

    // update file size in the database
    values.clear();
    values.put(MediaStore.Images.ImageColumns.SIZE, new File(imageMetadata.imageFilePath).length());
    resolver.update(imageUri, values, null, null);

    return imageUri;
  }

  /**
   * Prepare the metadata for our image.
   *
   * @return {@link com.f2prateek.dfg.core.DeviceFrameGenerator.ImageMetadata} that will be used
   *         for
   *         the image.
   */
  private ImageMetadata prepareMetadata() {
    ImageMetadata imageMetadata = new ImageMetadata();
    imageMetadata.imageTime = System.currentTimeMillis();
    String imageDate =
        new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(imageMetadata.imageTime));
    String imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        .getAbsolutePath();
    imageMetadata.imageFileName = String.format(AppConstants.DFG_FILE_NAME_TEMPLATE, imageDate);
    imageMetadata.imageFilePath =
        String.format(AppConstants.DFG_FILE_PATH_TEMPLATE, imageDir, AppConstants.DFG_DIR_NAME,
            imageMetadata.imageFileName);
    return imageMetadata;
  }

  private class ImageMetadata {
    String imageFileName;
    String imageFilePath;
    long imageTime;
  }
}