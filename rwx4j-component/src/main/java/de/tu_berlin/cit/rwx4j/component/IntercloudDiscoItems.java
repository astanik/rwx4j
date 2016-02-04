/**
 * Copyright 2010-2015 Complex and Distributed IT Systems, TU Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tu_berlin.cit.rwx4j.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 * 
 * @author Alexander Stanik <alexander.stanik@tu-berlin.de>
 */
public class IntercloudDiscoItems {

	private final Map<String, String> items;
//	private String rootJID;
	
//	private List<String> exchangeJIDs;
	
//	private List<String> gatewayJIDs;
	
	protected IntercloudDiscoItems() {
		this(new HashMap<String, String>());
	}
	
	protected IntercloudDiscoItems(Map<String, String> items) {
		this.items = items;
//		this.exchangeJIDs = exchangeJIDs;
//		this.gatewayJIDs = gatewayJIDs;
	}

	public void addItem(String jid, String name) {
		this.items.put(jid, name);
	}
	
	public Map<String, String> getItems() {
		return this.items;
	}
/*
	public String getRootJID() {
		if(this.rootJID == null)
			return RootServerWhitelist.list.get(0);
		else
			return rootJID;
	}

	public void setRootJID(String rootJID) {
		this.rootJID = rootJID;
	}

	public List<String> getExchangeJIDs() {
		return exchangeJIDs;
	}

	public void setExchangeJIDs(List<String> exchangeJIDs) {
		this.exchangeJIDs = exchangeJIDs;
	}

	public List<String> getGatewayJIDs() {
		return gatewayJIDs;
	}

	public void setGatewayJIDs(List<String> gatewayJIDs) {
		this.gatewayJIDs = gatewayJIDs;
	}
*/
}
