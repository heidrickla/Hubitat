/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.0"
}
    
definition(
    name: "Auto Lock Child",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open after X seconds.",
    category: "Convenience",
    parent: "heidrickla:Auto Lock",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences {
    page(name: "mainPage")
    page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
        }
    }
}
    
def mainPage() {
    ifTrace("mainPage")
    if (!isDebug) {
        app.updateSetting("isDebug", false)
    }
    if (isTrace == true) {
        runIn(1800, traceOff)
    }
    if (isDebug == true) {
        runIn(1800, debugOff)
    }
    if (isTrace == true) {
        runIn(1800, traceOff)
    }
    
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
        diagnosticHandler()
        if (state?.disabled == true) {
            state.pauseButtonName = "Disabled by Switch"
            unsubscribe()
            unschedule()
            subscribe(disabledSwitch, "switch", disabledHandler)
            updateLabel()
        } else if (state.paused == true) {
            state.pauseButtonName = "Resume"
            unsubscribe()
            unschedule()
            subscribe(disabledSwitch, "switch", disabledHandler)
            updateLabel()
        } else {
            state.pauseButtonName = "Pause"
            initialize()
            updateLabel()
        }
    section("") {
      input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
    }
    section("") {
        updateLabel()
        String defaultName = "Enter a name for this child app"
        if(state.newName != null) {defaultName = state.newName}
        label title: "Enter a name for this child app", required:false, defaultValue: defaultName, submitOnChange:true 
    }
    section("When a door unlocks...") {
        input "lock1", "capability.lock", title: "Lock: ${lock1Status}", required: true, submitOnChange: true
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
    }
    section() {
        input "duration", "number", title: "Lock it how many minutes/seconds later?"
    }
    section() {
        input type: "bool", name: "minSec", title: "Default is minutes. Use seconds instead?", required: true, defaultValue: false
    }
    section("Lock it only when this door is closed.") {
        input "contact", "capability.contactSensor", title: "Door Contact: ${contactStatus}", required: false, submitOnChange: true
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
        app.updateSetting("refresh",[value:"false",type:"bool"])
    }
    section("Logging Options", hideable: true, hidden: hideLoggingSection()) {
            input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, defaultValue: false
            input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, defaultValue: false
		    input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, defaultValue: false
            input "ifLevel","enum", title: "IDE logging level",required: true, options: getLogLevels(), defaultValue : "1"
            paragraph "NOTE: IDE logging level overrides the temporary logging selections."
    }
    section(title: "Only Run When:", hideable: true, hidden: hideOptionsSection()) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
			options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            input "disabledSwitch", "capability.switch", title: "Switch to Enable and Disable this app", submitOnChange:true, required:false, multiple:true
    }
    }
    
}

// Application settings and startup
def installed() {
    ifDebug("Auto Lock Door installed.")
    state.installed = true
    initialize()
}

def updated() {
    ifDebug("Auto Lock Door updated.")
    unsubscribe()
    unschedule()
    subscribe(disabledSwitch, "switch", disabledHandler)
    subscribe(deviceActivationSwitch, "switch", deviceActivationSwitchHandler)
    updateLabel()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    subscribe(lock1, "lock", doorHandler)
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
    subscribe(disabledSwitch, "switch.on", disabledHandler)
    subscribe(disabledSwitch, "switch.off", disabledHandler)
    subscribe(deviceActivationSwitch, "switch", deviceActivationSwitchHandler)
    subscribe(lock1, "lock", diagnosticHandler)
    subscribe(contact, "contact.open", diagnosticHandler)
    subscribe(contact, "contact.closed", diagnosticHandler)
    updateLabel()
}

// Device Handlers
def diagnosticHandler(evt) {
    ifTrace("diagnosticHandler")
    if (lock1?.currentValue("lock") != null) {
        lock1Status = lock1.currentValue("lock")
        ifTrace("diagnosticHandler: lock1.currentValue = ${lock1.currentValue("lock")}")
    } else if (lock1?.lastValue("lock") != null) {
        lock1Status = lock1.lastValue("lock")
        ifTrace("diagnosticHandler: lock1.lastValue = ${lock1.lastValue("lock")}")
    } else {
        lock1Status = " "
        ifTrace("diagnosticHandler: lock1Status = ${lock1Status}")
    }
    if (contact?.currentValue("contact") != null) {contactStatus = contact.currentValue("contact")
    } else if (contact?.lastValue("contact") != null) {contactStatus = contact.lastValue("contact")
    } else {
        contactStatus = " "   
    }
    updateLabel()
}

def doorHandler(evt) {
    ifTrace("doorHandler")
    updateLabel()
    if (state.pausedOrDisabled == false) {
        if (evt.value == "closed") {ifDebug("Door Closed")}
        if (evt.value == "opened") {
                ifDebug("Door open reset previous lock task...")
                unschedule(lockDoor)
                if (minSec) {
	                def delay = duration
                    runIn(delay, lockDoor)
	            } else {
	                def delay = duration * 60
                    runIn(delay, lockDoor)
                }
        }
        if (evt.value == "locked") {                  // If the human locks the door then...
            ifDebug("Cancelling previous lock task...")
            unschedule(lockDoor)                  // ...we don't need to lock it later.
            state.status = "(Locked)"
        } else {                                      // If the door is unlocked then...
            state.status = "(Unlocked)"
            if (minSec) {
	            def delay = duration
                ifDebug("Re-arming lock in in $duration second(s)")
                runIn( delay, lockDoor )
	        } else {
	            def delay = duration * 60
	            ifDebug("Re-arming lock in in $duration minute(s)")
                runIn( delay, lockDoor )
          }    
       }
    }
    updateLabel()
}

