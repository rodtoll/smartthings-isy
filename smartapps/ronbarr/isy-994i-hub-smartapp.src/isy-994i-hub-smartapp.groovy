/**
 *  SmartApp for Intergrating ISY-994i Hub into SmartThings. 
 *
 *  Copyright 2016 Rod Toll + Ron Barr
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
 */
definition(
    name: "ISY 994i Hub SmartApp",
    namespace: "ronbarr",
    author: "Rod Toll + Ron Barr",
    description: "Enables ISY 994i Hub to be controlled from SmartThings. Includes notifications so changes to devices are reflected in SmartThings immediately",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("ISY Configuration") {
            input "isyAddress", "text", title: "ISY Address", required: false, defaultValue: "192.168.7.109"  			// Address of the ISY Hub
            input "isyPort", "number", title: "ISY Port", required: false, defaultValue: 80						// Port to use for the ISY Hub
            input "isyUserName", "text", title: "ISY Username", required: false, defaultValue: "admin"				// Username to use for the ISY Hub
            input "isyPassword", "text", title: "ISY Password", required: false, defaultValue: "admin"			// Password to use for the ISY Hub
            input "bridgeAddress", "text", title: "Bridge Address", required: false, defaultValue: "192.168.7.109"		// Address of the bridge
            input "bridgePort", "text", title: "Bridge Port", required: false, defaultValue: 80					// Port of the bridge
            input "bridgeUserName", "text", title: "Bridge Username", required: false, defaultValue: "admin"		// Username to use with the bridge
            input "bridgePassword", "text", title: "Bridge Password", required: false, defaultValue: "admin"		// Password to use for the bridge
			input(name: "includeScenes", type: "bool", title: "Include Scenes", defaultValue: true)    				// Includes scenes as devices
            }
}

def setup() {
    atomicState.rawNodeList = []
    atomicState.deviceDetailsList = [:]
    atomicState.deviceGetDetailsIndex = 0;
	initialize()
}

def installed() {
	setCurrentLoadState("Startup")
	setup()
}

def updated() {
	setCurrentLoadState("Updating")
    unsubscribe()
 	setup()   
	log.debug "ISYSMARTAPP: Updated with settings: ${settings}"
}

private getAuthorization() {
    def userpassascii = settings.isyUserName + ":" + settings.isyPassword
    "Basic " + userpassascii.encodeAsBase64().toString()
}

private getMainAddress(sourceAddress) {
	def addressComponents = sourceAddress.split(' ')
    if(addressComponents.size() > 3) {
    	return addressComponents[0]+' '+addressComponents[1]+' '+addressComponents[2]
    } else {
    	return sourceAddress;
    }
}

private getSubAddress(sourceAddress) {
	def addressComponents = sourceAddress.split(' ')
    if(addressComponents.size() > 3) {
    	return addressComponents[3]
    } else {
    	return '1';
    }
}

def findDevice(address) {
 	log.debug 'ISYSMARTAPP: findDevice with address: '+address
    def device = getChildDevice(address)
    if(!device) {
        device = getChildDevice(getMainAddress(incomingAddress))
    }
    return deviceToUpdate
}

def handleInitialStatusMessage(xml) {
    log.debug 'ISYSMARTAPP: Processing status response'

    def newNodes = []
    int deviceCount = 0

	if (false) then {
	xml.node.each {
        def attributeMap = it.attributes()
        String nodeAddress = attributeMap['id'].toString
        if (!nodeAddress) {
        	nodeAddress = it.address
        }
        if(nodeAddress && (!atomicState.rawNodeList.contains(nodeAddress))) {
            def propValue = attributeMap['value']
            newNodes << nodeAddress
            log.debug 'ISYSMARTAPP: Discovered node of address: '+nodeAddress
            deviceCount++
        }else {
       		 log.debug 'did not add ' + it + 'at address' + nodeAddress
        }
    }
    log.debug 'ISYSMARTAPP: adding ' + sourceNodeList.size() + ' devices'
    }
      
	if(includeScenes) {
    	log.debug 'ISYSMARTAPP: Adding scenes from xml =' + xml.group
   		xml.group.each {
            def nodeAddress = it.address.toString()
   	   		log.debug 'ISYSMARTAPP: Adding group='+it.name+' address='+nodeAddress

            if (nodeAddress) {
   	   		log.debug 'ISYSMARTAPP: Adding group='+it.name+' address='+nodeAddress
            if(!atomicState.rawNodeList.contains(nodeAddress)) {
 	        newNodes << nodeAddress
            log.debug 'ISYSMARTAPP: Discovered group of address: '+nodeAddress
            deviceCount++
            }
       }
     }
    }
    
    log.debug 'ISYSMARTAPP: Device address list generated. Potential devices: '+ String.valueOf(newNodes.size())
    int i=0
    newNodes.each {
    i++
 		log.debug i+'address '+it
    }
    atomicState.rawNodeList = newNodes
}

