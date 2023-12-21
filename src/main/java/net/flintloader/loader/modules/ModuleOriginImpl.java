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
package net.flintloader.loader.modules;

import java.nio.file.Path;
import java.util.List;

import net.flintloader.loader.api.ModuleOrigin;

public final class ModuleOriginImpl implements ModuleOrigin {

	private final List<Path> paths;

	public ModuleOriginImpl(List<Path> paths) {
		this.paths = paths;
	}

	@Override
	public Kind getKind() {
		// TODO Support Nested Modules
		return Kind.PATH;
	}

	@Override
	public List<Path> getPaths() {
		return paths;
	}

	@Override
	public String getParentModuleId() {
		// TODO Support Nested Modules
		return "";
	}

	@Override
	public String getParentSubLocation() {
		// TODO Support Nested Modules
		return "";
	}
}
