/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/main/Apps/Diagnostic%20Page/Diagnostics%20Page.groovy
 *
 *   Author: Lewis Heidrick
 *   
 *   12/28/2020 - Project Published to GitHub
 */

def setVersion() {
    state.name = "Diagnostics Page"
	state.version = "1.0.6"
}
    
definition(
    name: "Diagnostics Page",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Diagnostics Page",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences {
    page(name: "mainPage")
}
    
def mainPage() {
    ifTrace("mainPage")
    dynamicPage(name: "mainPage", install: true, uninstall: true) {

    section("When a door unlocks...") {
    String defaultName = "Device Testing App"
        label title: "Device Testing App", required:false, defaultValue: defaultName, submitOnChange:true   
        input "lock1", "capability.lock", title: "Lock:", submitOnChange:true, required:false, multiple:false
        input "contact", "capability.contactSensor", title: "Contact Sensor:", submitOnChange:true, required:false, multiple:false
        input "lightSwitch", "capability.switch", title: "Switch:", submitOnChange:true, required:false, multiple:false
        getLogLevels = 3
        input "ifLevel","enum", title: "IDE logging level",required: true, options: getLogLevels(), defaultValue : "3"
    }
    def title = "Device Current Values"
    sensorStates()
    section(){
        paragraph "${lockStatus}"
        paragraph "${contactStatus}"
        paragraph "${lightSwitchStatus}"
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
        app.updateSetting("refresh",[value:"false",type:"bool"])
        }
    }
}




// Application settings and startup
def installed() {
    ifTrace("installed")
    state.installed = true
    initialize()
}

def updated() {
    ifTrace("updated")
    unsubscribe()
    unschedule()
    initialize()
    updateLabel()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    subscribe(lock1, "lock", sensorHandler)
    subscribe(contactSensor, "contact.open", sensorHandler)
    subscribe(contactSensor, "contact.closed", sensorHandler)
    subscribe(lightSwitch, "switch", sensorHandler)
}

def sensorHandler(evt) {
}

def sensorStates() {
    lockStatus = " "
    contactStatus = " "
    lightSwitchStatus = " "
    if (lock1 != null) {lockStatus = "${lock1} [${lock1?.currentValue("lock")}]" }
    if (contact != null) {contactStatus = "${contact} [${contact?.currentValue("contact")}]"}
    if (lightSwitch != null) {lightSwitchStatus = "${lightSwitch} [${lightSwitch?.currentValue("switch")}]"}
}

// Logging functions
def getLogLevels() {
    return [["0":"None"],["1":"Info"],["2":"Debug"],["3":"Trace"]]
}

def infoOff() {
    app.updateSetting("isInfo", false)
    log.info "${state.displayName}: Info logging auto disabled."
}

def debugOff() {
    app.updateSetting("isDebug", false)
    log.info "${state.displayName}: Debug logging auto disabled."
}

def traceOff() {
    app.updateSetting("isTrace", false)
    log.trace "${state.displayName}: Trace logging auto disabled."
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

def ifWarn(msg) {
    log.warn "${state.displayName}: ${msg}"
}

def ifInfo(msg) {       
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL == 1 && isInfo == false) {return}//bail
    else if (logL > 0) {
		log.info "${state.displayName}: ${msg}"
	}
}

def ifDebug(msg) {
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 2 && isDebug == false) {return}//bail
    else if (logL > 1) {
		log.debug "${state.displayName}: ${msg}"
    }
}

def ifTrace(msg) {       
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 3 && isTrace == false) {return}//bail
    else if (logL > 2) {
		log.trace "${state.displayName}: ${msg}"
    }
}
