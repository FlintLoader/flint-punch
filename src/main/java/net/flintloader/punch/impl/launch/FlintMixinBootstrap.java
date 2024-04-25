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
package net.flintloader.punch.impl.launch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleList;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.launch.punch.MixinServicePunch;
import net.flintloader.punch.impl.launch.punch.MixinServicePunchBootstrap;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import net.flintloader.punch.impl.util.mappings.MixinIntermediaryDevRemapper;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

import net.fabricmc.mapping.tree.TinyTree;

public final class FlintMixinBootstrap {
	private FlintMixinBootstrap() { }

	private static boolean initialized = false;

	public static void init(PunchLoaderImpl loader) {
		if (initialized) {
			throw new RuntimeException("FlintMixinBootstrap has already been initialized!");
		}

		System.setProperty("mixin.bootstrapService", MixinServicePunchBootstrap.class.getName());
		System.setProperty("mixin.service", MixinServicePunch.class.getName());

		MixinBootstrap.init();
		MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);

		if (PunchLauncherBase.getLauncher().isDevelopment()) {
			MappingConfiguration mappingConfiguration = PunchLauncherBase.getLauncher().getMappingConfiguration();
			TinyTree mappings = mappingConfiguration.getMappings();

			if (mappings != null) {
				List<String> namespaces = mappings.getMetadata().getNamespaces();

				if (namespaces.contains("intermediary") && namespaces.contains(mappingConfiguration.getTargetNamespace())) {
					System.setProperty("mixin.env.remapRefMap", "true");

					try {
						MixinIntermediaryDevRemapper remapper = new MixinIntermediaryDevRemapper(mappings, "intermediary", mappingConfiguration.getTargetNamespace());
						MixinEnvironment.getDefaultEnvironment().getRemappers().add(remapper);
						Log.info(LogCategory.MIXIN, "Loaded Flint development mappings for mixin remapper!");
					} catch (Exception e) {
						Log.error(LogCategory.MIXIN, "Flint development environment setup error - the game will probably crash soon!");
						e.printStackTrace();
					}
				}
			}
		}

		Map<String, FlintModuleMetadata> configToModuleMap = new HashMap<>();

		for (FlintModuleContainer container : ModuleList.getInstance().allModules()) {
			FlintModuleMetadata module = container.getMetadata();
			if (!module.isBuiltIn()) {
				for (String config : module.getMixins()) {
					FlintModuleMetadata prev = configToModuleMap.putIfAbsent(config, module);
					if (prev != null) throw new RuntimeException(String.format("Non-unique Mixin config name %s used by the modules %s and %s", config, prev.getId(), module.getId()));

					Mixins.addConfiguration(config);
				}
			}
		}

		try {
			IMixinConfig.class.getMethod("decorate", String.class, Object.class);
			MixinConfigDecorator.apply(configToModuleMap);
		} catch (NoSuchMethodException e) {
			Log.info(LogCategory.MIXIN, "Detected old Mixin version without config decoration support");
		}

		initialized = true;
	}

	private static final class MixinConfigDecorator {

		static void apply(Map<String, FlintModuleMetadata> configToModMap) {
			for (Config rawConfig : Mixins.getConfigs()) {
				FlintModuleMetadata module = configToModMap.get(rawConfig.getName());
				if (module == null) continue;

				IMixinConfig config = rawConfig.getConfig();
				config.decorate(FabricUtil.KEY_MOD_ID, module.getId());
				config.decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST);
			}
		}
	}
}
