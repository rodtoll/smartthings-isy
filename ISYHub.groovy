/**
 *  ISYHub - Represents the ISY Hub itself. Placeholder device needed to send out request for notifications so we can receive them.
 *
 *  The current implementation talks to an ISY directly and uses a bridge to get notifications from the ISY device. The bridge is needed because
 *  the way that SmartThings checks incoming POST requests for validity rejects the POST requests from the ISY device. The bridge simply enables
 *  the SmartThings device to subscribe to notifications and receive them in a form which it will accept. The bridge under the covers proxies the
 *  requests and notifications to the ISY device and fixes them up so they will be a form SmartThings will accept.
 * 
 *  See the st-isy-notify-bridge github project for details and instructions for setting it up.
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
 */
metadata {
	definition (name: "ISYHub", namespace: "rodtoll", author: "Rod Toll") {
		capability "Refresh"
        command "subscribe"
        command "unsubscribe"
        attribute "isyHubAddress", "string"  // Address of the ISY
        attribute "isyHubPort", "string"     // Port of the ISY. Usually 80.
        attribute "isyUserName", "string"    // Username to use with the ISY
        attribute "notifyAddress", "string"  // Address for the bridge running st-isy-notify-bridge
        attribute "notifyPort", "string"	 // Port for the bridge running st-isy-notify-bridge
		attribute "notifyUserName", "string" // Username to use with the instance of the st-isy-notify-bridge
        attribute "notifyPassword", "string" // Password to use with the instance of the st-isy-notify-bridge
        attribute "subsciptionId", "string"  // subscription Id for notifications
	}

	simulator {
	}

	tiles(scale: 2) {
        valueTile("isyHubAddress", "device.isyHubAddress", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Hub: ${currentValue}', defaultState: true
        } 
        valueTile("isyHubPort", "device.isyHubPort", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Hub Port: ${currentValue}', defaultState: true
        } 
        valueTile("isyUserName", "device.isyUserName", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Hub User: ${currentValue}', defaultState: true
        }         
        valueTile("notifyAddress", "device.notifyAddress", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Bridge: ${currentValue}', defaultState: true
        } 
        valueTile("notifyPort", "device.notifyPort", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Bridge Port: ${currentValue}', defaultState: true
        }
        valueTile("notifyUserName", "device.notifyUserName", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Bridge User: ${currentValue}', defaultState: true
        }
        valueTile("subsciptionId", "device.subsciptionId", decoration: "Flat", inactiveLabel: false, width: 3, height: 2) {
        	state "default", label: 'Sub: ${currentValue}', defaultState: true
        }        
    }
    main "isyHubAddress"
    details(["isyHubAddress","isyHubPort","isyUserName","notifyAddress","notifyPort","notifyUserName","subsciptionId"])
}

preferences {
	section("Information") {
	}
}

// Setup the parameters for the device and the bridge. 
def setParameters(isyHubAddress,isyHubPort,isyUserName,notifyAddress,notifyPort,notifyUserName,notifyPassword) {
	sendEvent(name: 'isyHubAddress', value: isyHubAddress)
	sendEvent(name: 'isyHubPort', value: isyHubPort)
	sendEvent(name: 'isyUserName', value: isyUserName)
	sendEvent(name: 'notifyAddress', value: notifyAddress)
	sendEvent(name: 'notifyPort', value: notifyPort)
	sendEvent(name: 'notifyUserName', value: notifyUserName)
	sendEvent(name: 'notifyPassword', value: notifyPassword)
}

// Parses incoming LAN messages. Proxies them to the SmartApp.
def parse(description) {
    def msg = parseLanMessage(description)
    if(!msg.xml) {
        if(msg.body && msg.body.length() > 0) {
            msg.xml = new XmlSlurper().parseText(msg.body)
        }
    }     
    if(msg.xml) {
        parent.handleXmlMessage(msg.xml)
        log.debug "ISYHUB: Processing..."+msg.xml.name()
        if(msg.xml.name() == "Envelope") {
        	setSubscribIdFromResponse(msg.xml)
        }
    } else {
        log.debug 'ISYHUB: Received non-xml command in ISYHub: '+ msg.body
    }    
}

def setSubscribIdFromResponse(xml) {
    def subscriptionSID = xml.Body.SubscriptionResponse.SID.toString()
    log.debug 'ISYHUB: Subscription Id='+subscriptionSID
    sendEvent(name: 'subsciptionId', value: subscriptionSID)
}

// Executes a refresh. This doesn't work yet.
def refresh() {
	log.debug "ISYHUB: Executing 'refresh'"
	parent.doRefresh()
}

// Buildings authorization string for username and password
private getAuthorization() {
    def userpassascii = device.currentValue("notifyUserName") + ":" + device.currentValue("notifyPassword")
    "Basic " + userpassascii.encodeAsBase64().toString()
}

// Execute a subscribe against the bridge (instance of st-isy-notify-bridge)
def subscribe(hubId) {
	def hub = location.hubs[hubId]
    def localHubIp = hub.localIP
    def localPort = hub.localSrvPortTCP
    def subscribeUrl = 'http://'+localHubIp+':'+localPort+'/'
	def bodyText = '<s:Envelope><s:Body><u:Subscribe xmlns:u="urn:udi-com:service:X_Insteon_Lighting_Service:1">'
    bodyText += '<reportURL>'+subscribeUrl+'</reportURL><duration>infinite</duration></u:Subscribe></s:Body></s:Envelope>\r\n\r\n';   
    def bridgeUrl = device.currentValue("notifyAddress")+":"+device.currentValue("notifyPort")
    log.debug "ISYHUB: Subscribing to via bridge: "+bridgeUrl
    def result = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/services",
        body: bodyText,
        headers: [
        	HOST: bridgeUrl,
            Authorization: getAuthorization()
        ]
    )
    return result
}

def unsubscribe() {
	log.debug "ISYHUB: Unsubscribing for id: "+device.currentValue("subsciptionId")
    log.debug "ISYHUB: Unsubscribe not implemented"
}
