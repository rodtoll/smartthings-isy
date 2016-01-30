/**
 *  ISYOnOffLight - Handles a light device which can be turned on or off (non-dimmable)
 *
 *  Copyright 2016 Rod Toll
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
  *
 */

metadata {
    definition (name: "ISYOnOffLight", namespace: "rodtoll", author: "Rod Toll") {
        capability "Switch"
        capability "Actuator"
        attribute "isyAddress", "string"
    }

    simulator {
    }
    
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Lighting.light13", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Lighting.light13", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.Lighting.light13", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.Lighting.light13", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            tileAttribute ("device.isyAddress", key: "SECONDARY_CONTROL") {
            	attributeState "default", label: 'ISY Address: ${currentValue}'
            }              
		}
        
        valueTile("isyAddress", "device.isyAddress", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }        
		
		main "switch"
		details(["switch","isyAddress"])
	}    
}

// Does nothing as the smartapp receives the status messages for the device and updates the devices.
def parse(String description) {
}

def setISYState(address, state) {
	sendEvent(name: 'isyAddress', value: address)
	if(state == ' ') {
    	log.debug 'Correcting empty state for on off light device'
    	state = 0
    }
	log.debug 'ISYONOFFLIGHT: Incoming switching dim level to: '+level
    def level = parent.scaleISYDeviceLevelToSTRange(state)
    if(level == 0) {
    	sendEvent(name: 'switch', value: 'off')
    } else {
    	sendEvent(name: 'switch', value: 'on')
    }
}

// handle commands
def on() {
    log.debug "ISYONOFFLIGHT: Executing 'on'"
    parent.on(this,device.deviceNetworkId)
}

def off() {
    log.debug "ISYONOFFLIGHT: Executing 'off'"
    parent.off(this,device.deviceNetworkId)
}
