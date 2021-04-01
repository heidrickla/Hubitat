def setVersion(){
	state.version = "1.1.49" // Version number of this app
	state.InternalName = "SmartStart"   // this is the name used in the JSON file for this app
}

preferences {
    input("refresh_rate", "enum", title:"State Refresh Rate", options: ["Every 5 minutes","Every 10 minutes","Every 15 minutes","Every 30 minutes","Every hour","Every 3 hours","Disabled"], description: "Refresh Interval of vehicle information.", required: true, defaultValue: "Every 3 hours")
}

metadata {
    definition(name: "SmartStart Car", namespace: "heidrickla", author: "Lewis Heidrick") {
        capability "Switch"
        capability "Lock"
        capability "Refresh"
        capability "Power Meter"
        capability "Sensor"
        capability "Actuator"
        
        attribute "Geolocation", "string"
        attribute "latitude", "number"
        attribute "longitude", "number"
        
        command "EngineOn"
        command "EngineOff"
    }
}
    
def installed() {
 checkRefresh(settings)
}
    
def updated() {
 checkRefresh(settings)
}

def checkRefresh(settings) {
    switch (settings.refresh_rate?.toLowerCase()) {
    case "disabled":
        unschedule(doRefresh)
        doRefresh()
    break
    case "every 5 minutes":
        runEvery5Minutes(doRefresh)
    break
    case "every 10 minutes":
        runEvery10Minutes(doRefresh)
    break
    case "every 15 minutes":
        runEvery15Minutes(doRefresh)
    break
    case "every 30 minutes":
        runEvery30Minutes(doRefresh)
    break
    case "every hour":
        runEvery1Hour(doRefresh)
    break
    case "every 3 hours":
        runEvery3Hours(doRefresh)
    break
    default:
        runEvery3Hours(doRefresh)
    break
    }
}
    
def doRefresh() {
    fakeDataTest(parent.GetRandomNumber().toInteger())
    parent.pollStatus(device.deviceNetworkId)
}

def fakeDataTest(randomNumber) {
    sendEvent(name: "power", value: randomNumber)
}
 
 // parse events into attributes
def parseEventData(Map results) {
    if (results?.error == "TokenExpiredRetry") {
        log.debug "Token Refreshed, Repolling Data"
        runIn(10, doRefresh)
    } else {
        results.each {
            name,
            value ->
            log.debug "Event Name: $name - Event Value: $value"
        }
        if (results["locked"] == true) {
            sendEvent(name: "lock", value: "locked")
        } else {
            sendEvent(name: "lock", value: "unlocked")
        }

        if (results["running"] == true) {
            sendEvent(name: "switch", value: "on")
        } else {
            sendEvent(name: "switch", value: "off")
        }
	    sendEvent(name: "latitude", value: results["latitude"])
        sendEvent(name: "longitude", value: results["longitude"])
    }
}
    
def refresh() {
    doRefresh()
}
    
 // handle commands
def retryCmd(cmd) {
    log.debug "Token Refreshed, Resending Command Data"
    parent.sendCommand(cmd, device.deviceNetworkId)
    runIn(15, doRefresh)
}

def EngineOn() {
    sendEvent(name: "switch", value: "on")
    log.debug "Executing 'Engine on'"
    parent.sendCommand("Start", device.deviceNetworkId)
    runIn(15, doRefresh)
    runIn(3600, doRefresh)
}

def EngineOff() {
    sendEvent(name: "switch", value: "off")
    log.debug "Executing 'Engine off'"
    parent.sendCommand("Start", device.deviceNetworkId)
    runIn(15, doRefresh)
    runIn(3600, doRefresh)
}
    
def lock() {
    sendEvent(name: "lock", value: "locked")
    log.debug "Executing 'Lock Doors'"
    parent.sendCommand("Lock", device.deviceNetworkId)
    runIn(15, doRefresh)
}

def unlock() {
    sendEvent(name: "lock", value: "unlocked")
    log.debug "Executing 'Unlock Doors'"
    parent.sendCommand("Unlock", device.deviceNetworkId)
    runIn(15, doRefresh)
}

def on() {
    EngineOn()
}

def off() {
    EngineOff()
}
