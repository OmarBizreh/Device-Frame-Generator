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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import butterknife.InjectView;
import com.f2prateek.dfg.R;
import java.util.ArrayList;
import java.util.List;

public class PictureViewerActivity extends BaseActivity {

  @InjectView(R.id.pager) ViewPager pager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Intent intent = getIntent();
    String action = intent.getAction();
    if (Intent.ACTION_SEND.equals(action)) {
      // Got a single image
      handleReceivedSingleImage(intent);
    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
      // Got multiple images
      handleReceivedMultipleImages(intent);
    }
  }

  /** Handle an intent that provides a single image. */
  private void handleReceivedSingleImage(Intent i) {
    Uri imageUri = i.getParcelableExtra(Intent.EXTRA_STREAM);
    List<Uri> imageUris = new ArrayList<Uri>();
    imageUris.add(imageUri);
    pager.setAdapter(new ImagePagerAdapter(imageUris));
  }

  /** Handle an intent that provides multiple images. */
  void handleReceivedMultipleImages(Intent i) {
    ArrayList<Uri> imageUris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    pager.setAdapter(new ImagePagerAdapter(imageUris));
  }
}
