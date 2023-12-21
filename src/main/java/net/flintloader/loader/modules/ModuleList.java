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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.flintloader.loader.api.ModuleContainer;
import net.flintloader.loader.modules.resolver.ModuleResolver;
import net.flintloader.loader.modules.resolver.impl.ClassPathModuleResolver;
import net.flintloader.loader.modules.resolver.impl.DirectoryModuleResolver;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

/**
 * @author HypherionSA
 * @date 21/06/2022
 */
public class ModuleList {

	private static ModuleList instance;
	private final ModuleResolver moduleResolver = new ModuleResolver();

    private final Map<String, ModuleContainer> MODULES = new HashMap<>();

	public static ModuleList getInstance() {
		if (instance == null) {
			instance = new ModuleList();
		}
		return instance;
	}

	public void discoverModules() {
		Log.info(LogCategory.DISCOVERY, "Discovering Modules...");
		moduleResolver.addResolver(new ClassPathModuleResolver());
		moduleResolver.addResolver(new DirectoryModuleResolver());
		moduleResolver.resolve(MODULES);

		Log.info(LogCategory.DISCOVERY, "Discovered " + MODULES.size() + " modules");

		FlintModuleMetadata mc = new FlintModuleMetadata("minecraft", "Minecraft", PunchLoaderImpl.INSTANCE.getGameProvider().getRawGameVersion(), true);
		ModuleContainerImpl mcContainer = new ModuleContainerImpl(mc, new ArrayList<>(), new ModuleOriginImpl(new ArrayList<>()));
		MODULES.put("minecraft", mcContainer);

		FlintModuleMetadata java = new FlintModuleMetadata("java", "Java", System.getProperty("java.version"), true);
		ModuleContainerImpl javaContainer = new ModuleContainerImpl(java, new ArrayList<>(), new ModuleOriginImpl(new ArrayList<>()));
		MODULES.put("java", javaContainer);
    }

	public List<ModuleContainer> allModules() {
		return new ArrayList<>(MODULES.values());
	}

	public boolean isModuleLoaded(String id) {
		return MODULES.containsKey(id);
	}

	public int getModuleCount() {
		return (int) MODULES.values().stream().map(m -> !m.getMetadata().isBuiltIn()).count();
	}

	public ModuleContainer getModuleMeta(String id) {
		if (MODULES.containsKey(id)) {
			return MODULES.get(id);
		}
		return null;
	}

}
