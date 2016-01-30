/**
 *  ISYFanLinc - Represents just the fan motor of a FanLinc connected to an ISY Hub. 
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
    definition (name: "ISYFanLinc", namespace: "rodtoll", author: "Rod Toll") {
        capability "Switch"
        capability "Switch Level"
        capability "Actuator"
        attribute "fanSpeed", "enum", ["Off", "Low", "Medium", "High"]
        command "setLowSpeed"
        command "setHighSpeed"
        command "setMediumSpeed"    
        attribute "isyAddress", "string"
    }

    simulator {
    }
    
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Appliances.appliances11", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Appliances.appliances11", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.Appliances.appliances11", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.Appliances.appliances11", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
            tileAttribute ("device.fanSpeed", key: "SECONDARY_CONTROL") {
            	attributeState "default", label: 'Fan Speed: ${currentValue}'
            }
		}
        
        standardTile("lowSpeed", "device.fanSpeed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "Low", label:'Low', action:"off", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Off", label:'Low', action:"setLowSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "High", label:'Low', action:"setLowSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "Medium", label:'Low', action:"setLowSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
        }
        standardTile("mediumSpeed", "device.fanSpeed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "Medium", label:'Medium', action:"off", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Off", label:'Medium', action:"setMediumSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "Low", label:'Medium', action:"setMediumSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "High", label:'Medium', action:"setMediumSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
        }
        
        standardTile("highSpeed", "device.fanSpeed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "High", label:'High', action:"off", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Off", label:'High', action:"setHighSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "Low", label:'High', action:"setHighSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "Medium", label:'High', action:"setHighSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
        }    
        
        standardTile("currentState", "device.fanSpeed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "High", label:'High', action:"off", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Medium", label:'Medium', action:"setHighSpeed", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Low", label:'Low', action:"setMediumSpeed", backgroundColor:"#79b821", icon:"st.Appliances.appliances11"
            state "Off", label:'Off', action:"setLowSpeed", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
            state "default", label:'High', action:"off", backgroundColor:"#ffffff", icon:"st.Appliances.appliances11"
        }    
        
        valueTile("isyAddress", "device.isyAddress", decoration: "Flat", inactiveLabel: false, width: 2, height: 2) {
        	state "default", label: '${currentValue}', defaultState: true
        }        
		
		main "currentState"
		details(["switch","lowSpeed","mediumSpeed","highSpeed", "isyAddress"])
	}    
}

// Does nothing as the smartapp receives the status messages for the device and updates the devices.
def parse(String description) {
}

def setISYState(address,state) {
	sendEvent(name: "isyAddress", value: address)
	if(state == ' ') {
    	log.debug 'Correcting empty state for fan device'
    	state = 0
    }
    if(state == 0) {
    	log.debug "ISYFANLINC: Locking fanSpeed to Off and level to 0"
    	sendEvent(name: "level", value: 0)
        sendEvent(name: "fanSpeed", value: "Off")
    } else if(state <= 63) {
    	log.debug "ISYFANLINC: Locking fanSpeed to Low and level to 63 setting to ST level 33"
    	sendEvent(name: "level", value: 33)
        sendEvent(name: "fanSpeed", value: "Low")
    } else if(state <= 191) {
       	log.debug "ISYFANLINC: Locking fanSpeed to Medium and level to 191 setting ST level 66"
    	sendEvent(name: "level", value: 66)
        sendEvent(name: "fanSpeed", value: "Medium")
    } else {
      	log.debug "ISYFANLINC: Locking fanSpeed to High and level to 255 setting ST level 99"
    	sendEvent(name: "level", value: 99)
        sendEvent(name: "fanSpeed", value: "High")
    }
    if(state == 0) {
    	sendEvent(name: 'switch', value: 'off')
    } else {
    	sendEvent(name: 'switch', value: 'on')
    }    
    
}


// handle commands
def on() {
    log.debug "ISYFANLINC: Executing 'on'"
    parent.on(this, device.deviceNetworkId)
}

def off() {
    log.debug "ISYFANLINC: Executing 'off'"
    parent.off(this, device.deviceNetworkId)
}

def getFanLevelFromST(sourceValue) {
	if(sourceValue == 0) {
    	return "Off"
    } else if(sourceValue <= 33) {
    	return "Low"
    } else if(sourceValue <= 66) {
    	return "Medium"
    } else {
    	return "High"
    }
}

def roundSTValueToLockedValue(sourceValue) {
	if(sourceValue == 0) {
    	return 0
    } else if(sourceValue <= 33) {
    	return 33
    } else if(sourceValue <= 66) {
    	return 66
    } else {
    	return 99
    }
}

def getISYValueFromLockedSTValue(sourceValue) {
	if(sourceValue == 0) {
    	return 0
    } else if(sourceValue <= 33) {
    	return 63
    } else if(sourceValue <= 66) {
    	return 191
    } else {
    	return 255
    }
}

def setLevel(number) {
    def newValue = getFanLevelFromST(number)
	def oldValue = device.currentValue("fanSpeed")
    def roundedNewValue = roundSTValueToLockedValue(number)

	log.debug "ISYFANLINC: Setting Level (from slider): Old Value: "+oldValue+" New Value: "+newValue+" Source: "+number+" Rounded ST: "+roundedNewValue

	// Need to do this local correction because we won't be changing the remote fan state with this
    // update and so therefore we won't get a callback which won't let us clamp the value
	if(newValue == oldValue) {
    	log.debug "ISYFANLINC: Executing local correction to scale to right local value."
    	return createEvent(name: "level", value: roundedNewValue)
    } else {
    	parent.setDimNoTranslation(this,getISYValueFromLockedSTValue(roundedNewValue),device.deviceNetworkId)
    }
}

def setLowSpeed() {
	setLevel(33)
}

def setHighSpeed() {
	setLevel(99)
}

def setMediumSpeed() {
	setLevel(66)
}
