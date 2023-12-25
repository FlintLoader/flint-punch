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
package net.flintloader.punch.impl.game.minecraft;

import java.io.File;

import net.flintloader.loader.api.FlintModule;
import net.flintloader.loader.core.PunchLauncherHooks;
import net.flintloader.loader.core.entrypoints.FlintEntryPoints;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public final class Hooks {
	public static final String INTERNAL_NAME = Hooks.class.getName().replace('.', '/');

	public static String appletMainClass;

	public static final String FLINT = "flint";
	public static final String VANILLA = "vanilla";

	public static String insertBranding(final String brand) {
		if (brand == null || brand.isEmpty()) {
			Log.warn(LogCategory.GAME_PROVIDER, "Null or empty branding found!", new IllegalStateException());
			return FLINT;
		}

		return VANILLA.equals(brand) ? FLINT : brand + ',' + FLINT;
	}

	public static void startClient(File runDir, Object gameInstance) {
		if (runDir == null) {
			runDir = new File(".");
		}

		PunchLoaderImpl.INSTANCE.prepareModuleInit(runDir.toPath(), gameInstance);
		FlintEntryPoints.invoke("main", FlintModule.class, FlintModule::initializeModule);
	}

	public static void setGameInstance(Object gameInstance) {
		PunchLoaderImpl.INSTANCE.setGameInstance(gameInstance);
	}
}
