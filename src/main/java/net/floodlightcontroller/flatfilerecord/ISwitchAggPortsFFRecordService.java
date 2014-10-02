/**
 * Copyright 2013-2014 Felipe Estrada-Solano <festradasolano at gmail>
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

package net.floodlightcontroller.flatfilerecord;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * The interface exposed by the record bundle that allows to get the path to
 * flat files that contain switches aggregate port stats records.
 * 
 * Copyright 2013-2014 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0
 * 
 * @author festradasolano
 */
public interface ISwitchAggPortsFFRecordService extends IFloodlightService {

	/**
	 * Returns the path of flat file that contains switch aggregate port stats
	 * records.
	 * 
	 * @param switchDpid
	 *            Switch DPID to get path of the flat file that contains its
	 *            aggregate port stats records
	 * @return The path of flat file that contains switch aggregate port stats
	 *         records
	 */
	public String getFlatFileRecordPath(long switchDpid);

}