def handleStatusMessage(xml) {
    log.debug 'ISYSMARTAPP: Processing status response for xml'+xml.name()
    if(xml.node.count) {
    xml.node.each {
        def attributeMap = it.attributes()
        String nodeAddress = attributeMap['id']
        // This is a refresh
        def device = findDevice(nodeAddress) 
        if(device != null) {
            def value = it.property.attributes()['value'].toString()
            if(!value.isInteger()) {
                log.debug "Setting value which is not an int..."+value
                device.setISYState(nodeAddress,value)
            } else {
                log.debug "Setting value whicvh is an int..."+value
                device.setISYState(nodeAddress,0)
            }
        } else {
           log.debug 'Not considering ' + it.attributes
        }
    }
    }
}

def getIsyType(devType, address, uom) {
	log.debug 'ISYSMARTAPP: getISYType, devType '+xml+' address '+address+' uom '+uom
	if(address == null || address.split(' ').size() < 4) {
        log.debug "ISYSMARTAPP: Invalid address, exiting early"
    	return "Unknown"
    }
        	def devTypePieces = devType.split('\\.')
    def mainType = devTypePieces[0].toInteger()
    def subType = devTypePieces[1].toInteger()
    def subAddress = address.split(' ')[3].toInteger()
    
    log.debug 'ISYSMARTAPP: Determining type... main='+mainType+' subType='+subType+' subAddress='+subAddress
    
    // Dimmable Devices
    if(mainType == 1) {
    	if(subType == 46 && subAddress == 2) {
        	return 'F'
        } else {
        	return 'DL'
        }
    // Non-Dimmable
    } else if(mainType == 2) {
    	// ApplianceLinc
    	if(subType == 6 || subType == 9 || subType == 12 || subType == 23) {
        	return "O"
        // Outlet Linc
        } else if(subType == 8 || subType == 33) {
        	return "O"
        // Dual Outlets
		} else if(subType == 57) {
        	return "O"
        } else {
        	return "L"
        }
    // Sensors
    } else if(mainType == 7) {
    	// I/O Lincs
    	if(subType == 0) {
        	// Sensor
            return "IO"
        } else {
        	return "U"
        }
    // Door Locks
    } else if(mainType == 15) {
        // MorningLinc positive to lock device
    	if(subType == 6 && subAddress == 1) {
        	return "ML"
        } else {
        	return "U"
        }
    // More sensors...
    } else if(mainType == 16) {
    	// Motion sensors
    	if(subType == 1 || subType == 3) {
        	return "IM"
        } else if(subType == 2 || subType == 9 || subType == 17) {
        	return "CS"
        }
        return "U"
    } else {
    	return "U"
    }
}

def findHub() {
    def savedIndex = 0
    
	for (def i = 0; i < location.hubs.size(); i++) {
        def hub = location.hubs[i]
        if(hub.type.toString() == "PHYSICAL") {
        	savedIndex = i
        }
    }
    
    log.debug "ISYSMARTAPP: Picking hub: "+savedIndex
    
    atomicState.hubIndex = savedIndex
}

def getZWaveType(nodeXml) {
	def zWaveSubType = nodeXml?.devtype?.cat?.toString().toInteger()
    if(zWaveSubType != null) {
    	if(zWaveSubType == 111) {
        	return "ZL"
        }
    } else {
    	return "U"
    }
}

