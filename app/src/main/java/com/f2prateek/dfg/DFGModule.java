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

import android.content.Context;
import com.f2prateek.dfg.core.AbstractGenerateFrameService;
import com.f2prateek.dfg.core.GenerateFrameService;
import com.f2prateek.dfg.core.GenerateMultipleFramesService;
import com.f2prateek.dfg.core.tape.FrameGenerationTaskQueue;
import com.f2prateek.dfg.ui.AboutFragment;
import com.f2prateek.dfg.ui.BaseActivity;
import com.f2prateek.dfg.ui.DeviceFragment;
import com.f2prateek.dfg.ui.MainActivity;
import com.f2prateek.dfg.ui.ReceiverActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Dagger module for setting up provides statements.
 * Register all of your entry points below.
 */
@Module(
    complete = false,

    injects = {
        DFGApplication.class, BaseActivity.class, MainActivity.class, ReceiverActivity.class,
        DeviceFragment.class, AboutFragment.class, AbstractGenerateFrameService.class,
        GenerateFrameService.class, GenerateMultipleFramesService.class
    }

)
public class DFGModule {

  @Provides @Singleton Bus provideOttoBus() {
    return new Bus();
  }

  @Provides @Singleton Gson provideGson() {
    return new GsonBuilder().create();
  }

  @Provides @Singleton
  FrameGenerationTaskQueue provideTaskQueue(Context context, Gson gson, Bus bus) {
    return FrameGenerationTaskQueue.create(context, gson, bus);
  }
}