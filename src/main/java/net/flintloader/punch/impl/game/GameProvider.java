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
package net.flintloader.punch.impl.game;

import java.nio.file.Path;

import net.flintloader.punch.impl.game.patch.GameTransformer;
import net.flintloader.punch.impl.launch.PunchLauncher;
import net.flintloader.punch.impl.util.Arguments;
import net.flintloader.punch.impl.util.LoaderUtil;

public interface GameProvider {
	String getGameId();
	String getGameName();
	String getRawGameVersion();
	String getNormalizedGameVersion();

	String getEntrypoint();
	Path getLaunchDirectory();
	boolean isObfuscated();
	boolean requiresUrlClassLoader();

	boolean isEnabled();
	boolean locateGame(PunchLauncher launcher, String[] args);
	void initialize(PunchLauncher launcher);
	GameTransformer getEntrypointTransformer();
	void unlockClassPath(PunchLauncher launcher);
	void launch(ClassLoader loader);

	default boolean displayCrash(Throwable exception, String context) {
		return false;
	}

	Arguments getArguments();
	String[] getLaunchArguments(boolean sanitize);

	default boolean canOpenErrorGui() {
		return true;
	}

	default boolean hasAwtSupport() {
		return LoaderUtil.hasAwtSupport();
	}
}
