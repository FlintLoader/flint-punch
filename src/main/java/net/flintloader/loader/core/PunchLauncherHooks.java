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
package net.flintloader.loader.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.vdurmont.semver4j.Semver;
import net.flintloader.loader.api.FlintModule;
import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.loader.core.entrypoints.FlintEntryPoints;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleList;
import net.flintloader.loader.modules.Strings;
import net.flintloader.loader.modules.entrypoint.EntryPointUtil;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author HypherionSA
 * Internal Hooks to handle module loading and registering
 */
@ApiStatus.Internal
public final class PunchLauncherHooks {

	public static final Gson gson = new Gson();

	/**
	 * Discover installed modules from modules dir and classpath during development
	 */
    public static void discoverModules() {
		ModuleList.getInstance().discoverModules();
    }

	/**
	 * Check if module dependencies are met and add them to the classpath
	 */
	public static void finishModuleSetup() {
		checkDependencies();

		for (FlintModuleContainer module : ModuleList.getInstance().allModules()) {
			if (!module.getMetadata().isBuiltIn()) {
				if (!module.getMetadata().getId().equalsIgnoreCase("flintloader")) {
					for (Path p : module.getRootPaths()) {
						PunchLauncherBase.getLauncher().addToClassPath(LoaderUtil.normalizePath(p));
					}
				}
			}
		}
    }

	/**
	 * Check for required and breaking dependencies
	 */
	private static void checkDependencies() {
		List<String> missing = new ArrayList<>();
		List<String> invalid = new ArrayList<>();

		for (FlintModuleContainer container : ModuleList.getInstance().allModules()) {
			FlintModuleMetadata meta = container.getMetadata();

			/* Check for required modules */
			if (!meta.isBuiltIn()) {
				meta.getDepends().forEach((depId, depVersion) -> {
					if (!ModuleList.getInstance().isModuleLoaded(depId)) {
						missing.add(Strings.MISSING_DEP.resolve(meta.getId(), depId));
					} else {
						FlintModuleMetadata metadata = ModuleList.getInstance().getModuleMeta(depId);
						if (metadata == null)
							return;

						Semver depVer = new Semver(metadata.getVersion(), Semver.SemverType.COCOAPODS);
						if (!depVer.satisfies(depVersion)) {
							String errType = depVer.isGreaterThan(depVersion) ? "greater than" : "less than";
							invalid.add(Strings.WRONG_DEP_VERSION.resolve(meta.getId(), errType, depVersion, depVer.getOriginalValue()));
						}
					}
				});

				/* Check for breaking modules */
				meta.getBreaks().forEach((depid, depVersion) -> {
					if (ModuleList.getInstance().isModuleLoaded(depid)) {
						FlintModuleMetadata metadata = ModuleList.getInstance().getModuleMeta(depid);
						if (metadata == null)
							return;

						Semver depVer = new Semver(metadata.getVersion(), Semver.SemverType.COCOAPODS);
						if (depVer.satisfies(depVersion)) {
							String errType = depVer.isGreaterThan(depVersion) ? "greater than" : "less than";
							invalid.add(Strings.BREAKS.resolve(meta.getId(), errType, depVersion, depVer.getOriginalValue()));
						}
					}
				});
			}
		}

		// TODO - Handle incompatible mod sets!
	}

	/**
	 * Discover declared entry points from modules
	 */
	public static void gatherEntryPoints() {
		for (FlintModuleContainer container : ModuleList.getInstance().allModules()) {
			FlintModuleMetadata metadata = container.getMetadata();
			if (metadata.isBuiltIn() || metadata.getEntryPoints() == null || metadata.getEntryPoints().isEmpty())
				continue;

			metadata.getEntryPoints().forEach((s, k) -> FlintEntryPoints.add(metadata, s, k));
		}
	}

}
