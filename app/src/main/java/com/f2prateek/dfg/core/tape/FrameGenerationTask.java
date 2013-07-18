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

import android.content.Context;
import android.net.Uri;
import com.f2prateek.dfg.model.Device;
import com.squareup.tape.Task;

public class FrameGenerationTask implements Task<FrameGenerationTask.Callback> {

  private final Context context;
  private final Device device;
  private final boolean withGlare;
  private final boolean withShadow;
  private final Uri imageUri;

  public FrameGenerationTask(Context context, Device device, boolean withGlare, boolean withShadow,
      Uri imageUri) {
    this.context = context;
    this.device = device;
    this.withGlare = withGlare;
    this.withShadow = withShadow;
    this.imageUri = imageUri;
  }

  @Override public void execute(Callback callback) {

  }

  public interface Callback {
    void onSuccess(Uri uri);

    void onFailure();
  }
}
