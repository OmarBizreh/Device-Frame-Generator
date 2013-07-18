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
import android.content.Intent;
import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import com.squareup.tape.TaskQueue;
import java.io.File;
import java.io.IOException;

public class FrameGenerationTaskQueue extends TaskQueue<FrameGenerationTask> {
  private static final String FILENAME = "image_upload_task_queue";

  private final Context context;
  private final Bus bus;

  private FrameGenerationTaskQueue(ObjectQueue<FrameGenerationTask> delegate, Context context,
      Bus bus) {
    super(delegate);
    this.context = context;
    this.bus = bus;
    bus.register(this);

    if (size() > 0) {
      startService();
    }
  }

  private void startService() {
    context.startService(new Intent(context, FrameGenerateTaskService.class));
  }

  @Override public void add(FrameGenerationTask task) {
    super.add(task);
    bus.post(produceSizeChanged());
    startService();
  }

  @Override public void remove() {
    super.remove();
    bus.post(produceSizeChanged());
  }

  @Produce public FrameGenerateQueueSizeEvent produceSizeChanged() {
    return new FrameGenerateQueueSizeEvent(size());
  }

  public static FrameGenerationTaskQueue create(Context context, Gson gson, Bus bus) {
    FileObjectQueue.Converter<FrameGenerationTask> converter =
        new GsonConverter<FrameGenerationTask>(gson, FrameGenerationTask.class);
    File queueFile = new File(context.getFilesDir(), FILENAME);
    FileObjectQueue<FrameGenerationTask> delegate;
    try {
      delegate = new FileObjectQueue<FrameGenerationTask>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new FrameGenerationTaskQueue(delegate, context, bus);
  }
}
