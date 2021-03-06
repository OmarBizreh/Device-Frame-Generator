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

package com.f2prateek.dfg;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.test.ServiceTestCase;
import com.f2prateek.dfg.core.AbstractGenerateFrameService;
import com.f2prateek.dfg.core.GenerateFrameService;
import com.f2prateek.dfg.model.Device;
import java.io.File;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

public class GenerateFrameServiceTest extends ServiceTestCase<GenerateFrameService> {

  private static final int WAIT_TIME = 10 * 1000; // 10 ms

  public GenerateFrameServiceTest() {
    super(GenerateFrameService.class);
  }

  public void testFrameGeneration() throws Exception {
    TestUtils.deleteFile(
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            AppConstants.DFG_DIR_NAME));
    File appDirectory =
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            AppConstants.DFG_DIR_NAME);

    // Pick a random device
    Device randomDevice = TestUtils.getRandomDevice();
    // Make the test screenshot
    Uri screenshotUri = TestUtils.makeTestScreenShot(getSystemContext(), randomDevice);

    Intent intent = new Intent(getSystemContext(), GenerateFrameService.class);
    intent.putExtra(AppConstants.KEY_EXTRA_DEVICE, randomDevice);
    intent.putExtra(AppConstants.KEY_EXTRA_SCREENSHOT, screenshotUri);
    startService(intent);
    assertThat(getService()).isNotNull();

    Thread.sleep(WAIT_TIME);

    assertThat(appDirectory).exists().isDirectory();
    assertThat(appDirectory.list()).hasSize(1);

    // Clean up
    ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(
        AbstractGenerateFrameService.DFG_NOTIFICATION_ID);
    TestUtils.deleteFile(new File(TestUtils.getPath(getSystemContext(), screenshotUri)));
    TestUtils.deleteFile(appDirectory);
    MediaScannerConnection.scanFile(getSystemContext(), new String[] { appDirectory.toString() },
        null, null);
  }
}
