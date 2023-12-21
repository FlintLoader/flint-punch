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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.flintloader.loader.api.ModuleContainer;
import net.flintloader.loader.api.ModuleOrigin;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.util.FileSystemUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public class ModuleContainerImpl implements ModuleContainer {

	private final FlintModuleMetadata meta;
	private final List<Path> paths;
	private List<Path> rootPaths;
	private final ModuleOrigin origin;

	public ModuleContainerImpl(FlintModuleMetadata meta, List<Path> paths, ModuleOrigin origin) {
		this.meta = meta;
		this.paths = paths;
		this.origin = origin;
	}

	@Override
	public FlintModuleMetadata getMetadata() {
		return meta;
	}

	private static boolean warnedMultiPath = false;

	@Override
	public List<Path> getRootPaths() {
		List<Path> ret = rootPaths;

		if (ret == null || !checkFsOpen(ret)) {
			rootPaths = ret = obtainRootPaths(); // obtainRootPaths is thread safe, but we need to avoid plain or repeated reads to root
		}

		return ret;
	}

	private boolean checkFsOpen(List<Path> paths) {
		for (Path path : paths) {
			if (path.getFileSystem().isOpen()) continue;

			if (!warnedClose) {
				if (!PunchLoaderImpl.INSTANCE.isDevelopmentEnvironment()) warnedClose = true;
				Log.warn(LogCategory.GENERAL, "FileSystem for %s has been closed unexpectedly, existing root path references may break!", this);
			}

			return false;
		}

		return true;
	}

	private boolean warnedClose = false;

	@Override
	public ModuleOrigin getOrigin() {
		return origin;
	}

	@Override
	public Optional<ModuleContainer> getContainingModule() {
		// TODO Support Nested Modules
		return Optional.empty();
	}

	@Override
	public Collection<ModuleContainer> getContainedModules() {
		// TODO Support Nested Modules
		return new ArrayList<>();
	}

	private List<Path> obtainRootPaths() {
		boolean allDirs = true;

		for (Path path : paths) {
			if (!Files.isDirectory(path)) {
				allDirs = false;
				break;
			}
		}

		if (allDirs) return paths;

		try {
			if (paths.size() == 1) {
				return Collections.singletonList(obtainRootPath(paths.get(0)));
			} else {
				List<Path> ret = new ArrayList<>(paths.size());

				for (Path path : paths) {
					ret.add(obtainRootPath(path));
				}

				return Collections.unmodifiableList(ret);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to obtain root directory for mod '" + meta.getId() + "'!", e);
		}
	}

	private static Path obtainRootPath(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			return path;
		} else /* JAR */ {
			FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(path, false);
			FileSystem fs = delegate.get();

			if (fs == null) {
				throw new RuntimeException("Could not open JAR file " + path + " for NIO reading!");
			}

			return fs.getRootDirectories().iterator().next();

			// We never close here. It's fine. getJarFileSystem() will handle it gracefully, and so should mods
		}
	}
}
