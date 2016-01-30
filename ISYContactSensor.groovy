/**
 *  ISYContactSensor - Represents an ISY contact sensor. Exposes the contact sensor as well as the low battery indicator. 
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
    definition (name: "ISYContactSensor", namespace: "rodtoll", author: "Rod Toll") {
 		capability "Contact Sensor"
        capability "Actuator"
        capability "Battery"
        attribute "isyAddress", "string"
    }

    simulator {
    }
    
	tiles(scale: 2) {
        multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
            tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
                attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
            }
            tileAttribute ("device.isyAddress", key: "SECONDARY_CONTROL") {
            	attributeState "default", label: 'ISY Address: ${currentValue} (1-2)'
            }              
        }
        
        valueTile("isyAddress", "device.isyAddress", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }        
        
        standardTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "10", label: 'Low', icon: "st.Appliances.appliances17" ,backgroundColor: "#ff0000"
            state "100", label: 'OK', icon: "st.Appliances.appliances17" ,backgroundColor: "#088a29"
            state "default", label: '${currentValue}', icon: "st.Appliances.appliances17" ,backgroundColor: "#ffff00", defaultState: true
        }         
		
		main "contact"
		details(["contact", "battery", "isyAddress"])
	}    
}

// Does nothing as the smartapp receives the status messages for the device and updates the devices.
def parse(String description) {
}

def setISYState(address,state) {
	if(parent.getSubAddress(address)=='1') {
    	sendEvent(name: 'isyAddress', value: address)
        log.debug 'ISYCONTACTSENSOR: Handling incoming contact sensor set value to: '+state
        if(state == 0 || state == ' ') {
            sendEvent(name: 'contact', value: 'closed')
        } else {
            sendEvent(name: 'contact', value: 'open')
        }
    } else {
    	log.debug 'ISYCONTACTSENSOR: Handling incoming contact sensor battery value to: '+state
        if(state == 0 || state == ' ') {
        	sendEvent(name: 'battery', value: 100)
        } else {
        	sendEvent(name: 'battery', value: 10)
        }
    }
}
