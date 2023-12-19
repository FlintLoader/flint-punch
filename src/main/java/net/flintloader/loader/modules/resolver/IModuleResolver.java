/**
* Copyright 2016 FabricMC
* Copyright 2023 Flint Loader Contributors
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

import static net.flintloader.loader.core.FlintLoader.gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.JsonParseException;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public interface IModuleResolver {

	public void resolve(Map<String, FlintModuleMetadata> outList);

	default void readModuleJson(Map<String, FlintModuleMetadata> outList, InputStream inputStream, Path source) throws IOException {
		try {
			FlintModuleMetadata moduleContainer = gson.fromJson(new InputStreamReader(inputStream), FlintModuleMetadata.class);
			moduleContainer.setSource(source);

			if (moduleContainer.getId() == null) {
				Log.error(LogCategory.DISCOVERY, "Module file " + moduleContainer.getSource() + "'s flintmodule.json is missing an 'id' field");
				return;
			} else if (outList.containsKey(moduleContainer.getId())) {
				//throw new DuplicateModException(modInfo, modInfoMap.get(modInfo.id));
			}

			outList.put(moduleContainer.getId(), moduleContainer);
			Log.info(LogCategory.DISCOVERY, "Loaded module '" + moduleContainer.getId() + "'");
		} catch (JsonParseException e) {
			throw new RuntimeException("Could not read flintmodule.json in " + source, e);
		} finally {
			inputStream.close();
		}
	}

}