// When we get the response from the node info request create the device
def handleNodeInfoMessage(xml) {
    // Parse out device details
    log.debug 'ISYSMARTAPP: handleNodeInfoMessage - ' + xml
    if (xml.node) {
       log.debug 'ISYSMARTAPP: node= ' + xml.node
    }
    if (xml.group) {
       log.debug 'ISYSMARTAPP: group =' + xml.group
    }
    // Core values common to devices and scenes
    String address
	String name 
    
    //Device properties, not relevant for scenes. We'll fake 'em for scenes
    String devType
    Boolean enabled = true
    def propNode
    String stateValue
    String formattedValue
    String uomValue
   
    if(xml.group) {
    	if (includeScenes) {
    	 	address = xml.group.address.toString()
         	name = xml.group.name.toString()
  		 	log.debug 'ISYSMARTAPP:  adding group name='+name+' address='+address
        	 // Faking a group as a Smartlinc dimmer
     	 	devType = '1.34.65.0'
    	 	enabled =  true    
        	stateValue = '0'
         	formattedValue = 'Off'
         	uomValue = "%/on/off"
         } else {
            enabled = false
       }
    } else if (xml.node.address) {
    	 address = xml.node.address.toString()
         name = xml.node.name.toString()
  		 log.debug 'ISYSMARTAPP:  adding device'  + it
    	 devType = xml.node.type.toString()
    	 enabled = xml.node.enabled.toString().toBoolean()    
    	 propNode = xml.node.'property' 
   		 if(propNode.size() == 0) {
    		log.debug "ISYSMARTAPP: Skipping node without property. Not supported"
        	enabled = false;
         } else {
 	       	def propAttributes = propNode[0].attributes()
        	stateValue = propAttributes['value'].toString()
        	formattedValue = propAttributes['formatted'].toString()
        	uomValue = propAttributes['uom'].toString()
         }
    }
   	    
   log.debug 'ISYSMARTAPP: adding device. Address='+address+' name='+name
   if(enabled) {
     
        // Determine root address to use and sub-address for sub-devices
        def rootAddress = getMainAddress(address) 
        def subAddress = getSubAddress(address)

        // Fix up statevalue
        if(stateValue == null) {
            stateValue = " "
        }    

        // Determine isy device type
        def isyType = 'U'
        def isyCollect = false

		def familyNode = xml.node.family
        def familyId = null;
        
        if(familyNode != null) {
        	if(familyNode.toString().isInteger()) {
        		familyId = familyNode.toString().toInteger()
            }
        }
        
        if(familyId == null) { //Insteon device?
        	if (xml.group) { //Scene?
            	isyType = 'DL'
            } else {
        		isyType = getIsyType(devType.toString(), address.toString(), uomValue.toString())
            }
        } else if(familyId == 4) {
        	isyType = getZWaveType(xml.node)
        }

        String addressToUse = address

        // Mark devices which we want to treat as composites as such
        if(isyType == 'IO' || isyType == 'IM' || isyType == 'CS') {
            isyCollect = true
            addressToUse = rootAddress
        }

        log.debug 'ISYSMARTAPP: DEVICE: isyType='+isyType+' addr='+address+' rootAddress='+rootAddress+' subAddr='+subAddress+' name='+name+' isyType='+isyType+' isyCollect='+isyCollect+' devType='+devType+' state='+stateValue+' form='+formattedValue+' uom='+uomValue

        def newDevice = null

        if(isyCollect && getChildDevice(addressToUse) != null) {
            newDevice = getChildDevice(addressToUse)
        } else {
            if(isyType == "DL" ) {
                log.debug "ISYSMARTAPP: Creating Dimmable device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYDimmableLight', addressToUse, null, [label:name,completedSetup: true])
            } else if(isyType == "L") {
                log.debug "ISYSMARTAPP: Creating On Off Light device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYOnOffLight', addressToUse, null, [label:name,completedSetup:true])
            } else if(isyType == "F") {
                log.debug "ISYSMARTAPP: Creating FAN device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYFanLinc', addressToUse, null, [label:name,completedSetup:true])
            } else if(isyType == "O") {
                log.debug "ISYSMARTAPP: Creating Outlet device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYOutlet', addressToUse, null, [label:name,completedSetup:true])  
            } else if(isyType == "CS") {
                log.debug "ISYSMARTAPP: Creating Contact Sensor device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYContactSensor', addressToUse, null, [label:name,completedSetup:true]) 
            } else if(isyType == "IM") {
                log.debug "ISYSMARTAPP: Creating Motion Sensor device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYMotionSensor', addressToUse, null, [label:name,completedSetup:true])   
            } else if(isyType == "ML") {
                log.debug "ISYSMARTAPP: Creating MorningLinc device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYMorningLinc', addressToUse, null, [label:name,completedSetup:true])    
            } else if(isyType == "ZL") {
                log.debug "ISYSMARTAPP: Creating ZWaveLock device with name="+name
                newDevice = addChildDevice('ronbarr', 'ISYZWaveLock', addressToUse, null, [label:name,completedSetup:true])                   
            } else {
                log.debug "ISYSMARTAPP: Ignoring device: dni="+addressToUse+"+ name="+name+" type="+isyType
            }
        }

        if(newDevice != null) {
            if(stateValue == ' ') {
                //log.debug "ISYSMARTAPP: Setting blank default state on device: "+name
                newDevice.setISYState(address, stateValue)
            } else {
                newDevice.setISYState(address, stateValue.toInteger())
            }
        }
    } else {
	    log.debug 'ISYSMARTAPP: Ignoring disabled device. Address='+address
    }

    sendNextGetInfoRequest()
}

