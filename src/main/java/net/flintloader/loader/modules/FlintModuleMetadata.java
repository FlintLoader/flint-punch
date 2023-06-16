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
package net.flintloader.loader.modules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author HypherionSA
 * @date 21/06/2022
 */
public class FlintModuleMetadata {

	private transient boolean builtIn = false;
	private transient Path source;

	private String id;
	private String version;
	private String name;
	private String description;
	private List<String> authors;
	private String license;
	private String icon;
	private List<String> mixins;
	private String accessWidener;

	private String entryPoint;
	private HashMap<String, String> contact = new HashMap<>();
	private HashMap<String, String> depends = new HashMap<>();
	private HashMap<String, String> breaks = new HashMap<>();

	FlintModuleMetadata(String id, String name, String version) {
		this(id, name, version, false);
	}

	FlintModuleMetadata(String id, String name, String version, boolean builtIn) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.builtIn = builtIn;
	}

	// Getters
	public boolean isBuiltIn() {
		return builtIn || id.equals("flintloader");
	}

	public String getId() {
		return id == null ? "" : id;
	}

	public String getVersion() {
		return version == null ? "0.0.0" : version;
	}

	public String getName() {
		return name == null ? "Unknown Module" : name;
	}

	public String getDescription() {
		return description == null ? "" : description;
	}

	public List<String> getAuthors() {
		if (authors == null) {
			return new ArrayList<>();
		}
		return authors;
	}

	public String getLicense() {
		return license == null ? "" : license;
	}

	public String getIcon() {
		return icon == null ? "" : icon;
	}

	public List<String> getMixins() {
		if (mixins == null) {
			return new ArrayList<>();
		}
		return mixins;
	}

	public String getAccessWidener() {
		return accessWidener == null ? "" : accessWidener;
	}

	public HashMap<String, String> getContact() {
		if (contact == null) {
			return new HashMap<>();
		}
		return contact;
	}

	public HashMap<String, String> getDepends() {
		if (depends == null) {
			return new HashMap<>();
		}
		return depends;
	}

	public HashMap<String, String> getBreaks() {
		if (breaks == null) {
			return new HashMap<>();
		}
		return breaks;
	}

	public Path getSource() {
		return source;
	}

	public void setSource(Path path) {
		this.source = path;
	}

	public String getEntryPoint() {
		return entryPoint;
	}

	public Optional<Path> findPath(String file) {
		Path path = getSource().resolve(file.replace("/", getSource().getFileSystem().getSeparator()));
		if (Files.exists(path)) return Optional.of(path);
		return Optional.empty();
	}
}