def disabledHandler(evt) {
    ifTrace("disabledHandler")
    if(disabledSwitch) {
        disabledSwitch.each { it ->
        disabledSwitchState = it.currentValue("switch")
            if (disabledSwitchState == "on") {
                state.disabled = false
                if (state?.paused == true) {
                    state.status = "(Paused)"
                    state.pausedOrDisabled = true
                } else {
                    state.paused = false
                    state.disabled = false
                    state.pausedOrDisabled = false
                    if (lock1.currentValue("lock") == "unlocked" && (contact.currentValue("contact") == "closed" || contact == null)) {
                        ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                        lockDoor()
                    }
                }
            } else if (disabledSwitchState == "off") {
                state.pauseButtonName = "Disabled by Switch"
                state.status = "(Disabled)"
                state.disabled = true
                updateLabel()
            }
        }
    }
    updateLabel()
}

def deviceActivationSwitchHandler(evt) {
    ifTrace("deviceActivationSwitchHandler")
    updateLabel()
    if (state.pausedOrDisabled == false) {
        if(deviceActivationSwitch) {
            deviceActivationSwitch.each { it ->
                deviceActivationSwitchState = it.currentValue("switch")
            }
                if (deviceActivationSwitchState == "on") {
                    ifDebug("deviceActivationSwitchHandler: Locking the door now")
                    lockDoor()
                    state.status = "(Locked)"
                } else if (deviceActivationSwitchState == "off") {
                    ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                    unlockDoor()
                    state.status = "(Unlocked)"
                }
        }
    } else {
        ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
    }
    updateLabel()
}

// Application Functions
def lockDoor() {
    ifTrace("lockDoor")
    ifDebug("Locking Door if Closed")
    updateLabel()
    if (state.pausedOrDisabled == false) {
        ifTrace("lockDoor: contact == ${contact}")
        if ((contact?.currentValue("contact") == "closed" || contact == null)) {
            ifDebug("Door Closed")
            lock1.lock()
            state.status = "(Locked)"
        } else {
            if (contact.currentValue("contact") == "open") {
                if (lock1.currentValue("lock") == "locked") {
                    lock1.unlock()
                    state.status = "(Unlocked)"
                    updateLabel()
                }
                ifTrace("lockDoor Door was open - waiting")
	            if (minSec) {
	                def delay = duration
                    ifDebug("Door open will try again in $duration second(s)")
                    runIn(delay, lockDoor)
	            } else {
	                def delay = duration * 60
	                ifDebug("Door open will try again in $duration minute(s)")
                    runIn(delay, lockDoor)
                }
	        }
        }
    }
    updateLabel()
}

def unlockDoor() {
    ifTrace("unlockDoor")
    ifDebug("Unlocking Door")
    updateLabel()
    if (state.pausedOrDisabled == false) {
        if (lock1.currentValue("lock") == "locked") {
            ifInfo("unlockDoor: Unlocking the door now")
            lock1.unlock()
        }
    }
    updateLabel()
}

def changeMode(mode) {
    ifTrace("changeMode")
    ifDebug("Changing Mode to: ${mode}")
	if (location.mode != mode && location.modes?.find { it.name == mode}) setLocationMode(mode)
}

//Label Updates
void updateLabel() {
    if ((state?.pause == true) || (state.disabled == true)) {
        state.pausedOrDisabled = true
    } else {
        state.pausedOrDisabled = false
        if ((contact?.currentValue("contact") == "open") && (lock1?.currentValue("lock") == "locked")) {
            ifDebug("Door Open and Lock is locked, Unlocking.")
            lock1.unlock()
        }
    }
    if (!app.label.contains("<span") && !app.label.contains("Paused") && !app.label.contains("Disabled") && !app.label.contains("Locked") && !app.label.contains("Unlocked") && !app.label.contains("Unknown")) {
        state.displayName = app.label
    }
        String label = "${state.displayName} <span style=color:"
    if (state?.disabled == true) {
        state.status = "(Disabled)"
        status = "(Disabled)"
        label += "red"
    } else if (state?.paused == true) {
        state.status = "(Paused)"
        status = "(Paused)"
        label += "red"
    } else if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        status = "(Locked)"
        label += "green"
    } else if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        status = "(Unlocked)"
        label += "orange"
    } else {
        state.paused = false
        state.disabled = false
        state.pausedOrDisabled = false
        state.status = "(Unknown)"
        status = "(Unknown)"
        label += "yellow"
    }
    label += ">${status}</span>"
    app.updateLabel(label)
    state.newName = label
    if(state.newName != null) {defaultName = state.newName}
}

//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Disabled by Switch") {
        state.disabled = false
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (lock1.currentValue("lock") == "unlocked" && (contact.currentValue("contact") == "closed" || contact == null)) {
                ifTrace("appButtonHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                lockDoor()
            }
        }
    }
    updateLabel()
}

// Application Page settings
private hideLoggingSection() {
	(isInfo || isDebug || isTrace || ifLevel) ? true : true
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? true : true
}

private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
    ifDebug("modeOk = ${result}")
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
    ifTrace("daysOk = ${result}")
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
    ifTrace{"timeOk = ${result}"}
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

// Logging functions
def getLogLevels() {
    return [["0":"None"],["1":"Info"],["2":"Debug"],["3":"Trace"]]
}

def infoOff() {
    app.updateSetting("isInfo", false)
    log.info "${state.displayName}: Info logging disabled."
}

def debugOff() {
    app.updateSetting("isDebug", false)
    log.info "${state.displayName}: Debug logging disabled."
}

def traceOff() {
    app.updateSetting("isTrace", false)
    log.trace "${state.displayName}: Trace logging disabled."
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