def scaleISYDeviceLevelToSTRange(incomingLevel) {
    def topOfLevel = 99 * incomingLevel
    def bottomOfLevel = 255
    def scaledLevel = topOfLevel / bottomOfLevel
    def scaled2Level = (Integer) (Math.ceil(topOfLevel / bottomOfLevel))
    scaled2Level
}

def scaleSTRangeToISYDeviceLevel(incomingLevel) {
	def topOfLevel = 255 * incomingLevel
    def bottomOfLevel = 99
    def scaledLevel = topOfLevel / bottomOfLevel
    def scaled2Level = (Integer) (Math.ceil(topOfLevel / bottomOfLevel))
	scaled2Level
}

def handleNodeUpdateMessage(xml) {
	log.debug 'ISYSMARTAPP: handleNodeUpdateMessage: '+xml
    if(xml.control.toString() == 'ST') {
    	String incomingAddress = xml.node.toString()
        def deviceToUpdate = getChildDevice(incomingAddress)
        if(!deviceToUpdate) {
        	deviceToUpdate = getChildDevice(getMainAddress(incomingAddress))
        }
        if(deviceToUpdate) {
            if(xml.action == null || !xml.action.toString().isInteger()) {
            	log.debug "ISYSMARTAPP: Incoming message had a blank or wrong device state value..."
            	deviceToUpdate.setISYState(incomingAddress, " ")
            
            } else {
            	deviceToUpdate.setISYState(incomingAddress, xml.action.toString().toInteger())
            }
        } 
    } 
}

private String makeNetworkId(ipaddr, port) { 
     String hexIp = ipaddr.tokenize('.').collect { 
     String.format('%02X', it.toInteger()) 
     }.join() 
     String hexPort = String.format('%04X', port) 
     return "${hexIp}:${hexPort}" 
}

def sendSubscribeCommand() {
    def dni = makeNetworkId(settings.isyAddress,settings.isyPort)
    atomicState.hubDni = dni
	log.debug "ISYSMARTAPP: Now we are sending out subscribe for changes.."+dni
	def newDevice = addChildDevice('ronbarr', 'ISYHub', dni, location.hubs[atomicState.hubIndex].id, [label:"ISY Hub",completedSetup: true])
    newDevice.setParameters(settings.isyAddress,settings.isyPort,settings.isyUserName,settings.bridgeAddress,settings.bridgePort,settings.bridgeUserName,settings.bridgePassword)
    newDevice.subscribe(atomicState.hubIndex)
}
    
def sendNextGetInfoRequest() {
	if(atomicState.rawNodeList == null) {
    	log.debug "ISYSMARTAPP: IGNORING GetNextInfo because we are not yet setup."
        return
    }
    if(atomicState.rawNodeList.size() > 0) {
	    setCurrentLoadState("LoadingDetails"+atomicState.rawNodeList.size())          
    	log.debug "ISYSMARTAPP: Enumerating devices now... let's do the next one."
    	String nextAddress = atomicState.rawNodeList[0]
        def nodeList = atomicState.rawNodeList
        nodeList.remove(0)
        atomicState.rawNodeList = nodeList
        def requestPath = '/rest/nodes/'+nextAddress
        requestPath = requestPath.replaceAll(" ", "%20")
    	log.debug "ISYSMARTAPP: Sending request... "+requestPath
        sendHubCommand(getRequest(requestPath))
    } else {
    	atomicState.rawNodeList = null
		setCurrentLoadState("SendSubscribe")       
    	sendSubscribeCommand()
    }
}

def setCurrentLoadState(state) {
    atomicState.loadingState = state
    log.debug "ISYSMARTAPP: #### Transitioning to state: "+state
}

