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
package net.flintloader.loader.modules.resolver.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.flintloader.loader.api.ModuleContainer;
import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleContainerImpl;
import net.flintloader.loader.modules.resolver.IModuleResolver;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.SystemProperties;
import net.flintloader.punch.impl.util.UrlConversionException;
import net.flintloader.punch.impl.util.UrlUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

/**
 * @author HypherionSA
 * Load mods from classpath during development
 */
public final class ClassPathModuleResolver implements IModuleResolver {

	@Override
	public void resolve(Map<String, ModuleContainer> outList) {
		if (PunchLauncherBase.getLauncher().isDevelopment()) {
			Log.info(LogCategory.DISCOVERY, "Discovering Modules in Classpath...");
			Map<Path, List<Path>> pathGroups = getPathGroups();

			try {
				Enumeration<URL> urlEnumeration = PunchLauncherBase.getLauncher().getTargetClassLoader().getResources("flintmodule.json");

				while (urlEnumeration.hasMoreElements()) {
					URL url = urlEnumeration.nextElement();

					try {
						Path path = LoaderUtil.normalizeExistingPath(UrlUtil.getCodeSource(url, "flintmodule.json"));
						List<Path> paths = pathGroups.get(path);
						InputStream in = url.openStream();

						if (paths == null) {
							readModuleJson(outList, in, Collections.singletonList(path));
						} else {
							readModuleJson(outList, in, paths);
						}

					} catch (UrlConversionException e) {
						Log.debug(LogCategory.DISCOVERY, "Error determining location for flintmodule.json from %s", url, e);
					}
				}

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Map<Path, List<Path>> getPathGroups() {
		String prop = System.getProperty(SystemProperties.PATH_GROUPS);
		if (prop == null) return Collections.emptyMap();

		Set<Path> cp = new HashSet<>(PunchLauncherBase.getLauncher().getClassPath());
		Map<Path, List<Path>> ret = new HashMap<>();

		for (String group : prop.split(File.pathSeparator+File.pathSeparator)) {
			Set<Path> paths = new LinkedHashSet<>();

			for (String path : group.split(File.pathSeparator)) {
				if (path.isEmpty()) continue;

				Path resolvedPath = Paths.get(path);

				if (!Files.exists(resolvedPath)) {
					Log.debug(LogCategory.DISCOVERY, "Skipping missing class path group entry %s", path);
					continue;
				}

				resolvedPath = LoaderUtil.normalizeExistingPath(resolvedPath);

				if (cp.contains(resolvedPath)) {
					paths.add(resolvedPath);
				}
			}

			if (paths.size() < 2) {
				Log.debug(LogCategory.DISCOVERY, "Skipping class path group with no effect: %s", group);
				continue;
			}

			List<Path> pathList = new ArrayList<>(paths);

			for (Path path : pathList) {
				ret.put(path, pathList);
			}
		}

		return ret;
	}
}
