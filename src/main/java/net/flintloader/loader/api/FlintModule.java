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
package net.flintloader.loader.api;

/**
 * @author HypherionSA
 */
public interface FlintModule {

	/**
	 * This is called before Minecraft has loaded. Use this to register configs, events etc.
	 * Trying to access game data at this point, will result in a crash!
	 */
	default void earlyInitialization() {}

	/**
	 * This is called when all minecraft variables and systems have loaded, just before the loading screen is shown.
	 * Trying to access game data here is safe
	 */
	void initializeModule();
}
