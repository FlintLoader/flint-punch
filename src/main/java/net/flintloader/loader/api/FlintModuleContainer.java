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
package net.flintloader.loader.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.flintloader.loader.modules.FlintModuleMetadata;

/**
 * @author HypherionSA
 * Helper class for Loaded Modules for things such as file loading
 */
public interface FlintModuleContainer {

	FlintModuleMetadata getMetadata();

	List<Path> getRootPaths();

	default Optional<Path> findPath(String file) {
		for (Path root : getRootPaths()) {
			Path path = root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
			if (Files.exists(path)) return Optional.of(path);
		}

		return Optional.empty();
	}

	ModuleOrigin getOrigin();

	Optional<FlintModuleContainer> getContainingModule();

	Collection<FlintModuleContainer> getContainedModules();

}
