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
package net.flintloader.loader.modules.resolver.impl;

import java.io.File;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.resolver.IModuleResolver;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public class DirectoryModuleResolver implements IModuleResolver {

	private final File runDirectory = new File(".");
	private final File modulesDirectory = new File(runDirectory, "modules");

	@Override
	public void resolve(Map<String, FlintModuleMetadata> outList) {
		Log.info(LogCategory.DISCOVERY, "Discovering Modules in Modules Directory...");
		if (!modulesDirectory.exists()) {
			modulesDirectory.mkdirs();
		}

		File[] modules = modulesDirectory.listFiles();
		if (modules != null) {
			for (File file : modules) {
				if (!file.getName().endsWith(".jar")) continue;

				try (JarFile jarFile = new JarFile(file)) {
					if (!file.isFile()) continue;

					JarEntry entry = jarFile.getJarEntry("flintmodule.json");
					if (entry != null) {
						readModuleJson(outList, jarFile.getInputStream(entry), file.toPath());
						continue;
					}

					Log.warn(LogCategory.DISCOVERY, "Skipped " + file + " since it does not contain flintmodule.json");
				} catch (ZipException e) {
					Log.warn(LogCategory.DISCOVERY, "Could not read file " + file + " as a jar file", e);
				} catch (Throwable t) {
					Log.error(LogCategory.DISCOVERY, "Exception while checking if file " + file + " is a mod", t);
				}
			}
		}
	}
}