def handleXmlMessage(xml) {
	log.debug "ISYSMARTAPP: Processing xml message:" + xml
//    try {
        if(xml.name() == 'nodes') {
        	if(atomicState.loadingState == "InitialStatusRequest") {
                handleInitialStatusMessage(xml)
                setCurrentLoadState("LoadingNodeDetails")
                sendNextGetInfoRequest()
            } else {
            	handleStatusMessage(xml)
            }
        } else if(xml.name() == 'group') {
        	if(atomicState.loadingState == "InitialStatusRequest") {
                handleInitialStatusMessage(xml)
                setCurrentLoadState("LoadingNodeDetails")
                sendNextGetInfoRequest()
            } else {
            	handleStatusMessage(xml)
            }
      } else if(xml.name() == 'nodeInfo') {
            handleNodeInfoMessage(xml)
        } else if(xml.name() == 'Envelope') {
            setCurrentLoadState("LoadCompleted")
            def hubDevice = getChildDevice(atomicState.hubDni)
            log.debug 'ISYSMARTAPP: Hub device '+hubDevice
            log.debug 'ISYSMARTAPP: XML: '+xml.toString()
            hubDevice.setSubscribIdFromResponse(xml)
            log.debug 'ISYSMARTAPP: Got an envelope message'
            log.debug 'ISYSMARTAPP: Envelope '+xml.toString()
        } else if(xml.name() == 'Event') {
            log.debug 'ISYSMARTAPP: Got a node update message'       
        	handleNodeUpdateMessage(xml)
        } else if(xml.name() == 'RestResponse'){
        	handleStatusMessage(xml)
        } else {
        	log.debug 'ISYSMARTAPP: Ignoring malformed message. Message='+xml.name()+' XML:'+xml
        }
//    } catch(e) {
//        log.debug 'Error parsing devices: '+e
//    }
}

def locationHandler(evt) {
    //try {
        def msg = parseLanMessage(evt.description)
        if(!msg.xml) {
        	if(msg.body && msg.body.length() > 2) {
 	            msg.xml = new XmlSlurper().parseText(msg.body)
            }
        } 
        if(msg.xml) {
        	log.debug 'Received command... ' + msg.xml.name()
        	handleXmlMessage(msg.xml)
        } else {
            log.debug 'ISYSMARTAPP: Received non-xml command: '+ msg
        }
    //} catch(e) {
    //    log.debug 'ERROR -- Details: '+ e
   // }
}

def initialize() {
	setCurrentLoadState("Setup")
	findHub()
	subscribe(location, null, locationHandler, [filterEvents:false])
	setCurrentLoadState("InitialStatusRequest")
    sendHubCommand(getRequest('/rest/nodes'))
}

def doRefresh() {
    sendHubCommand(getRequest('/rest/status'))
}

def getRequest(path) {
	//log.debug "Building request..."+path+" host: "+settings.isyAddress+":"+settings.isyPort+" auth="+getAuthorization()
    new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': settings.isyAddress+":"+settings.isyPort,
            'Authorization': getAuthorization()
        ], null)
}

def getDeviceAddressSafeForUrl(device) {
	return getStringSafeForUrl(device.device.deviceNetworkId)
}

def getStringSafeForUrl(value) {
	return value.toString().replaceAll(' ', '%20')
}

def on(device,address) {
    String command = '/rest/nodes/'+getStringSafeForUrl(address)+'/cmd/DON'
	sendHubCommand(getRequest(command))
}

def off(device,address) {
    String command = '/rest/nodes/'+getStringSafeForUrl(address)+'/cmd/DOF'
	sendHubCommand(getRequest(command))
}

def setDim(device, level,address) {
	def isyLevel = scaleSTRangeToISYDeviceLevel(level)
    def command = '/rest/nodes/'+getStringSafeForUrl(address)+'/cmd/DON/'+isyLevel
	sendHubCommand(getRequest(command))
}

def setDimNoTranslation(device, isyLevel,address) {
    def command = '/rest/nodes/'+getStringSafeForUrl(address)+'/cmd/DON/'+isyLevel
	sendHubCommand(getRequest(command))
}

def secureLockCommand(device, address, locked) {
    def command = '/rest/nodes/'+getStringSafeForUrl(address)+'/cmd/SECMD/'
    if(locked) {
    	command = command + "1"
    } else {
    	command = command + "0"
    }
	sendHubCommand(getRequest(command))
}

def uninstalled() {
	def currentState = atomicState.loadingState
	log.debug "ISYSMARTAPP: Uninstalling. In current state: "+currentState
	if(atomicState.hubDni != null && currentState == "LoadCompleted") {
    	log.debug "ISYSMARTAPP: Doing an unsubscribe"
    	def hubDevice = getChildDevice(atomicState.hubDni);
        hubDevice.unsubscribe()
    } else {
    	log.debug "ISYSMARTAPP: Not unsubscribing, not needed."
    }
}