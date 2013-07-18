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

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import com.f2prateek.dfg.DFGApplication;
import com.squareup.otto.Bus;
import javax.inject.Inject;

public class FrameGenerateTaskService extends Service implements FrameGenerationTask.Callback {

  @Inject FrameGenerationTaskQueue queue;
  @Inject Bus bus;

  private boolean running;

  @Override public void onCreate() {
    super.onCreate();
    DFGApplication.getInstance().inject(this);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    executeNext();
    return START_STICKY;
  }

  private void executeNext() {
    if (running) return; // Only one task at a time.

    FrameGenerationTask task = queue.peek();
    if (task != null) {
      running = true;
      task.execute(this);
    } else {
      stopSelf();
    }
  }

  @Override public void onSuccess(final Uri uri) {
    running = false;
    queue.remove();
    bus.post(new FrameGenerateSuccessEvent(uri));
    executeNext();
  }

  @Override public void onFailure() {
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}