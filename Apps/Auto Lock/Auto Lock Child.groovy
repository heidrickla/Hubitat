/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */
import groovy.transform.Field

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.16"
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
      input "detailedInstructions", "bool", title: "Enable detailed instructions?", submitOnChange:true, required: false, defaultValue: false
    }
    section("") {
        if ((state.thisName == null) || (state.thisName == "null <span style=color:white> </span>")) {state.thisName = "Enter a name for this app."}
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true, defaultValue: "Enter a name for this app."
        state.thisName = thisName
        updateLabel()
    }
    section("") {
        if (detailedInstructions == true) {paragraph "This is the lock that will all actions will activate against. The app watches for locked or unlocked status sent from the device.  If it cannot determine the current status, the last known status of the lock will be used.  If there is not a last status available and State sync fix is enabled it will attempt to determine its' state, otherwise it will default to a space. Once a device is selected, the current status will appear on the device.  The status can be updated by refreshing the page or clicking the refresh status toggle."}
        input "lock1", "capability.lock", title: "Lock:  ${lock1Status}", submitOnChange: true, required: true
        if (detailedInstructions == true) {paragraph "This is the contact sensor that will be used to determine if the door is open.  The lock will not lock while the door is open.  If it does become locked and Bolt/Frame strike protection is enabled, it will immediately try to unlock to keep from hitting the bolt against the frame. If you are having issues with your contact sensor or do not use one, it is recommended to disable Bolt/frame strike protection as it will interfere with the operation of the lock."}
        input "contact", "capability.contactSensor", title: "Door Contact:  ${contactStatus}", submitOnChange: true, required: false
        if (detailedInstructions == true) {paragraph "This option performs an immediate update to the current status of the Lock, Contact Sensor, Presence Sensor, and Status of the application.  It will automatically reset back to off after activated."}
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true, required: false
        app.updateSetting("refresh",[value:"false",type:"bool"])
    }
    section(title: "Locking Options:", hideable: true, hidden: hideLockOptionsSection()) {
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing locking actions will be based on minutes or seconds.  This will update the label on the next option to show its' setting."}
        input "minSecLock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
        if (detailedInstructions == true) {paragraph "This value is used to determine the delay before locking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (minSecLock == false) {input "durationLock", "number", title: "Lock it how many minutes later?", required: true, defaultValue: 10}
        if (minSecLock == true) {input "durationLock", "number", title: "Lock it how many seconds later?", required: true, defaultValue: 10}
        if (detailedInstructions == true) {paragraph "Enable retries if lock fails to change state enables all actions that try to lock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        input "retryLock", "bool", title: "Enable retries if lock fails to change state.", required: false, defaultValue: true
        if (detailedInstructions == true) {paragraph "Maximum number of retries is used to determine the limit of times that a locking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        input "maxRetriesLock", "number", title: "Maximum number of retries?", required: false, defaultValue: 3
        if (detailedInstructions == true) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the locking action.  If you set this too low  and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        input "delayBetweenRetriesLock", "number", title: "Delay between retries in second(s)?", require: false, defaultValue: 5
    }
    section(title: "Unlocking Options:", hideable: true, hidden: hideUnlockOptionsSection()) {
        if (detailedInstructions == true) {if (settings.whenToUnlock?.contains("2")) {paragraph "This sensor is used for presence unlock triggers."}}
        if (settings.whenToUnlock?.contains("2")) {input "unlockPresenceSensor", "capability.presenceSensor", title: "Presence:  ${unlockPresenceStatus}", submitOnChange: true, required: false, multiple: false}
//        if ((settings.whenToUnlock?.contains("2")) && (unlockPresenceSensor)) {input "allUnlockPresenceSensor", "bool", title: "Present status requires all presence sensors to be present?", submitOnChange:true, required: false, defaultValue: false}
        if (detailedInstructions == true) {paragraph "Bolt/Frame strike protection detects when the lock is locked and the door is open and immediately unlocks it to prevent it striking the frame.  This special case uses a modified delay timer that ignores the Unlock it how many minutes/seconds later and Delay between retries option.  It does obey the Maximum number of retries though."}
        if (detailedInstructions == true) {paragraph "Presence detection uses the selected presence device(s) and on arrival will unlock the door.  It is recommended to use a combined presence app to prevent false triggers.  I recommend Presence Plus and Life360 with States by BPTWorld, and the iPhone Presence driver (it works on android too).  You might need to mess around with battery optimization options to get presence apps to work reliably on your phone though."}
        if (detailedInstructions == true) {paragraph "Fire/Medical panic unlock will unlock the door whenever a specific sensor is opened.  I have zones on my alarm that trip open if one of these alarms are triggered and use an Envisalink 4 to bring over the zones into Hubitat. They show up as contact sensors.  If you have wired smoke detectors to your alarm panel, these are typically on zone 1.  You could use any sensor though to trigger."}
        if (detailedInstructions == true) {paragraph "Switch triggered unlock lets you trigger an unlock with a switch.  You can use the Switch trigger logic to flip the trigger logic to when the switch is on or off. This is different from the Switch to lock and unlock option as it is only one-way."}
        if (detailedInstructions == true) {paragraph "State sync fix is used when the lock is locked but the door becomes opened.  Since this shouldn't happen it immediately unlocks the lock and tries to refresh the lock if successful it updates the app status.  If the unlock attempt fails, it then will attempt to retry and follows any unlock delays or retry restrictions.  This option allows you to use the lock and unlock functionality and still be able to use the app when you experience sensor problems by disabling this option."}
        if (detailedInstructions == true) {paragraph "Prevent unlocking under any circumstances is used when you want to disable all unlock functionality in the app. It overrides all unlock settings including Fire/Medical panic unlock."}
        input "whenToUnlock", "enum", title: "When to unlock?  Default: '(Prevent unlocking under any circumstances)'", options: whenToUnlockOptions, defaultValue: 6, required: true, multiple: true, submitOnChange:true
        if (!settings.whenToUnlock?.contains("6")) {
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing unlocking actions will be based on minutes or seconds. This will update the label on the next option to show its' setting."}
        input "minSecUnlock", "bool", title: "Use seconds instead?", submitOnChange: true, required: true, defaultValue: true
        if (detailedInstructions == true) {paragraph "This value is used to determine the delay before unlocking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (minSecUnlock == false) {input "durationUnlock", "number", title: "Unlock it how many minutes later?", submitOnChange: true, required: true, defaultValue: 2}
        if (minSecUnlock == true) {input "durationUnlock", "number", title: "Unlock it how many seconds later?", submitOnChange: true, required: true, defaultValue: 2}
        if (detailedInstructions == true) {paragraph "Enable retries if unlock fails to change state enables all actions that try to unlock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        input "retryUnlock", "bool", title: "Enable retries if unlock fails to change state.", submitOnChange: true, require: false, defaultValue: true
        if (detailedInstructions == true) {paragraph "Maximum number of retries is used to determine the limit of times that an unlocking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        input "maxRetriesUnlock", "number", title: "Maximum number of retries? While door is open it will wait for it to close.", submitOnChange: true, required: false, defaultValue: 3
        if (detailedInstructions == true) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the unlocking action.  If you set this too low and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        input "delayBetweenRetriesUnlock", "number", title: "Delay between retries in second(s)?", submitOnChange: true, require: false, defaultValue: 3
        }
    }
    section(title: "Logging Options:", hideable: true, hidden: hideLoggingSection()) {
        if (detailedInstructions == true) {paragraph "Enable Info logging for 30 minutes will enable info logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for checking if the app is performing actions as expected."}
        input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Debug logging for 30 minutes will enable debug logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for troubleshooting problems."}
        input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Trace logging for 30 minutes will enable trace logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for following the logic inside the application but usually not neccesary."}
        input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "IDE logging level is used to permanantly set your logging level for the application.  If it is set higher than any temporary logging options you enable, it will override them.  If it is set lower than temporary logging options, they will take priority until their timer expires.  This is useful if you prefer you logging set to a low level and then can use the logging toggles for specific use cases so you dont have to remember to go back in and change them later.  It's also useful if you are experiencing issues and need higher logging enabled for longer than 30 minutes."}
        input "ifLevel","enum", title: "IDE logging level",required: true, options: logLevelOptions, defaultValue : "1"
    }
    section(title: "Only Run When:", hideable: true, hidden: hideOptionsSection()) {
        def timeLabel = timeIntervalLabel()
        if (detailedInstructions == true) {paragraph "Only during a certain time is used to restrict the app to running outside of the assigned times. You can use this to prevent false presence triggers while your sleeping from unlocking the door."}
        href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
        if (detailedInstructions == true) {paragraph "Only on certain days of the week restricts the app from running outside of the assigned days. Useful if you work around the yard frequently on the weekends and want to keep your door unlocked and just want the app during the week."}
        input "days", "enum", title: "Only on certain days of the week", submitOnChange:true, multiple: true, required: false, options: daysOptions
        if (detailedInstructions == true) {paragraph "Only when mode is allows you to prevent the app from running outside of the specified modes. This is useful if you have a party mode and want the lock from re-locking on you while company is over.  This could also be used like the Only during a certain time mode to prevent faluse triggers at night for instance."}
        input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        if (detailedInstructions == true) {paragraph "Switch to Enable and Disable this app prevents the app from performing any actions other than status updates for the lock and contact sensor state and battery state on the app page."}
        input "disabledSwitch", "capability.switch", title: "Switch to Enable and Disable this app", submitOnChange:true, required:false, multiple:true
    }
    }
}
// Application settings and startup
@Field static List<Map<String,String>> whenToUnlockOptions = [
    ["1": "Bolt/frame strike protection"],
    ["2": "Presence unlock"],
    ["3": "Fire/medical panic unlock - Not in service yet"],
    ["4": "Switch triggered unlock - Not in service yet"],
    ["5": "State sync fix"],
    ["6": "Prevent unlocking under any circumstances"]
]

@Field static List<Map<String,String>> daysOptions = [
    ["1": "Monday"],
    ["2": "Tuesday"],
    ["3": "Wednesday"],
    ["4": "Thursday"],
    ["5": "Friday"],
    ["6": "Saturday"],
    ["7": "Sunday"]
]

@Field static List<Map<String,String>> logLevelOptions = [
    ["0": "None"],
    ["1": "Info"],
    ["2": "Debug"],
    ["3": "Trace"]
]



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
    if (state?.installed == null)
	{
		state.installed = true
	}
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
    subscribe(lock1, "battery", diagnosticHandler)
    subscribe(contact, "battery", diagnosticHandler)
    subscribe(unlockPresenceSensor, "presence", diagnosticHandler)
    getAllOk()
}

// Device Handlers
def diagnosticHandler(evt) {
    ifTrace("diagnosticHandler")
    if ((lock1?.currentValue("battery") != null) && (lock1?.currentValue("lock") != null)) {lock1Status = "[ Lock: ${lock1.currentValue("lock")} ] [ Battery: ${lock1.currentValue("battery")} ]"
    } else if (lock1?.currentValue("lock") != null) {lock1Status = "[ Lock: ${lock1.currentValue("lock")} ]"
    } else if (lock1?.latestValue("lock") != null) {lock1Status = "[ Lock: ${lock1.latestValue("lock")} ]"
    } else {lock1Status = " "}
    
    if ((contact?.currentValue("battery") != null) && (contact?.currentValue("contact") != null)) {contactStatus = "[ Contact: ${contact.currentValue("contact")} ] [ Battery: ${contact.currentValue("battery")} ]"
    } else if (contact?.currentValue("contact") != null) {contactStatus = "[ Contact: ${contact.currentValue("contact")} ]"
    } else if (contact?.latestValue("contact") != null) {contactStatus = "[ Contact: ${contact.latestValue("contact")} ]"
    } else {(contactStatus = " ")}

    if ((settings.ifLevel?.contains("2")) && (unlockPresenceSensor?.currentValue("presence") != null)) {unlockPresenceSensorStatus = "[ Presence: ${unlockPresenceSensor.currentValue("presence")} ]"
    if ((settings.ifLevel?.contains("2")) && (unlockPresenceSensor?.latestValue("presence") != null)) {unlockPresenceSensorStatus = "[ Presence: ${unlockPresenceSensor.latestValue("presence")} ]"                                                                                                      
    } else {(presenceSensorStatus = " ")}
    updateLabel()
    }
}

def lockHandler(evt) {
    ifTrace("lockHandler")
    ifTrace("lockHandler: ${evt.value}")
    updateLabel()
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lockHandler: Application is paused or disabled.")
    } else {
        if (("${settings.ifLevel?.contains("1")}") && ("${!settings.ifLevel?.contains("6")}") && (lock1.currentValue("lock") == "locked") && (contact.currentValue("contact") != "closed")) {
            ifDebug("lockHandler:  Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
            lock1.unlock()
            unschedule(unlockDoor)
            def delayUnlock = 1
            runIn(delayUnlock, unlockDoor)
        } else if ((lock1.currentValue("lock") == "locked") && (contact?.currentValue("contact") == "closed" || contact == null)) {
            ifDebug("Cancelling previous lock task...")
            unschedule(lockDoor)                  // ...we don't need to lock it later.
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
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
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
        // Unlock and refresh if known state is out of sync with reality.
        } else if ((contact.currentValue("contact") == "open") && (lock1.currentValue("lock") == "locked") && ("${settings.ifLevel?.contains("5")}") && ("${!settings.ifLevel?.contains("6")}")) {
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
    if (getAllOk() == false) {
        ifTrace("TurnOffFanSwitchManual: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
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
                            if (minSecLock) {
                                def delayLock = durationLock
                                ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                                runIn(delayLock, lockDoor)
                            } else {
	                            def delayLock = durationLock * 60
                                ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
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


def unlockPresence(evt) {
    ifTrace("presenceHandler")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("unlockPresenceHanlder: Application is paused or disabled.")
        if (("${settings.ifLevel?.contains("2")}") && ("${unlockPresenceSensor.currentValue("presence")}" == "present")) {unlockDoor()}
    }
}


def deviceActivationSwitchHandler(evt) {
    ifTrace("deviceActivationSwitchHandler")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
    } else {
        updateLabel()
        if (deviceActivationSwitch) {
            deviceActivationSwitch.each { it ->
                deviceActivationSwitchState = it.currentValue("switch")
            }
            if (deviceActivationSwitchState == "on") {
                ifDebug("deviceActivationSwitchHandler: Locking the door now")
                countUnlock = maxRetriesUnlock
                state.status = "(Locked)"
                lock1.lock()
                if (minSecLock) {
                    def delayLock = durationLock
                    runIn(delayLock, lockDoor)
                } else {
                    def delayLock = durationLock * 60
                    runIn(delayLock, lockDoor)
                }
            } else if ((deviceActivationSwitchState == "off") && ("${!settings.ifLevel?.contains("6")}")) {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                countUnlock = maxRetriesUnlock
                state.status = "(Unlocked)"
                lock1.unlock()
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
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lockDoor: Application is paused or disabled.")
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
        } else if ((contact?.currentValue("contact") == "open") && (lock1?.currentValue("lock") == "locked") && ("${settings.ifLevel?.contains("1")}") && ("${!settings.ifLevel?.contains("6")}")) {
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
    if (((getAllOk() == false) || (state?.pausedOrDisabled == true)) || ("${settings.ifLevel?.contains("6")}")) {
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
    } else if ("${!settings.ifLevel?.contains("6")}"){
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
    } else if ((retryUnlock == true) && (lock1.currentValue("lock") != "unlocked") && ("${settings.ifLevel?.contains("6")}")) {
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
    if (getAllOk() == false) {
        if ((state?.paused == true) || (state?.disabled == true)) {
            state.pausedOrDisabled = true
        } else {
            state.pausedOrDisabled = false
        }
        if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}

        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
        } else {
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
            initialize()
            state.pausedOrDisabled = false
            state.status = " "
            appStatus = "<span style=color:white> </span>"
        }
    }
    if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
    app.updateLabel("${state.thisName} ${appStatus}")
}

//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Disabled by Switch") {
        state.disabled = false
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(lock1, "lock", diagnosticHandler)
        subscribe(contact, "contact", diagnosticHandler)
        subscribe(lock1, "battery", diagnosticHandler)
        subscribe(contact, "battery", diagnosticHandler)
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(lock1, "lock", diagnosticHandler)
        subscribe(contact, "contact", diagnosticHandler)
        subscribe(lock1, "battery", diagnosticHandler)
        subscribe(contact, "battery", diagnosticHandler)
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
            subscribe(disabledSwitch, "switch", disabledHandler)
            subscribe(lock1, "lock", diagnosticHandler)
            subscribe(contact, "contact", diagnosticHandler)
            subscribe(lock1, "battery", diagnosticHandler)
            subscribe(contact, "battery", diagnosticHandler)
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (lock1?.currentValue("lock") == "unlocked") {
                ifTrace("appButtonHandler: App was enabled or unpaused and lock was unlocked. Locking the door.")
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
        subscribe(lock1, "lock", diagnosticHandler)
        subscribe(contact, "contact", diagnosticHandler)
        subscribe(lock1, "battery", diagnosticHandler)
        subscribe(contact, "battery", diagnosticHandler)
        updateLabel()
    } else if (state?.paused == true) {
        state.pauseButtonName = "Resume"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(lock1, "lock", diagnosticHandler)
        subscribe(contact, "contact", diagnosticHandler)
        subscribe(lock1, "battery", diagnosticHandler)
        subscribe(contact, "battery", diagnosticHandler)
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

def getAllOk() {
    if ((modeOk && daysOk && timeOk) == true) {
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

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
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
    log.info "${state.thisName}: Info logging disabled."
    app.updateSetting("isInfo",[value:"false",type:"bool"])
}

def debugOff() {
    app.updateSetting("isDebug", false)
    log.info "${state.thisName}: Debug logging disabled."
    app.updateSetting("isDebug",[value:"false",type:"bool"])
}

def traceOff() {
    app.updateSetting("isTrace", false)
    log.trace "${state.thisName}: Trace logging disabled."
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
    log.warn "${state.thisName}: ${msg}"
}

def ifInfo(msg) {
    if (("${!settings.ifLevel?.contains("1")}" || "${!settings.ifLevel?.contains("1")}" || "${!settings.ifLevel?.contains("1")}") && (isInfo != true)) {return}//bail
    else if ("${settings.ifLevel?.contains("1")}" || "${settings.ifLevel?.contains("2")}" || "${settings.ifLevel?.contains("3")}") {log.info "${state.thisName}: ${msg}"}
}

def ifDebug(msg) {
    if (("${!settings.ifLevel?.contains("2")}" || "${!settings.ifLevel?.contains("3")}") && (isDebug != true)) {return}//bail
    else if ("${settings.ifLevel?.contains("2")}" || "${settings.ifLevel?.contains("3")}") {log.debug "${state.thisName}: ${msg}"}
}

def ifTrace(msg) {       
    if (("${!settings.ifLevel?.contains("3")}") && (isTrace != true)) {return}//bail
    else if ("${settings.ifLevel?.contains("3")}") {log.trace "${state.thisName}: ${msg}"}
}

def getVariableInfo() {
    ifTrace("state.thisName = ${state.thisName}")
    ifTrace("getAllOk = ${getAllOk()}")
    ifTrace("getModeOk = ${getModeOk()}")
    ifTrace("getDaysOk = ${getDaysOk()}")
    ifTrace("getTimeOk = ${getTimeOk()}")
    ifTrace("pausedOrDisabled = ${state.pausedOrDisabled}")
    ifTrace("state.disabled = ${state.disabled}")
    ifTrace("state.paused = ${state.paused}")
    log.info "settings.ifLevel?.contains(1) = ${settings.ifLevel?.contains("1")}"
    log.info "settings.ifLevel?.contains(2) = ${settings.ifLevel?.contains("2")}"
    log.info "settings.ifLevel?.contains(3) = ${settings.ifLevel?.contains("3")}"
}
