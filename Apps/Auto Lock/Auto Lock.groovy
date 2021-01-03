/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.6"
}

definition(
    name: "Auto Lock Child",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open.",
    category: "Convenience",
    parent: "heidrickla:Auto Lock",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy")

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
    dynamicPage(name: "mainPage", install: true, uninstall: true, refreshInterval:0) {
    ifTrace("mainPage")
    turnOffLoggingTogglesIn30()
    setPauseButtonName()
    diagnosticHandler()
    section("") {
      input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
    }
    section("") {
        if ((thisName == null) || (thisName == "null <span style=color:pink>(Unknown)</span>")) {thisName = "Enter a name for this app."}
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true
        updateLabel()
    }
    section("") {
        input "lock1", "capability.lock", title: "Lock: [${lock1Status}]", required: true, submitOnChange: true
        input "contact", "capability.contactSensor", title: "Door Contact: [${contactStatus}]", required: false, submitOnChange: true
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
//        input "autoRefreshXMinutesLock", "enum", title: "Force a refresh of the state of the lock?", require: false, options: ["Never", "1", "5", "15", "30", "60"], defaultValue: "Never"
        app.updateSetting("refresh",[value:"false",type:"bool"])
    }
    section(title: "Locking Options:", hideable: true, hidden: hideLockOptionsSection()) {
        input "minSecLock", "bool", title: "Default is minutes. Use seconds instead?", required: true, defaultValue: false
        input "durationLock", "number", title: "Lock it how many minutes/seconds later?", required: true, defaultValue: 10
        input "retryLock", "bool", title: "Enable retries if lock fails to change state.", require: false, defaultValue: true
        input "maxRetriesLock", "number", title: "Maximum number of retries?", required: false, defaultValue: 3
        input "delayBetweenRetriesLock", "number", title: "Delay between retries in second(s)?", require: false, defaultValue: 5

    }
    section(title: "Unlocking Options:", hideable: true, hidden: hideUnlockOptionsSection()) {
        input "minSecUnlock", "bool", title: "Default is minutes. Use seconds instead?", required: true, defaultValue: false
        input "durationUnlock", "number", title: "Unlock it how many minutes/seconds later?", required: true, defaultValue: 2
        input "retryUnlock", "bool", title: "Enable retries if unlock fails to change state.", require: false, defaultValue: true
        input "maxRetriesUnlock", "number", title: "Maximum number of retries? While door is open it will wait for it to close.", required: false, defaultValue: 3
        input "delayBetweenRetriesUnlock", "number", title: "Delay between retries in second(s)?", require: false, defaultValue: 3
    }
    section(title: "Logging Options:", hideable: true, hidden: hideLoggingSection()) {
        input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "ifLevel","enum", title: "IDE logging level",required: true, options: getLogLevels(), defaultValue : "1"
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
    ifTrace("installed")
    ifDebug("Auto Lock Door installed.")
    state.installed = true
    if (lock1Status == null) {lock1Status = " "}
    if (contactStatus == null) {contactStatus = " "}
    if (lock1batteryStatus == null) {lock1BatteryStatus = " "}
    if (contactBatteryStatus == null) {contactBatteryStatus = " "}
    initialize()
}

def updated() {
    ifTrace("updated")
    ifDebug("Settings: ${settings}")
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    subscribe(lock1, "lock", lockHandler)
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
    subscribe(disabledSwitch, "switch.on", disabledHandler)
    subscribe(disabledSwitch, "switch.off", disabledHandler)
    subscribe(deviceActivationSwitch, "switch", deviceActivationSwitchHandler)
    subscribe(lock1, "lock", diagnosticHandler)
    subscribe(contact, "contact.open", diagnosticHandler)
    subscribe(contact, "contact.closed", diagnosticHandler)
    subscribe(lock1, "battery", batteryHandler)
    subscribe(contact, "battery", batteryHandler)
    diagnosticHandler()
    updateLabel()
    getAllOk()
}

// Device Handlers
def diagnosticHandler(evt) {
    ifTrace("diagnosticHandler")
    if (lock1?.currentValue("lock") != null) {
        lock1Status = lock1.currentValue("lock")
    } else if (lock1?.latestValue("lock") != null) {
        lock1Status = lock1.latestValue("lock")
    } else {
        lock1Status = " "
        ifTrace("diagnosticHandler: lock1Status = ${lock1Status}")
    }
    
    if (contact?.currentValue("contact") != null) {
        contactStatus = contact.currentValue("contact")
    } else if (contact?.latestValue("contact") != null) {
        contactStatus = contact.latestValue("contact")
    } else {
        contactStatus = " "   
    }
    updateLabel()
}

def batteryHandler(evt) {
    if (lock1?.currentValue("battery") != null) {
        lock1BatteryStatus = lock1.currentValue("battery")
    } else if (lock1?.latestValue("battery") != null) {
        lock1BatteryStatus = lock1.latestValue("battery")
    } else {
        lock1BatteryStatus = " "
    }
    if (contact?.currentValue("battery") != null) {
        contactBatteryStatus = contact.currentValue("battery")
    } else if (contact?.latestValue("contact") != null) {
        contactBatteryStatus = contact.latestValue("battery")
    } else {
        lock1BatteryStatus = " "
    }
}

def lockHandler(evt) {
    ifTrace("lockHandler")
    ifTrace("lockHandler: ${evt.value}")
    updateLabel()
    if ((getAllOk() != true) || (state?.pausedOrDisabled == true)) {
        ifTrace("lockHandler: Application is paused or disabled.")
    } else {
        if ((lock1.currentValue("lock") == "locked") && (contact.currentValue("contact") != "closed")) {
            ifDebug("lockHandler:  Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
            lock1.unlock()
            unschedule(unlockDoor)
            def delayUnlock = 1
            runIn(delayUnlock, unlockDoor)
        } else if ((lock1.currentValue("lock") == "locked") && (contact?.currentValue("contact") == "closed" || contact == null)) {
            ifDebug("Cancelling previous lock task...")
            unschedule(lockDoor)                  // ...we don't need to lock it later.
            unschedule(unlockDoor) 
            state.status = "(Locked)"
        } else if ((lock1.currentValue("lock") == "unlocked") && (contact?.currentValue("contact") == "open")) {
            ifTrace("The door is open and the lock is unlocked. Nothing to do.")
            state.status = "(Unlocked)"           
        } else {                                  
            countLocked = maxRetriesLock
            state.status = "(Unlocked)"
            if (minSecLock) {
	            def delayLock = durationLock
                ifDebug("Re-arming lock in in ${durationLock} second(s)")
                runIn(delayLock, lockDoor)
            } else {
                def delayLock = (durationLock * 60)
                ifDebug("Re-arming lock in in ${durationLock} minute(s)")
                runIn(delayLock, lockDoor)
            }
        }
    updateLabel()
    }
}

def doorHandler(evt) {
    ifTrace("doorHandler")
    updateLabel()
    if ((getAllOk() != true) || (state?.pausedOrDisabled == true)){
        ifTrace("doorHandler: Application is paused or disabled.")
    } else {
        if ((contact.currentValue("contact") == "closed") && (lock1.currentValue("lock") == "unlocked")) {
            ifDebug("Door closed, locking door.")
            unschedule(lockDoor)
            if (minSecLock) {
                def delayLock = durationLock
                runIn(delayLock, lockDoor)
            } else {
	            def delayLock = (durationLock * 60)
                runIn(delayLock, lockDoor)
            }
        } else if ((contact.currentValue("contact") == "open") && (lock1.currentValue("lock") == "locked")) {
            ifTrace("doorHandler: Door was opend while lock was locked. Performing a fast unlock in case and device refresh to get current state.")
            countUnlock = maxRetriesUnlock
            lock1.unlock()
            unschedule(checkUnlockedStatus)
            def delayUnlock = 1
            runIn(delayUnlock, checkUnlockedStatus)
            lock1.refresh()
            contact.refresh()
        }
    updateLabel()
    }
}

def disabledHandler(evt) {
    ifTrace("disabledHandler")
    updateLabel()
    if ((getAllOk() != true) || (state?.pausedOrDisabled == true)){
        ifTrace("disabledHandler: Application is paused or disabled.")
        } else {
        if(disabledSwitch) {
            disabledSwitch.each { it ->
            disabledSwitchState = it.currentValue("switch")
                if (disabledSwitchState == "on") {
                    ifTrace("disabledHandler: Disable switch turned on")
                    state.disabled = false
                    if (state?.paused == true) {
                        state.status = "(Paused)"
                        state.pausedOrDisabled = true
                    } else {
                        state.paused = false
                        state.disabled = false
                        state.pausedOrDisabled = false
                        if (lock1?.currentValue("lock") == "unlocked" && (contact?.currentValue("contact") == "closed" || contact == null)) {
                            ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                            if (minSecLock) {
                                def delayLock = durationLock
                                runIn(delayLock, lockDoor)
                            } else {
	                            def delayLock = durationLock * 60
                                runIn(delayLock, lockDoor)
                            }
                        }
                    }
                } else if (disabledSwitchState == "off") {
                    state.pauseButtonName = "Disabled by Switch"
                    state.status = "(Disabled)"
                    ifTrace("disabledHandler: (Disabled)")
                    state.disabled = true
                    updateLabel()
                }
            }
        }
        updateLabel()
    }
}

def deviceActivationSwitchHandler(evt) {
    ifTrace("deviceActivationSwitchHandler")
    updateLabel()
    if ((getAllOk() != true) || (state.pausedOrDisabled == true)) {
        ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
    } else {
        if (deviceActivationSwitch) {
            deviceActivationSwitch.each { it ->
                deviceActivationSwitchState = it.currentValue("switch")
            }
            if (deviceActivationSwitchState == "on") {
                ifDebug("deviceActivationSwitchHandler: Locking the door now")
                countUnlock = maxRetriesUnlock
                lock1.lock()
                if (minSecLock) {
                    def delayLock = durationLock
                    runIn(delayLock, lockDoor)
                } else {
                    def delayLock = durationLock * 60
                    runIn(delayLock, lockDoor)
                }
            } else if (deviceActivationSwitchState == "off") {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                countUnlock = maxRetriesUnlock
                lock1.unlock()
                countUnlock = maxRetriesUnlock
                if (minSecUnlock) {
                    def delayUnlock = durationUnlock
                    runIn(delayUnlock, unlockDoor)
                } else {
                    def delayUnlock = (durationUnlock * 60)
                    runIn(delayUnlock, unlockDoor)
                }
            }
        }
    updateLabel()
    }
}

// Application Functions
def lockDoor() {
    ifTrace("lockDoor")
    if ((getAllOk() != true) && (state?.pausedOrDisabled == false)) {
        ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
    } else {
        updateLabel()
        ifTrace("lockDoor: contact = ${contact}")
        if ((contact?.currentValue("contact") == "closed" || contact == null)) {
            ifDebug("Door is closed, locking door.")
            lock1.lock()
            unschedule(checkLockedStatus)
            countLock = maxRetriesLock
            if (minSecLock) {
                def delayLock = durationLock
                runIn(delayLock, checkLockedStatus)
            } else {
	            def delayLock = durationLock * 60
                runIn(delayLock, checkLockedStatus)
                } 
        } else if ((contact?.currentValue("contact") == "open") && (lock1?.currentValue("lock") == "locked")) {
            ifTrace("lockDoor: Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
            countUnlock = maxRetriesUnlock
            lock1.unlock()
            unschedule(unlockDoor)
            if (minSecUnlocked) {
                def delayUnlock = 1
                ifTrace("lockDoor: Performing a fast unlock to prevent hitting the bolt against the frame.")
                runIn(delayUnlock, checkUnlockedStatus)
                lock1.refresh()
            }
        } else {
            ifTrace("lockDoor: Unhandled exception")
        }
    updateLabel()
    }
}



def unlockDoor() {
    ifTrace("unlockDoor")
    if ((getAllOk() != true) && (state?.pausedOrDisabled == false)) {
        ifTrace("unlockDoor: Application is paused or disabled.")
    } else {
        updateLabel()
        ifTrace("unlockDoor: Unlocking door.")
        lock1.unlock()
        countUnlock = maxRetriesUnlock
        unschedule(unlockDoor)
        if (minSecUnlock) {
            def delayUnlock = durationUnlock
            runIn(delayUnlock, checkUnlockedStatus)
        } else {
	        def delayUnlock = (durationUnlock * 60)
            runIn(delayUnlock, checkUnlockedStatus)
        }
    updateLabel()
    }
}

def checkLockedStatus() {
    ifTrace("checkLockedStatus")
    if (lock1.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        ifTrace("checkLockedStatus: The lock was locked successfully")
        countLock = maxRetriesLock
    } else {
        state.status = "(Unlocked)"
        lock1.lock()
        countLock = (countLock - 1)
        if (countLock > -1) {
            runIn(delayBetweenRetriesLock, retryLockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on locking door.")
            countLock = maxRetriesLock
        }
    }
    updateLabel()
}

def checkUnlockedStatus() {
    ifTrace("checkUnlockedStatus")
    if (lock1.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        ifTrace("checkUnlockedStatus: The lock was unlocked successfully")
        countUnlock = maxRetriesUnlock
    } else {
        state.status = "(Locked)"
        lock1.unlock()
        countUnlock = (countUnlock - 1)
        if (countUnlock > -1) {
            checkLockedStatus
            runIn(delayBetweenRetriesUnlock, retryUnlockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on unlocking door.")
            countUnlock = maxRetriesUnlock
        }
    }
    updateLabel()
}

def retryLockingCommand() {
    ifTrace("retryLockingCommand")
    if (lock1.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        ifTrace("retryLockingCommand: The lock was locked successfully")
        countLock = maxRetriesLock
    } else if ((retryLock == true) && (lock1.currentValue("lock") != "locked")) {
        state.status = "(Unlocked)"
        lock1.lock()
        if (countUnlock > -1) {runIn(delayBetweenRetriesLock, retryLockingCommand)}
    } else {
        ifTrace("retryLockingCommand: retryLock = ${retryLock} - Doing nothing.")
    }
}

def retryUnlockingCommand() {
    ifTrace("retryUnlockingCommand")
    if (lock1.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        ifTrace("retryUnlockingCommand: The lock was unlocked successfully")
        countUnlock = maxRetriesUnlock
    } else if ((retryUnlock == true) && (lock1.currentValue("lock") != "unlocked")) {
        state.status = "(Locked)"
        lock1.unlock()
        countUnlock = (countUnlock - 1)
        if (countUnlock > -1) {runIn(delayBetweenRetriesUnlock, retryUnlockingCommand)}
    } else {
        ifTrace("retryUnlockingCommand: retryUnlock = ${retryUnlock} - Doing nothing.")
    }
}
    
//Label Updates
void updateLabel() {
    ifTrace("updateLabel")
    if (getAllOk() != true) {
        ifTrace("updateLabel: getAllOk = ${getAllOk()}")
        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
    } else {
        if ((state?.pause == true) || (state?.disabled == true)) {
            state.pausedOrDisabled = true
        } else {
            state.pausedOrDisabled = false
        }
        if (state?.disabled == true) {
            state.status = "(Disabled)"
            appStatus = "<span style=color:red>(Disabled)</span>"
        } else if (state?.paused == true) {
            state.status = "(Paused)"
            appStatus = "<span style=color:red>(Paused)</span>"
        } else if (lock1?.currentValue("lock") == "locked") {
            state.status = "(Locked)"
            appStatus = "<span style=color:green>(Locked)</span>"
        } else if (lock1?.currentValue("lock") == "unlocked") {
            state.status = "(Unlocked)"
            appStatus = "<span style=color:orange>(Unlocked)</span>"
        } else {
            state.paused = false
            state.disabled = false
            state.pausedOrDisabled = false
            state.status = "(Unknown)"
            appStatus = "<span style=color:pink>(Unknown)</span>"
        }
    }
    app.updateLabel("${thisName} ${appStatus}")
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
            if (lock1?.currentValue("lock") == "unlocked") {
                ifTrace("appButtonHandler: App was enabled or unpaused and lock was locked. Locking the door.")
                lockDoor()
            }
        }
    }
    updateLabel()
}

def setPauseButtonName() {
    if (state?.disabled == true) {
        state.pauseButtonName = "Disabled by Switch"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        updateLabel()
    } else if (state?.paused == true) {
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
}

// Application Page settings
private hideLockOptionsSection() {
	(minSecLock || durationLock || retryLock || maxRetriesLock || delayBetweenRetriesLock) ? false : true
}

private hideUnlockOptionsSection() {
	(minSecUnlock || durationUnlock || retryUnlock || maxRetriesUnlock || delayBetweenRetriesUnlock) ? false : true
}

private hideLoggingSection() {
	(isInfo || isDebug || isTrace || ifLevel) ? true : true
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? true : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

def getAllOk() {
    if (modeOk && daysOk && timeOk) {
        return true
    } else {
        return false
    }
}

private getModeOk() {
	def result = (!modes || modes.contains(location.mode))
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
	result
}

private getTimeOk() {
	def result = true
	if ((starting != null) && (ending != null)) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

// Logging functions
def getLogLevels() {
    return [["0":"None"],["1":"Info"],["2":"Debug"],["3":"Trace"]]
}

def turnOffLoggingTogglesIn30() {
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
}

def infoOff() {
    app.updateSetting("isInfo", false)
    log.info "${thisName}: Info logging disabled."
    app.updateSetting("isInfo",[value:"false",type:"bool"])
}

def debugOff() {
    app.updateSetting("isDebug", false)
    log.info "${thisName}: Debug logging disabled."
    app.updateSetting("isDebug",[value:"false",type:"bool"])
}

def traceOff() {
    app.updateSetting("isTrace", false)
    log.trace "${thisName}: Trace logging disabled."
    app.updateSetting("isTrace",[value:"false",type:"bool"])
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
    log.warn "${thisName}: ${msg}"
}

def ifInfo(msg) {       
    def logL = 0
    if ((ifLevel) || (isInfo == true)) logL = ifLevel.toInteger()
    if (logL == 1 && isInfo == false) {return}//bail
    else if (logL > 0) {
		log.info "${thisName}: ${msg}"
	}
}

def ifDebug(msg) {
    def logL = 0
    if ((ifLevel) || (isDebug == true)) logL = ifLevel.toInteger()
    if (logL < 2 && isDebug == false) {return}//bail
    else if (logL > 1) {
		log.debug "${thisName}: ${msg}"
    }
}

def ifTrace(msg) {       
    def logL = 0
    if ((ifLevel) || (isTrace == true)) logL = ifLevel.toInteger()
    if (logL < 3 && isTrace == false) {return}//bail
    else if (logL > 2) {
		log.trace "${thisName}: ${msg}"
    }
}
