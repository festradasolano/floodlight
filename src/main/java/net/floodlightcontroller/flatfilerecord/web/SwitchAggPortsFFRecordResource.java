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

package net.floodlightcontroller.flatfilerecord.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.flatfilerecord.ISwitchAggPortsFFRecordService;

import org.openflow.util.HexString;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Retrieves switch aggregate port stats that are recorded in a flat file.
 * 
 * Copyright 2013 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0
 * 
 * @author festradasolano
 */
public class SwitchAggPortsFFRecordResource extends ServerResource {

	/**
	 * Returns per switch a list of last requested aggregate ports stats. This
	 * includes: Received/transmitted packets; Received/transmitted bytes;
	 * Received/transmitted dropped packets; Received/transmitted packets with
	 * error; Received packets with frame error; Received packets with overrun
	 * error; Received packets with CRC error; Collisions.
	 * 
	 * @return
	 */
	@Get("json")
	public Map<String, List<Map<String, Object>>> retrieve() {
		// get parameters
		String switchId = (String) getRequestAttributes().get("switchId");
		String lastRecords = (String) getRequestAttributes().get("lastRecords");
		// get and verify flat file path
		ISwitchAggPortsFFRecordService switchAggPortStatsFFRecord = (ISwitchAggPortsFFRecordService) getContext()
				.getAttributes()
				.get(ISwitchAggPortsFFRecordService.class.getCanonicalName());
		String filePath = switchAggPortStatsFFRecord
				.getFlatFileRecordPath(HexString.toLong(switchId));
		if (filePath == null) {
			return null;
		}
		// read file in a list
		List<String> records;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			records = new ArrayList<String>();
			String l;
			while ((l = reader.readLine()) != null) {
				records.add(l);
			}
			reader.close();
		} catch (IOException e) {
			return null;
		}
		// slide list to return last records requested
		int _lastRecords = Integer.parseInt(lastRecords);
		if (records.size() > _lastRecords) {
			int actualSize = records.size();
			for (int i = 0; i < actualSize - _lastRecords; i++) {
				records.remove(0);
			}
		}
		// build and return json data
		HashMap<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> listPortStats = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < records.size(); i++) {
			HashMap<String, Object> portStats = new HashMap<String, Object>();
			String log = records.get(i);
			int j = 0;
			while (j < log.length()) {
				String key = log.substring(j, log.indexOf("=", j));
				j = log.indexOf("=", j) + 1;
				String value = log.substring(j, log.indexOf("|", j));
				j = log.indexOf("|", j) + 1;
				portStats.put(key, value);
			}
			listPortStats.add(portStats);
		}
		result.put(switchId, listPortStats);
		return result;
	}

}
