/**
 *  ISYZWaveLock - Handles a ZWave Lock connected to an ISY Hub
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
    definition (name: "ISYZWaveLock", namespace: "rodtoll", author: "Rod Toll") {
        capability "Lock"
        capability "Actuator"
        attribute "isyAddress", "string"
    }

    simulator {
    }
    
	tiles(scale: 2) {
		multiAttributeTile(name:"toggle", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.lock", key: "PRIMARY_CONTROL") {
				attributeState "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
				attributeState "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ff0000", nextState:"locking"
				attributeState "locking", label:'locking', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
				attributeState "unlocking", label:'unlocking', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ff0000", nextState:"locking"
			}
            tileAttribute ("device.isyAddress", key: "SECONDARY_CONTROL") {
            	attributeState "default", label: 'ISY Address: ${currentValue}'
            }              
		}
        
        valueTile("isyAddress", "device.isyAddress", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }       
        
        valueTile("lockState", "device.lock", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }          
		
		main "toggle"
		details(["toggle", "lockState", "isyAddress"])
	}    
}

// Does nothing as the smartapp receives the status messages for the device and updates the devices.
def parse(String description) {
}

def setISYState(address,state) {
	sendEvent(name: 'isyAddress', value: address)
	if(state == ' ') {
    	log.debug 'Correcting empty state for lock device'
    	state = 0
    }
	log.debug 'ISYZWAVELOCK: Switching lock level to: '+state
    def level = parent.scaleISYDeviceLevelToSTRange(state)
    if(level == 0) {
    	sendEvent(name: 'lock', value: 'unlocked')
    } else {
    	sendEvent(name: 'lock', value: 'locked')
    }
}

def lock() {
    log.debug "ISYZWAVELOCK: Executing 'securelock'"
    parent.secureLockCommand(this,device.deviceNetworkId,true)
}

def unlock() {
    log.debug "ISYZWAVELOCK: Executing 'secureunlock'"
    parent.secureLockCommand(this,device.deviceNetworkId,false)
}
