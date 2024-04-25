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
package net.flintloader.punch.impl.launch.punch;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class MixinServicePunchBootstrap implements IMixinServiceBootstrap {
	@Override
	public String getName() {
		return "Punch";
	}

	@Override
	public String getServiceClassName() {
		return "net.flintloader.punch.impl.launch.punch.MixinServicePunch";
	}

	@Override
	public void bootstrap() {
		// already done in Punch
	}
}
