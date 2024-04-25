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
package net.flintloader.loader.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.loader.modules.resolver.ModuleResolvers;
import net.flintloader.loader.modules.resolver.impl.ClassPathModuleResolver;
import net.flintloader.loader.modules.resolver.impl.DirectoryModuleResolver;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import org.jetbrains.annotations.Nullable;

/**
 * @author HypherionSA
 */
public final class ModuleList {

	private static ModuleList instance;
	private final ModuleResolvers moduleResolvers = new ModuleResolvers();

    private final Map<String, FlintModuleContainer> MODULES = new HashMap<>();

	public static ModuleList getInstance() {
		if (instance == null) {
			instance = new ModuleList();
		}
		return instance;
	}

	public void discoverModules() {
		Log.info(LogCategory.DISCOVERY, "Discovering Modules...");
		moduleResolvers.addResolver(new ClassPathModuleResolver());
		moduleResolvers.addResolver(new DirectoryModuleResolver());
		moduleResolvers.resolve(MODULES);

		Log.info(LogCategory.DISCOVERY, "Discovered " + MODULES.size() + " modules");

		FlintModuleMetadata mc = new FlintModuleMetadata("minecraft", "Minecraft", PunchLoaderImpl.INSTANCE.getGameProvider().getRawGameVersion(), true);
		FlintModuleContainerImpl mcContainer = new FlintModuleContainerImpl(mc, new ArrayList<>(), new ModuleOriginImpl(new ArrayList<>()));
		MODULES.put("minecraft", mcContainer);

		FlintModuleMetadata java = new FlintModuleMetadata("java", "Java", System.getProperty("java.version"), true);
		FlintModuleContainerImpl javaContainer = new FlintModuleContainerImpl(java, new ArrayList<>(), new ModuleOriginImpl(new ArrayList<>()));
		MODULES.put("java", javaContainer);
    }

	public List<FlintModuleContainer> allModules() {
		return new ArrayList<>(MODULES.values());
	}

	public boolean isModuleLoaded(String id) {
		return MODULES.containsKey(id);
	}

	public int getModuleCount() {
		return (int) MODULES.values().stream().map(m -> !m.getMetadata().isBuiltIn()).count();
	}

	@Nullable
	public FlintModuleContainer getModuleContainer(String id) {
		if (MODULES.containsKey(id)) {
			return MODULES.get(id);
		}
		return null;
	}

	@Nullable
	public FlintModuleMetadata getModuleMeta(String id) {
		if (MODULES.containsKey(id)) {
			return MODULES.get(id).getMetadata();
		}
		return null;
	}

}
