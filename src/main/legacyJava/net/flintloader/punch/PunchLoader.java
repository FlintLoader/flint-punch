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
package net.flintloader.punch;

import java.io.File;

import net.flintloader.punch.impl.PunchLoaderImpl;

/**
 * The main class for mod loading operations.
 *
 * @deprecated Use {@link net.flintloader.punch.api.PunchLoader}
 */
@Deprecated
public abstract class PunchLoader implements net.flintloader.punch.api.PunchLoader {
	/**
	 * @deprecated Use {@link net.flintloader.punch.api.PunchLoader#getInstance()} where possible,
	 * report missing areas as an issue.
	 */
	@Deprecated
	public static final PunchLoader INSTANCE = PunchLoaderImpl.InitHelper.get();

	public File getModulesDirectory() {
		return getGameDir().resolve("modules").toFile();
	}
}
