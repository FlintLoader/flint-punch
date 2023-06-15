/**
* Copyright 2016 FabricMC
* Copyright 2023 HypherionSA and contributors
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
package net.flintloader.loader.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.vdurmont.semver4j.Semver;
import net.flintloader.loader.api.FlintModule;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleList;
import net.flintloader.loader.modules.Strings;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

/**
 * @author HypherionSA
 * @date 23/06/2022
 */
public class FlintLoader {

	public static final Gson gson = new Gson();
	private static final Map<String, FlintModule> entryPoints = new HashMap<>();

    public static void discoverModules() {
		ModuleList.getInstance().discoverModules();
		gatherEntryPoints();
    }

	public static void finishModuleSetup() {
		checkDependencies();
        for (FlintModuleMetadata module : ModuleList.getInstance().allModules()) {
            if (!module.isBuiltIn()) {
				if (!module.getId().equalsIgnoreCase("flintloader")) {
					PunchLauncherBase.getLauncher().addToClassPath(LoaderUtil.normalizePath(module.getSource()));
				}
			}
        }
		earlyInitModules();
    }

	private static void checkDependencies() {
		List<String> missing = new ArrayList<>();
		List<String> invalid = new ArrayList<>();

		for (FlintModuleMetadata meta : ModuleList.getInstance().allModules()) {

			/* Check for required modules */
			if (!meta.isBuiltIn()) {
				if (meta.getDepends() != null) {
					meta.getDepends().forEach((depId, depVersion) -> {
						if (!ModuleList.getInstance().isModuleLoaded(depId)) {
							missing.add(Strings.MISSING_DEP.resolve(meta.getId(), depId));
						} else {
							FlintModuleMetadata metadata = ModuleList.getInstance().getModuleMeta(depId);
							Semver depVer = new Semver(metadata.getVersion(), Semver.SemverType.COCOAPODS);
							if (!depVer.satisfies(depVersion)) {
								String errType = depVer.isGreaterThan(depVersion) ? "greater than" : "less than";
								invalid.add(Strings.WRONG_DEP_VERSION.resolve(meta.getId(), errType, depVersion, depVer.getOriginalValue()));
							}
						}
					});
				}

				/* Check for breaking modules */
				if (meta.getBreaks() != null) {
					meta.getBreaks().forEach((depid, depVersion) -> {
						if (ModuleList.getInstance().isModuleLoaded(depid)) {
							FlintModuleMetadata metadata = ModuleList.getInstance().getModuleMeta(depid);
							Semver depVer = new Semver(metadata.getVersion(), Semver.SemverType.COCOAPODS);
							if (depVer.satisfies(depVersion)) {
								String errType = depVer.isGreaterThan(depVersion) ? "greater than" : "less than";
								invalid.add(Strings.BREAKS.resolve(meta.getId(), errType, depVersion, depVer.getOriginalValue()));
							}
						}
					});
				}
			}
		}

		// TODO - Handle incompatible mod sets!
	}

	public static void earlyInitModules() {
		entryPoints.forEach((s, flintModule) -> {
			try {
				flintModule.earlyInitialization();
			} catch (Exception e) {
				Log.error(LogCategory.ENTRYPOINT, "Failed to call early entry point on module '" + s + "'");
			}
		});
	}

	public static void initializeModules() {
		entryPoints.forEach((s, flintModule) -> {
			try {
				flintModule.initializeModule();
			} catch (Exception e) {
				Log.error(LogCategory.ENTRYPOINT, "Failed to call entry point on module '" + s + "'");
			}
		});
    }

	private static void gatherEntryPoints() {
		for (FlintModuleMetadata metadata : ModuleList.getInstance().allModules()) {
			if (!metadata.isBuiltIn() && metadata.getEntryPoint() != null) {
				try {
					Class<?> clazz = PunchLauncherBase.getClass(metadata.getEntryPoint());
					entryPoints.put(metadata.getId(), ((FlintModule)clazz.newInstance()));
				} catch (Exception e) {
					Log.error(LogCategory.ENTRYPOINT, "Failed to discover entry points for '" + metadata.getId() + "'");
				}
			}
		}
	}

}
