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
package net.flintloader.loader.core.entrypoints;

import net.flintloader.loader.modules.FlintModuleMetadata;

public class EntryPointContainer<T> implements EntryPointHolder<T> {

	private final String key;
	private final Class<T> type;
	private final FlintEntryPoints.Entry entry;
	private T instance;

	public EntryPointContainer(String key, Class<T> type, FlintEntryPoints.Entry entry) {
		this.key = key;
		this.type = type;
		this.entry = entry;
	}

	/**
	 * Create EntrypointContainer without lazy init.
	 */
	public EntryPointContainer(FlintEntryPoints.Entry entry, T instance) {
		this.key = null;
		this.type = null;
		this.entry = entry;
		this.instance = instance;
	}

	@SuppressWarnings("deprecation")
	@Override
	public synchronized T getEntryPoint() {
		if (instance == null) {
			try {
				instance = entry.getOrCreate(type);
				assert instance != null;
			} catch (Exception ex) {
				throw new EntrypointException(key, getProvider().getId(), ex);
			}
		}

		return instance;
	}

	@Override
	public FlintModuleMetadata getProvider() {
		return entry.getModuleContainer();
	}
}
