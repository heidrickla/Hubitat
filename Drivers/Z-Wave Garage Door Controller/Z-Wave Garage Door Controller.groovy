import hubitat.zwave.commands.barrieroperatorv1.*
import groovy.transform.Field

def setVersion() {
    state.name = "Z-Wave Garage Door Controller"
	state.version = "1.1.67"
}

metadata {
	definition (
        name: "Z-Wave Garage Door Controller",
        namespace: "heidrickla",
        author: "Lewis.Heidrick",
        description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open.",
        category: "Convenience",
        iconUrl: "",
        iconX2Url: "",
        iconX3Url: "",
        importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Drivers/Z-Wave%20Garage%20Door%20Controller/Z-Wave%20Garage%20Door%20Controller.groovy")
    {
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Sensor"
        capability "Polling"
        capability "Switch"
        capability "Relay Switch"
        capability "Momentary"
        capability "Battery"
        capability "Configuration"
        capability "Health Check"
        
        command "resetBattery"
        
        attribute "lowBattery", "string"

		fingerprint deviceId: "0x4007", inClusters: "0x98"
		fingerprint deviceId: "0x4006", inClusters: "0x98"
        fingerprint mfr:"014F", prod:"4744", model:"3030", deviceJoinName: "Linear GoControl Garage Door Opener" // zw:Ls type:4007 mfr:014F prod:4744 model:3030 ver:1.02 zwv:3.67 lib:03 cc:72,98,5A sec:86,66,22,71,85
		fingerprint mfr:"014F", prod:"4744", model:"3530", deviceJoinName: "GoControl Smart Garage Door Controller"
	}

	simulator {
		status "opening": "command: 9881, payload: 00 66 03 FE"
		status "open": "command: 9881, payload: 00 66 03 FF"
		status "closing": "command: 9881, payload: 00 66 03 FC"
		status "closed": "command: 9881, payload: 00 66 03 00"
        status "unknown": "command: 9881, payload: 00 66 03 FD"

		reply "988100660100": "command: 9881, payload: 00 66 03 FC"
		reply "9881006601FF": "command: 9881, payload: 00 66 03 FE"
	}

    preferences {
        input "batteryReset", "bool", title: "Reset low battery.", description: "Toggle to reset battery level"
        input "ifLevel","enum", title: "Logging level", required: false, multiple: true, submitOnChange: false, options: logLevelOptions
    }
}

@Field static List<Map<String,String>> logLevelOptions = [
    ["0": "None"],
    ["1": "Info"],
    ["2": "Debug"],
    ["3": "Trace"]
]

def uninstalled() {
    ifTrace( "Uninstalled called")
}

def installed() {
    ifTrace( "Installed called")

	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	
    try {
        response(refresh()) // Get the updates
	} catch (e) {
		log.warn "updated() threw $e"
	}
}

def updated() {
	ifTrace( "Updated called settings: $settings")
    if (ifInfo) runIn(1800,infoOff)
    if (ifDebug) runIn(1800,debugOff)
    if (ifTrace) runIn(1800,traceOff)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    resetBattery()
	
    try {
        response(refresh())
	} catch (e) {
		log.warn "updated() threw $e"
	}
}

def configure() {
    ifTrace( "Configure called")
    resetBattery()
}

def ping() {
	ifTrace( "Ping called")
	// Just get device state, there's no need to flood more commands
	sendHubCommand(new hubitat.device.HubAction(secure(zwave.barrierOperatorV1.barrierOperatorGet())))
}

private getCommandClassVersions() {
	[
		0x63: 1,  // User Code
		0x71: 3,  // Notification
		0x72: 2,  // Manufacturer Specific
		0x80: 1,  // Battery
		0x85: 2,  // Association
		0x98: 1   // Security 0
	]
}

def parse(String description) {
    sendEvent([name: "driverName", value: "Z-Wave Garage Door Controller"])

	def result = null
	if (description.startsWith("Err")) {
		if (state.sec) {
			result = createEvent(descriptionText:description, displayed:false)
		} else {
			result = createEvent(
				descriptionText: "This device failed to complete the network security key exchange. If you are unable to control it via Hubitat, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				displayed: true,
			)
		}
	} else {
		def cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	ifDebug("\"$description\" parsed to ${result.inspect()}")
	result
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	ifDebug("encapsulated: $encapsulatedCommand")
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(hubitat.zwave.commands.securityv1.NetworkKeyVerify cmd) {
	createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful")
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
	state.sec = cmd.commandClassSupport.collect { String.format("%02X ", it) }.join()
	if (cmd.commandClassControl) {
		state.secCon = cmd.commandClassControl.collect { String.format("%02X ", it) }.join()
	}
	ifDebug("Security command classes: $state.sec")
	createEvent(name:"secureInclusion", value:"success", descriptionText:"$device.displayText is securely included")
}

def zwaveEvent(BarrierOperatorReport cmd) {
	ifDebug("BarrierOperatorReport $cmd")
	def result = []
	def map = [ name: "door" ]
	switch (cmd.barrierState) {
		case BarrierOperatorReport.BARRIER_STATE_CLOSED:
			map.value = "closed"
			result << createEvent(name: "contact", value: "closed", displayed: false)
			result << createEvent(name: "switch", value: "off", displayed: false)
			break
		case BarrierOperatorReport.BARRIER_STATE_UNKNOWN_POSITION_MOVING_TO_CLOSE:
			map.value = "closing"
			break
		case BarrierOperatorReport.BARRIER_STATE_UNKNOWN_POSITION_STOPPED:
			map.descriptionText = "$device.displayName door state is unknown"
			map.value = "unknown"
			break
		case BarrierOperatorReport.BARRIER_STATE_UNKNOWN_POSITION_MOVING_TO_OPEN:
			map.value = "opening"
			break
		case BarrierOperatorReport.BARRIER_STATE_OPEN:
			map.value = "open"
			result << createEvent(name: "contact", value: "open", displayed: false)
			result << createEvent(name: "switch", value: "on", displayed: false)
			break
	}
	result + createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
	def result = []
	def map = [:]
	if (cmd.notificationType == 6) {
		map.displayed = true
		switch(cmd.event) {
			case 0x40:
				if (cmd.eventParameter[0]) {
					map.descriptionText = "$device.displayName performing initialization process"
				} else {
					map.descriptionText = "$device.displayName initialization process complete"
				}
				break
			case 0x41:
				map.descriptionText = "$device.displayName door operation force has been exceeded"
				break
			case 0x42:
				map.descriptionText = "$device.displayName motor has exceeded operational time limit"
				break
			case 0x43:
				map.descriptionText = "$device.displayName has exceeded physical mechanical limits"
				break
			case 0x44:
				map.descriptionText = "$device.displayName unable to perform requested operation (UL requirement)"
				break
			case 0x45:
				map.descriptionText = "$device.displayName remote operation disabled (UL requirement)"
				break
			case 0x46:
				map.descriptionText = "$device.displayName failed to perform operation due to device malfunction"
				break
			case 0x47:
				if (cmd.eventParameter[0]) {
					map.descriptionText = "$device.displayName vacation mode enabled"
				} else {
					map.descriptionText = "$device.displayName vacation mode disabled"
				}
				break
			case 0x48:
				if (cmd.eventParameter[0]) {
					map.descriptionText = "$device.displayName safety beam obstructed"
				} else {
					map.descriptionText = "$device.displayName safety beam obstruction cleared"
				}
				break
			case 0x49:
				if (cmd.eventParameter[0]) {
					map.descriptionText = "$device.displayName door sensor ${cmd.eventParameter[0]} not detected"
				} else {
					map.descriptionText = "$device.displayName door sensor not detected"
				}
				break
			case 0x4A:
				if (cmd.eventParameter[0]) {
					map.descriptionText = "$device.displayName door sensor ${cmd.eventParameter[0]} has a low battery"
				} else {
					map.descriptionText = "$device.displayName door sensor has a low battery"
				}
                sendEvent(name: "lowBattery", value: "Replace Sensor Battery", descriptionText: map.descriptionText, displayed: true, isStateChange: true)
				result << createEvent(name: "battery", value: 1, unit: "%", descriptionText: map.descriptionText)
				break
			case 0x4B:
				map.descriptionText = "$device.displayName detected a short in wall station wires"
				break
			case 0x4C:
				map.descriptionText = "$device.displayName is associated with non-Z-Wave remote control"
				break
			default:
				map.descriptionText = "$device.displayName: access control alarm $cmd.event"
				map.displayed = false
				break
		}
	} else if (cmd.notificationType == 7) {
		switch (cmd.event) {
			case 1:
			case 2:
				map.descriptionText = "$device.displayName detected intrusion"
				break
			case 3:
				map.descriptionText = "$device.displayName tampering detected: product cover removed"
				break
			case 4:
				map.descriptionText = "$device.displayName tampering detected: incorrect code"
				break
			case 7:
			case 8:
				map.descriptionText = "$device.displayName detected motion"
				break
			default:
				map.descriptionText = "$device.displayName: security alarm $cmd.event"
				map.displayed = false
		}
	} else if (cmd.notificationType){
		map.descriptionText = "$device.displayName: alarm type $cmd.notificationType event $cmd.event"
	} else {
		map.descriptionText = "$device.displayName: alarm $cmd.v1AlarmType is ${cmd.v1AlarmLevel == 255 ? 'active' : cmd.v1AlarmLevel ?: 'inactive'}"
	}
	result ? [createEvent(map), *result] : createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
        sendEvent(name: "lowBattery", value: "Replace Sensor Battery", descriptionText: map.descriptionText, displayed: true, isStateChange: true)
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbatt = new Date().time
	createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	ifDebug("msr: $msr")
	updateDataValue("MSR", msr)

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
	result
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	def text = "$device.displayName: firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
	createEvent(descriptionText: text, isStateChange: false)
}

def zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
	def msg = cmd.status == 0 ? "try again later" :
	          cmd.status == 1 ? "try again in $cmd.waitTime seconds" :
	          cmd.status == 2 ? "request queued" : "sorry"
	createEvent(displayed: true, descriptionText: "$device.displayName is busy, $msg")
}

def zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
	createEvent(displayed: true, descriptionText: "$device.displayName rejected the last request")
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	createEvent(displayed: false, descriptionText: "$device.displayName: $cmd")
}

def open() {
    delayBetween([
	    secure(zwave.barrierOperatorV1.barrierOperatorSet(requestedBarrierState: BarrierOperatorSet.REQUESTED_BARRIER_STATE_OPEN)),
        secure(zwave.barrierOperatorV1.barrierOperatorGet())
    ], 10000)
}

def close() {
    delayBetween([
	    secure(zwave.barrierOperatorV1.barrierOperatorSet(requestedBarrierState: BarrierOperatorSet.REQUESTED_BARRIER_STATE_CLOSE)),
        secure(zwave.barrierOperatorV1.barrierOperatorGet())
    ], 10000)
}

def refresh() {
	ifTrace( "Refresh called")
    poll()
}

def poll() {
	ifTrace( "Poll called")
    ifDebug("Device MSR ${state.MSR}")

	// Get the latest status
	delayBetween([
    	secure(zwave.barrierOperatorV1.barrierOperatorGet()),
        secure(zwave.batteryV1.batteryGet()), // Try to get battery level using secure channel
        zwave.batteryV1.batteryGet().format(), // Try to get battery level
        state.MSR ? [] : zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
        ], 2000)
}

def resetBattery() {
    ifDebug("Resetting low battery notifications")
	sendEvent(name: "lowBattery", value: "Sensor Battery OK", descriptionText: "$device.displayName door sensor has an OK battery")
	sendEvent(name: "battery", value: 100, unit: "%")
}

private secure(hubitat.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private secureSequence(commands, delay=200) {
	delayBetween(commands.collect{ secure(it) }, delay)
}

def on() {
	open()
}

def off() {
	close()
}

def push() {    
    def latest = device.latestValue("door");
	ifDebug("Garage door push button, current state $latest")

	switch (latest) {
    	case "open":
        	ifDebug("Closing garage door")
        	close()
            sendEvent(name: "momentary", value: "pushed", isStateChange: true)
            break
            
        case "closed":
        	ifDebug("Opening garage door")
        	open()
            sendEvent(name: "momentary", value: "pushed", isStateChange: true)
            break
            
        default:
        	ifDebug("Can't change state of door, unknown state $latest")
            break
    }
}

def turnOffLoggingTogglesIn30() {
    ifTrace("turnOffLoggingTogglesIn30")
    if (!isInfo) {app.updateSetting("isInfo",[value:"false",type:"bool"])}
    if (!isDebug) {app.updateSetting("isDebug",[value:"false",type:"bool"])}
    if (!isTrace) {app.updateSetting("isTrace",[value:"false",type:"bool"])}
    if (isTrace == true) {runIn(1800, infoOff)}
    if (isDebug == true) {runIn(1800, debugOff)}
    if (isTrace == true) {runIn(1800, traceOff)}
}

def infoOff() {
    app.updateSetting("isInfo",[value:"false",type:"bool"])
    if (isInfo == false) {log.warn "${state.thisName}: Info logging disabled."}
}

def debugOff() {
    app.updateSetting("isDebug",[value:"false",type:"bool"])
    if (isDebug == false) {log.warn "${state.thisName}: Debug logging disabled."}
}

def traceOff() {
    app.updateSetting("isTrace",[value:"false",type:"bool"])
    if (isTrace == false) {log.warn "${state.thisName}: Trace logging disabled."}
}

def disableInfoIn30() {
    if (isInfo == true) {
        runIn(1800, infoOff)
        log.info "Info logging disabling in 30 minutes."
    }
}

def disableDebugIn30() {
    if (isDebug == true) {
        runIn(1800, debugOff)
        log.debug "Debug logging disabling in 30 minutes."
    }
}

def disableTraceIn30() {
    if (isTrace == true) {
        runIn(1800, traceOff)
        log.trace "Trace logging disabling in 30 minutes."
    }
}

def ifWarn(msg) {log.warn "${state.thisName}: ${msg}"}

def ifInfo(msg) {
    if (!settings.ifLevel?.contains("1") && (isInfo != true)) {return}//bail
    else if (settings.ifLevel?.contains("1") || (isInfo == true)) {log.info "${state.thisName}: ${msg}"}
}

def ifDebug(msg) {
    if (!settings.ifLevel?.contains("2") && (isDebug != true)) {return}//bail
    else if (settings.ifLevel?.contains("2") || (isDebug == true)) {log.debug "${state.thisName}: ${msg}"}
}

def ifTrace(msg) {
    if (!settings.ifLevel?.contains("3") && (isTrace != true)) {return}//bail
    else if (settings.ifLevel?.contains("3") || (isTrace == true)) {log.trace "${state.thisName}: ${msg}"}
}

