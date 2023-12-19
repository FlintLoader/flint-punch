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

/**
 * @author HypherionSA
 * @date 27/06/2022
 */
public enum Strings {
	MISSING_DEP("Module %s requires module %s, but it's not installed"),
	WRONG_DEP_VERSION("Module %s requires module %s version %s %s, but version (%s) is installed"),
	BREAKS("Module %s breaks module %s version %s %s. Version (%s) is installed");

	private final String message;

	Strings(String message) {
		this.message = message;
	}

	public String resolve(Object... args) {
		return String.format(this.message, args);
	}

	@Override
	public String toString() {
		return this.message;
	}
}
