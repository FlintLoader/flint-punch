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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import net.flintloader.loader.api.ModuleContainer;
import net.flintloader.loader.modules.resolver.IModuleResolver;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

/**
 * @author HypherionSA
 * Load modules from the modules directory
 */
public class DirectoryModuleResolver implements IModuleResolver {

	private final File runDirectory = new File(".");
	private final File modulesDirectory = new File(runDirectory, "modules");

	@Override
	public void resolve(Map<String, ModuleContainer> outList) {
		Log.info(LogCategory.DISCOVERY, "Discovering Modules in Modules Directory...");
		if (!modulesDirectory.exists()) {
			modulesDirectory.mkdirs();
		}

		if (!Files.isDirectory(modulesDirectory.toPath())) {
			throw new RuntimeException(modulesDirectory.getAbsolutePath() + " is not a directory!");
		}

		try {
			Files.walkFileTree(this.modulesDirectory.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (isValidFile(file)) {
						try (JarFile jarFile = new JarFile(file.toFile())) {

							JarEntry entry = jarFile.getJarEntry("flintmodule.json");
							if (entry != null) {
								readModuleJson(outList, jarFile.getInputStream(entry), Collections.singletonList(file));
								return FileVisitResult.CONTINUE;
							}

							Log.warn(LogCategory.DISCOVERY, "Skipped " + file + " since it does not contain flintmodule.json");
						} catch (ZipException e) {
							Log.warn(LogCategory.DISCOVERY, "Could not read file " + file + " as a jar file", e);
						} catch (Throwable t) {
							Log.error(LogCategory.DISCOVERY, "Exception while checking if file " + file + " is a mod", t);
						}
					}

					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Exception while searching for modules in '" + modulesDirectory + "'!", e);
		}
	}

	static boolean isValidFile(Path path) {
		/*
		 * We only propose a file as a possible mod in the following scenarios:
		 * General: Must be a jar file
		 *
		 * Some OSes Generate metadata so consider the following because of OSes:
		 * UNIX: Exclude if file is hidden; this occurs when starting a file name with `.`
		 * MacOS: Exclude hidden + startsWith "." since Mac OS names their metadata files in the form of `.mod.jar`
		 */

		if (!Files.isRegularFile(path)) return false;

		try {
			if (Files.isHidden(path)) return false;
		} catch (IOException e) {
			Log.warn(LogCategory.DISCOVERY, "Error checking if file %s is hidden", path, e);
			return false;
		}

		String fileName = path.getFileName().toString();

		return fileName.endsWith(".jar") && !fileName.startsWith(".");
	}
}
