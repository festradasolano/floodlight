/**
 * Copyright 2013 Felipe Estrada-Solano <festradasolano at gmail>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.floodlightcontroller.core.web;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.IFloodlightProviderService;

/**
 * Retrieves FloodLight general information: listen address and listen port for
 * switch connections.
 * 
 * Copyright 2013 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0
 * 
 * @author festradasolano
 */
public class ControllerInfoResource extends ServerResource {

	@Get("json")
	public Map<String, Object> retrieve() {
		IFloodlightProviderService floodlightProvider = (IFloodlightProviderService) getContext()
				.getAttributes().get(
						IFloodlightProviderService.class.getCanonicalName());
		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("listenAddress", "*");
		model.put("listenPort", floodlightProvider.getOpenFlowPort());
		// TODO 2013-10-17 festradasolano: more general information can be added
		return model;
	}

}
