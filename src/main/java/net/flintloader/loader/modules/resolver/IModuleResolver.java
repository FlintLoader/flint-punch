/**
* Copyright 2016 FabricMC
* Copyright 2024 Flint Loader Contributors
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
**/
package net.flintloader.loader.modules.resolver;

import static net.flintloader.loader.core.PunchLauncherHooks.gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParseException;
import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.loader.modules.FlintModuleContainerImpl;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleOriginImpl;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public interface IModuleResolver {

	void resolve(Map<String, FlintModuleContainer> outList);

	default void readModuleJson(Map<String, FlintModuleContainer> outList, InputStream inputStream, List<Path> source) throws IOException {
		try {
			FlintModuleMetadata moduleContainer = gson.fromJson(new InputStreamReader(inputStream), FlintModuleMetadata.class);
			FlintModuleContainerImpl container = new FlintModuleContainerImpl(moduleContainer, source, new ModuleOriginImpl(source));

			if (outList.containsKey(moduleContainer.getId())) {
				// TODO Duplicate Modid exception
				//throw new DuplicateModException(modInfo, modInfoMap.get(modInfo.id));
			}

			outList.put(moduleContainer.getId(), container);
			Log.info(LogCategory.DISCOVERY, "Loaded module '" + moduleContainer.getId() + "'");
		} catch (JsonParseException e) {
			throw new RuntimeException("Could not read flintmodule.json in " + source, e);
		} finally {
			inputStream.close();
		}
	}

}
