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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ModuleResolvers {

	private final List<IModuleResolver> resolvers = new ArrayList<>();

	public ModuleResolvers() {}

	public void addResolver(IModuleResolver resolver) {
		if (!resolvers.contains(resolver)) {
			resolvers.add(resolver);
		} else {
			Log.error(LogCategory.DISCOVERY, "Duplicate Module Resolver registered: " + resolver.getClass().getSimpleName());
		}
	}

	public void resolve(Map<String, FlintModuleContainer> outList) {
		resolvers.forEach(resolver -> resolver.resolve(outList));
	}

}
