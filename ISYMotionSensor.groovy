/**
 *  ISYMotionSensor - Represents an ISY Motion Sensor. Exposes the motion sensor, battery alert and light level.
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
    definition (name: "ISYMotionSensor", namespace: "rodtoll", author: "Rod Toll") {
 		capability "Motion Sensor"
        capability "Actuator"
        capability "Battery"
        capability "Illuminance Measurement"
        attribute "isyAddress", "string"
    }

    simulator {
    }
    
	tiles(scale: 2) {
        multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
            tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
                attributeState "active", label:'${name}', icon:"st.motion.motion.active", backgroundColor:"#ffa81e"
                attributeState "inactive", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#79b821"
            }
            tileAttribute ("device.isyAddress", key: "SECONDARY_CONTROL") {
            	attributeState "default", label: 'ISY Address: ${currentValue} (1-3)'
            }              
        }
        
        standardTile("illuminance", "device.illuminance", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "0", label:'Night', icon: "st.Weather.weather4" ,backgroundColor: "#000000"
            state "99", label: 'Day', icon: "st.Weather.weather14" ,backgroundColor: "#DF7401"
            state "default", label: '${currentValue}', icon: "st.Weather.weather14", backgroundColor: "#ffff00", defaultState: true
        }   
        
        standardTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "10", label: 'Low', icon: "st.Appliances.appliances17" ,backgroundColor: "#ff0000"
            state "100", label: 'OK', icon: "st.Appliances.appliances17" ,backgroundColor: "#088a29"
            state "default", label: '${currentValue}', icon: "st.Appliances.appliances17" ,backgroundColor: "#ffff00", defaultState: true
        }  
        
        valueTile("isyAddress", "device.isyAddress", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }
		
		main "motion"
		details(["motion", "illuminance", "battery", "isyAddress"])
	}    
}

// Does nothing as the smartapp receives the status messages for the device and updates the devices.
def parse(String description) {
}

def setISYState(address,state) {
	if(parent.getSubAddress(address)=='1') {
    	sendEvent(name: 'isyAddress', value: address)
        log.debug 'ISYMOTIONSENSOR: Handling incoming motion sensor set value to: '+state
        if(state == 255) {
            sendEvent(name: 'motion', value: 'active')
        } else {
            sendEvent(name: 'motion', value: 'inactive')
        }
	} else if(parent.getSubAddress(address)=='2'){
    	log.debug 'ISYMOTIONSENSOR: Handling incoming motion sensor light value to: '+state
        def newIlluminance = 0
        if(state != ' ') {
        	newIlluminance = parent.scaleISYDeviceLevelToSTRange(state)
        }
        sendEvent(name: 'illuminance', value: newIlluminance)
	} else if(parent.getSubAddress(address)=='3'){
    	log.debug 'ISYMOTIONSENSOR: Handling incoming motion sensor battery value to: '+state
        if(state == 0 || state == ' ') {
        	sendEvent(name: 'battery', value: 100)
        } else {
        	sendEvent(name: 'battery', value: 10)
        }    
    } else {
    	log.debug 'ISYMOTIONSENSOR: Ignoring unknown sub-device of motion sensor. Address='+address
    }
}
