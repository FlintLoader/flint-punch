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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author HypherionSA
 */
public final class FlintModuleMetadata {

	private transient boolean builtIn = false;

	private @NotNull final String id;
	private @NotNull final String version;
	private @NotNull final String name;
	private @Nullable String description;
	private final @NotNull List<String> authors = new ArrayList<>();
	private @Nullable String license;
	private @Nullable String icon;
	private final @NotNull List<String> mixins = new ArrayList<>();
	private @Nullable String accessWidener;
	private @NotNull final HashMap<String, String> contact = new HashMap<>();
	private @NotNull final HashMap<String, String> depends = new HashMap<>();
	private @NotNull final HashMap<String, String> breaks = new HashMap<>();
	private @NotNull final HashMap<String, String> entryPoints = new HashMap<>();

	FlintModuleMetadata(@NotNull String id, @NotNull String name, @NotNull String version) {
		this(id, name, version, false);
	}

	FlintModuleMetadata(@NotNull String id, @NotNull String name, @NotNull String version, boolean builtIn) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.builtIn = builtIn;
	}

	// Getters
	public boolean isBuiltIn() {
		return builtIn || id.equals("flintloader");
	}

	public @NotNull String getId() {
		return id;
	}

	public @NotNull String getVersion() {
		return version;
	}

	public @NotNull String getName() {
		return name;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public @NotNull List<String> getAuthors() {
		return authors;
	}

	@Nullable
	public String getLicense() {
		return license;
	}

	@Nullable
	public String getIcon() {
		return icon;
	}

	public @NotNull List<String> getMixins() {
		return mixins;
	}

	@Nullable
	public String getAccessWidener() {
		return accessWidener;
	}

	public @NotNull HashMap<String, String> getContact() {
		return contact;
	}

	public @NotNull HashMap<String, String> getDepends() {
		return depends;
	}

	public @NotNull HashMap<String, String> getBreaks() {
		return breaks;
	}

	public @NotNull HashMap<String, String> getEntryPoints() {
		return entryPoints;
	}

	@NotNull
	public Optional<String> getEntryPoint(String type) {
		if (entryPoints.containsKey(type)) {
			return Optional.of(entryPoints.get(type));
		}
		return Optional.empty();
	}
}
