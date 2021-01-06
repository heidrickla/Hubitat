/**
*  Bathroom Humidity Fan
*
*  Turns on a fan when you start taking a shower... turns it back off when you are done.
*    -Uses humidity change rate for rapid response
*    -Timeout option when manaully controled (for stench mitigation)
*    -Child/Parent with pause/resume (Thanks to Lewis.Heidrick!)
*
*  Copyright 2018 Craig Romei
*  GNU General Public License v2 (https://www.gnu.org/licenses/gpl-2.0.txt)
*
*/
import groovy.transform.Field

def setVersion() {
    state.version = "1.1.13" // Version number of this app
    state.InternalName = "BathroomHumidityFan"   // this is the name used in the JSON file for this app
}

definition(
    name: "Bathroom Humidity Fan Child",
    namespace: "Craig.Romei",
    author: "Craig Romei",
    description: "Control a fan (switch) based on relative humidity.",
    category: "Convenience",
    parent: "Craig.Romei:Bathroom Humidity Fan",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/napalmcsr/Hubitat_Napalmcsr/master/Apps/BathroomHumidityFan/BathroomHumidityChild.src")

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
    dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {
    ifTrace("mainPage")
    turnOffLoggingTogglesIn30()
    setPauseButtonName()
    diagnosticHandler()
        
    section("") {
        input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
    }
    section("") {
        if ((state.thisName == null) || (state.thisName == "null <span style=color:white> </span>")) {state.thisName = "Enter a name for this app."}
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true
        state.thisName = thisName
        updateLabel()
    }
	section("") {
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
        input "FanSwitch", "capability.switch", title: "${fanSwitchStatus}", required: true, submitOnChange:true
        input "HumiditySensor", "capability.relativeHumidityMeasurement", title: "${humiditySensorStatus}", required: true, submitOnChange:true
        paragraph "NOTE: The humidity sensor you select will need to report about 5 min or less."
        input "humidityResponseMethod", "enum", title: "Humidity Response Method", options: humidityResponseMethodOptions, defaultValue: 1, required: true, multiple: false, submitOnChange:true
        app.updateSetting("refresh",[value:"false",type:"bool"])
    }    
    if ((settings.humidityResponseMethod?.contains("3")) || (settings.humidityResponseMethod?.contains("4"))) {
        section("Comparison Sensor", hideable: true, hidden: hideComparisonSensorSection()) {
        input "CompareHumiditySensor", "capability.relativeHumidityMeasurement", title: "${compareHumiditySensorStatus}", required: true, submitOnChange:true
        if (settings.humidityResponseMethod?.contains("4")) {input "CompareHumiditySensorOffset", "number", title: "Comparison Offset Trigger", required: true, submitOnChange:true
        paragraph "How much deviation from the comparison sensor do you want to trigger the fan? This will set the comparison sensor to be the threshold plus this offset."}
        }
    }
	section("<b><u>Fan Activation</u></b>"){
        input "HumidityIncreaseRate", "number", title: "Humidity Increase Rate:", required: true, defaultValue: 2
        input "HumidityThreshold", "number", title: "Humidity Threshold (%):", required: false, defaultValue: 65
        if (settings.humidityResponseMethod?.contains("4")) {
        input "HumidityIncreasedBy", "number", title: "When humidity rises above or equal to this amount plus the baseline sensor humidity turn on the fan: ", required: false, defaultValue: 9}
        input "FanOnDelay", "number", title: "Delay turning fan on (Minutes):", required: false, defaultValue: 0
    }
    section("<b><u>Fan Deactivation</b></u>") {
        input "HumidityDropTimeout", "number", title: "How long after the humidity starts to drop should the fan turn off (minutes):", required: true, defaultValue:  10
        input "HumidityDropLimit", "number", title: "What percentage above the starting humidity before triggering the turn off delay:", required: true, defaultValue:  25
        input "MaxRunTime", "number", title: "Maximum time(minutes) for Fan to run when automatically turned on:", required: false, defaultValue: 120    
    }
    section("<b><u>Manual Activation</b></u>") {
        input "ManualControlMode", "enum", title: "When should the fan turn off when turned on manually?", submitOnChange:true, required: true, options: manualControlModeOptions, defaultValue: 2
        if (settings.ManualControlMode?.contains("2")) {input "ManualOffMinutes", "number", title: "How many minutes until the fan is auto-turned-off?", submitOnChange:true, required: false, defaultValue: 20}
    }
    section(title: "Additional Features:", hideable: true, hidden: hideOptionsSection()) {
        input "deviceActivationSwitch", "capability.switch", title: "Switches to turn on and off the fan immediately.", submitOnChange:false, required:false, multiple:true
    }
    section("Logging Options", hideable: true, hidden: hideLoggingSection()) {
        input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, defaultValue: false
        input "ifLevel","enum", title: "IDE logging level",required: true, options: getLogLevels, defaultValue : "1"
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
@Field static List<Map<String,String>> humidityResponseMethodOptions = [
    ["1": "Rate of change"],
    ["2": "Humidity over fixed threshold"],
    ["3": "Rate of change with comparison sensor"],
    ["4": "Humidity over comparison sensor"]
]

@Field static List<Map<String,String>> manualControlModeOptions = [
    ["1": "By Humidity"],
    ["2": "After Set Time"],
    ["3": "Manually"],
    ["4": "Never"]
]

def installed() {
    ifTrace("installed")
    state.installed = true
    initialize()
}

def updated() {
    ifDebug("Bathroom Humidity Fan Updated")
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
    defaultHumidityThresholdValue = 65
    state.OverThreshold = false
    state.AutomaticallyTurnedOn = false
    state.TurnOffLaterStarted = false
    subscribe(deviceActivationSwitch, "switch", deviceActivationSwitchHandler)
    subscribe(disabledSwitch, "switch", disabledHandler)
    subscribe(FanSwitch, "switch", diagnosticHandler)
    subscribe(HumiditySensor, "humidity", diagnosticHandler)
    subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
    if ((settings.humidityResponseMethod?.contains("3")) || (settings.humidityResponseMethod?.contains("4"))) {
        subscribe(CompareHumiditySensor, "humidity", compareHumidityHandler)} else {unsubscribe(compareHumidtySensor)}
    subscribe(FanSwitch, "switch", FanSwitchHandler)
    subscribe(HumiditySensor, "humidity", HumidityHandler)
    subscribe(location, "mode", modeChangeHandler)
    diagnosticHandler()
    updateLabel()
    getAllOk()
}

// Device Handlers
def diagnosticHandler(evt) {
    ifTrace("diagnosticHandler")
    if (FanSwitch?.currentValue("switch") != null) {fanSwitchStatus = "[ Fan: ${FanSwitch.currentValue("switch")} ]"
    } else if (FanSwitch?.latestValue("switch") != null) {fanSwitchStatus = "Fan: ${FanSwitch.latestValue("switch")}"} else {fanSwitchStatus = " "}

    if ((HumiditySensor?.currentValue("battery") != null) && (HumiditySensor?.currentValue("humidity") != null)) {humiditySensorStatus = "[ Humidity: ${HumiditySensor.currentValue("humidity")} ]  [ Battery: ${HumiditySensor.currentValue("battery")} ]"
    } else if (HumiditySensor?.currentValue("humidity") != null) {humiditySensorStatus = "[Humidity: ${HumiditySensor.currentValue("humidity")}]"
    } else if (HumiditySensor?.latestValue("humidity") != null) {humiditySensorStatus = "[Humidity: ${HumiditySensor.latestValue("humidity")}]"
    } else {
        humiditySensorStatus = " "   
    }
    if ((CompareHumiditySensor?.currentValue("battery") != null) && (CompareHumiditySensor?.currentValue("humidity") != null)) {compareHumiditySensorStatus = "[ Humidity: ${CompareHumiditySensor.currentValue("humidity")} ] [ Battery: ${CompareHumiditySensor.currentValue("battery")} ]"
    } else if (CompareHumiditySensor?.currentValue("humidity") != null) {compareHumiditySensorStatus = "[ Humidity: ${CompareHumiditySensor.currentValue("humidity")} ]"
    } else if (compareHumiditySensor?.latestValue("humidity") != null) {compareHumiditySensorStatus = "[ Humidity: ${CompareHumiditySensor.latestValue("humidity")} ]"
    } else {
        compareHumiditySensorStatus = " "   
    }
    updateLabel()
}

def modeChangeHandler(evt) {
	ifTrace("modeChangeHandler")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
        ifDebug("modeChangeHandler: Entered a disabled mode, turning off the Fan")
	    TurnOffFanSwitch()
        updateLabel()
    }
}

// Humidity Handler Methods
def HumidityHandler(evt) {
	ifInfo("HumidityHandler: Running Humidity Check")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("HumidityHandler: getAllOk = ${getAllOk()}")
    } else {
	    humidityHandlerVariablesBefore()
	    state.OverThreshold = CheckThreshold(evt)
	    state.lastHumidityDate = state.currentHumidityDate
        if (state?.currentHumidity) {state.lastHumidity = state.currentHumidity} else {state.lastHumidity = 100}
	    if (!state?.StartingHumidity) {state.StartingHumidity = 100}
        if (!state?.HighestHumidity) {state.HighestHumidity = 100}
	    state.currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	    state.currentHumidityDate = evt.date.time
	    state.HumidityChangeRate = state.currentHumidity - state.lastHumidity
	    if (state?.currentHumidity > state.HighestHumidity)	{state.HighestHumidity = state.currentHumidity}
	    state.targetHumidity = state.StartingHumidity+HumidityDropLimit/100*(state.HighestHumidity-state.StartingHumidity)              
	    humidityHandlerVariablesAfter()
	    //if the humidity is high (or rising fast) and the fan is off, kick on the fan
        if (settings.humidityResponseMethod?.contains("1")) {humidityRateOfChangeOn()}
        if (settings.humidityResponseMethod?.contains("3")) {compareHumidityRateOfChangeOn()}
	    //turn off the fan when humidity returns to normal and it was kicked on by the humidity sensor
        if (settings.humidityResponseMethod?.contains("1")) {humidityRateOfChangeOff()}
        if (settings.humidityResponseMethod?.contains("3")) {compareHumidityRateOfChangeOff()}
    }
}

def CheckThreshold(evt) {
	ifTrace("CheckThreshold")
    if (Double.parseDouble(evt.value.replace("%", "")) >= HumidityThreshold) {  
		ifInfo("IsHumidityPresent: Humidity is above the Threshold")
		return true
    } else {
		return false
	}
}

def compareHumidityHandler(evt) {
    ifTrace("compareHumidityHandler")
    compareHumidityValue = (evt.value)
    if (settings.humidityResponseMethod?.contains("3") && CompareHumiditySensor && compareHumidityValue) {
    }
    if (settings.humidityResponseMethod?.contains("4") && CompareHumiditySensor && compareHumidityValue && CompareHumiditySensorOffset) {
        compareHumidityValueAndOffset = (compareHumidityValue + CompareHumiditySensorOffset)
        app.updateSetting("HumidityThreshold",[type: "number", value:"compareHumidityValueAndOffset"])
    }
}

def FanSwitchHandler(evt) {
    ifTrace("FanSwitchHandler")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("FanSwitchHandler: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
    updateLabel()
    if (evt.value == "on") {
        if (!state?.AutomaticallyTurnedOn && (settings.manualControlModeOptions?.contains("2")) && ManualOffMinutes) {
            if (ManualOffMinutes == 0) {
                ifDebug("FanSwitchHandler: Turning the Fan off now")
                TurnOffFanSwitch()
                state.status = "(Off)"
            } else if (FanSwitch.currentValue("switch") == "on") {
                ifDebug("FanSwitchHandler: Will turn off later")
                runIn(60 * ManualOffMinutes.toInteger(), TurnOffFanSwitch)
                state.status = "(On)"
            } else {
                (FanSwitch.currentValue("switch") == "off")
                ifDebug("FanSwitchHandler: Switch already turned off manually")
                state.status = "(Off)"
            }
        }    
    } else if (evt.value == "off") {
        ifDebug("FanSwitchHandler: Switch turned off")
        state.status = "(Off)"
        state.AutomaticallyTurnedOn = false
        state.TurnOffLaterStarted = false
        unschedule()
        }
    }
    updateLabel()
}

def disabledHandler(evt) {
    ifTrace("disabledHandler")
    if (getAllOk == false) {
    ifTrace("TurnOffFanSwitchManual: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
        } else {
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
                        if (FanSwitch.currentValue("switch") == "off") {
                            state.status = "(Off)"
                            ifDebug("disabledHandler: App was enabled or unpaused and fan was off.")
                        }
                    }
                } else if (disabledSwitchState == "off") {
                    state.pauseButtonName = "Disabled by Switch"
                    state.status = "(Disabled)"
                    state.disabled = true
                    updateLabel()
                    ifDebug("disabledHandler: App was disabled and fan is ${FanSwitch.currentValue("switch")}.")
                }
            }
        }
        updateLabel()
    }
}

def deviceActivationSwitchHandler(evt) {
    ifTrace("deviceActivationSwitchHandler")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("deviceActivationSwitchHandler: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        updateLabel()
        if(deviceActivationSwitch) {
            deviceActivationSwitch.each { it ->
                deviceActivationSwitchState = it.currentValue("switch")
            }
            if (deviceActivationSwitchState == "on") {
                ifDebug("deviceActivationSwitchHandler: Turning on the fan now")
                state.status = "(On)"
                TurnOnFan()
            } else if (deviceActivationSwitchState == "off") {
                ifDebug("deviceActivationSwitchHandler: Turning off the fan now")
                TurnOffFanSwitch()
            }
        }
        updateLabel()
    }
}


// Application functions
def humidityRateOfChangeOn() {
    ifTrace("humidityRateOfChangeOn")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("humidityRateOfChangeOff: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        ifTrace("HumidityHandler: modeOk = ${modeOk}")
       if (((state?.HumidityChangeRate > HumidityIncreaseRate) || state?.OverThreshold) && (FanSwitch?.currentValue("switch") == "off") && modeOk && !state.AutomaticallyTurnedOn) {
            ifTrace("If the humidity is high (or rising fast) and the fan is off, kick on the fan")
            state.TurnOffLaterStarted = false
            if ((FanOnDelay > 0) && (FanOnDelay != null)) {
                ifDebug("humidityRateOfChangeOn: Turning on fan later")
                runIn(60 * FanOnDelay.toInteger(), TurnOnFan)
            } else {
                ifDebug("humidityRateOfChangeOn: Turning on fan due to humidity increase")
	            state.AutomaticallyTurnedOn = true
                TurnOnFan()
            }
            state.StartingHumidity = state.lastHumidity
            state.HighestHumidity = state.currentHumidity    
            ifTrace("humidityRateOfChangeOn: new state.StartingHumidity = ${state.StartingHumidity}")
            ifTrace("humidityRateOfChangeOn: new state.HighestHumidity = ${state.HighestHumidity}")
            ifTrace("humidityRateOfChangeOn: new state.targetHumidity = ${state.targetHumidity}")
       }
    }
}

def humidityRateOfChangeOff() {
    ifTrace("humidityRateOfChangeOff")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("humidityRateOfChangeOff: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        if ((state?.AutomaticallyTurnedOn || settings.manualControlModeOptions?.contains("1")) && !state.TurnOffLaterStarted) {
            ifTrace("humidityRateOfChangeOff: state?.currentHumidity = ${state?.currentHumidity} state?.targetHumidity = ${state?.targetHumidity}")
            if (state?.currentHumidity <= state?.targetHumidity) {
               if (HumidityDropTimeout == 0) {
                    ifDebug("humidityRateOfChangeOff: Fan Off")
                    TurnOffFanSwitch()
                    ifDebug("humidityRateOfChangeOff: Turning off the fan. Humidity has returned to normal and it was kicked on by the humidity sensor.")
                } else {
                    ifInfo ("humidityRateOfChangeOff: Turn Fan off in ${HumidityDropTimeout} minutes.")
                    state.TurnOffLaterStarted = true
                    runIn(60 * HumidityDropTimeout.toInteger(), TurnOffFanSwitchCheckHumidity)
                    ifDebug("Turning off the fan in ${60 * HumidityDropTimeout.toInteger()} minutes.")
                    ifTrace("humidityRateOfChangeOff: state.TurnOffLaterStarted = ${state.TurnOffLaterStarted}")
               }
            }
        }
    }
}

def compareHumidityRateOfChangeOn() {
    ifTrace("compareHumidityRateOfChangeOn")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("compareHumidityRateOfChangeOn: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
    ifTrace("compareHumidityRateOfChangeOn: modeOk = ${modeOk}")
        if (((state?.HumidityChangeRate > HumidityIncreaseRate) || state?.OverThreshold) && (FanSwitch?.currentValue("switch") == "off") && modeOk && !state.AutomaticallyTurnedOn) {
            ifTrace("If the humidity is high (or rising fast) and the fan is off, kick on the fan")
            state.TurnOffLaterStarted = false
            if ((FanOnDelay > 0) && (FanOnDelay != null)) {
                ifDebug("compareHumidityRateOfChangeOn: Turning on fan later")
                runIn(60 * FanOnDelay.toInteger(), TurnOnFan)
            } else {
                ifDebug("compareHumidityRateOfChangeOn: Turning on fan due to humidity increase")
	            state.AutomaticallyTurnedOn = true
                TurnOnFan()
            }
            state.StartingHumidity = state.lastHumidity
            state.HighestHumidity = state.currentHumidity    
            ifTrace("compareHumidityRateOfChangeOn: new state.StartingHumidity = ${state.StartingHumidity}")
            ifTrace("compareHumidityRateOfChangeOn: new state.HighestHumidity = ${state.HighestHumidity}")
            ifTrace("compareHumidityRateOfChangeOn: new state.targetHumidity = ${state.targetHumidity}")
        }
    }
}

def compareHumidityRateOfChangeOff() {
    ifTrace("compareHumidityRateOfChangeOff")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("compareHumidityRateOfChangeOff: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        if ((state?.AutomaticallyTurnedOn || settings.manualControlModeOptions?.contains("1")) && !state.TurnOffLaterStarted) {
            ifTrace("compareHumidityRateOfChangeOff: state?.currentHumidity = ${state?.currentHumidity} state?.targetHumidity = ${state?.targetHumidity}")
            if (state?.currentHumidity <= state?.targetHumidity) {
                if (HumidityDropTimeout == 0) {
                    ifDebug("compareHumidityRateOfChangeOff: Fan Off")
                    TurnOffFanSwitch()
                    ifDebug("compareHumidityRateOfChangeOff: Turning off the fan. Humidity has returned to normal and it was kicked on by the humidity sensor.")
                } else {
                    ifInfo ("compareHumidityRateOfChangeOff: Turn Fan off in ${HumidityDropTimeout} minutes.")
                    state.TurnOffLaterStarted = true
                    runIn(60 * HumidityDropTimeout.toInteger(), TurnOffFanSwitchCheckHumidity)
                    ifDebug("Turning off the fan in ${60 * HumidityDropTimeout.toInteger()} minutes.")
                    ifTrace("compareHumidityRateOfChangeOff: state.TurnOffLaterStarted = ${state.TurnOffLaterStarted}")
                }
            }
        }
    }
}

def TurnOffFanSwitchMaxTime() {
    ifTrace("TurnOffFanSwitchMaxTime")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("TurnOffFanSwitchMaxTime: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        updateLabel()
        TurnOffFanSwitch()
    updateLabel()
    }
}

def TurnOffFanSwitchCheckHumidity() {
    ifTrace("TurnOffFanSwitchCheckHumidity")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("TurnOffFanSwitchCheckHumidity: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        updateLabel()
        if (FanSwitch?.currentValue("switch") == "on") {
            if(state?.currentHumidity > state.targetHumidity) {
                ifDebug("TurnOffFanSwitchCheckHumidity: Didn't turn off fan because humidity rate is ${state.HumidityChangeRate}")
                state.AutomaticallyTurnedOn = true
                state.TurnOffLaterStarted = false
                state.status = "(On)"
            }
	    } else {
	        TurnOffFanSwitch()
	    }
    updateLabel()
	}
}

def TurnOffFanSwitch() {
    ifTrace("TurnOffFanSwitch")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("urnOffFanSwitch: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        updateLabel()
        ifInfo("TurnOffFanSwitch: Turning the Fan off now")
        FanSwitch.off()
        state.status = "(Off)"
        state.AutomaticallyTurnedOn = false
        state.TurnOffLaterStarted = false
        updateLabel()
    }
}

def TurnOffFanSwitchManual() {
    ifTrace("TurnOffFanSwitchManual")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("TurnOffFanSwitchManual: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        updateLabel()
        if (state?.AutomaticallyTurnedOn == false) {
            ifInfo("TurnOffFanSwitchManual: Turning the Fan off now")
            FanSwitch.off()
            state.status = "(Off)"
            updateLabel()
            state.AutomaticallyTurnedOn = false
            state.TurnOffLaterStarted = false
        } else {
            ifInfo("Not turning off switch, either the switch was off or the Auto routine kicked in")
        }
    updateLabel()
    }
}

def TurnOnFan() {
    ifTrace("TurnOnFan")
    if ((getAllOk == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("TurnOffFanSwitchManual: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
    } else {
        FanSwitch.on()
        state.status = "(On)"
        updateLabel()
        if (MaxRunTime) {
            ifDebug("Maximum run time is ${MaxRunTime} minutes")
            runIn(60 * MaxRunTime.toInteger(), TurnOffFanSwitchMaxTime)
        }
    updateLabel()
    }
}
                  
def changeMode(mode) {
    ifTrace("changeMode")
    ifDebug("Changing Mode to: ${mode}")
	if (location?.mode != mode && location.modes?.find { it.name == mode}) setLocationMode(mode)
}

def humidityHandlerVariablesBefore() {
    ifDebug("HumidityHandler: Before")
    ifDebug("HumidityHandler: state.OverThreshold = ${state.OverThreshold}")
	ifDebug("HumidityHandler: state.AutomaticallyTurnedOn = ${state.AutomaticallyTurnedOn}")
	ifDebug("HumidityHandler: state.TurnOffLaterStarted = ${state.TurnOffLaterStarted}")
	ifDebug("HumidityHandler: state.lastHumidity = ${state.lastHumidity}")
	ifDebug("HumidityHandler: state.lastHumidityDate = ${state.lastHumidityDate}")
	ifDebug("HumidityHandler: state.currentHumidity = ${state.currentHumidity}")
	ifDebug("HumidityHandler: state.currentHumidityDate = ${state.currentHumidityDate}")
	ifDebug("HumidityHandler: state.StartingHumidity = ${state.StartingHumidity}")
	ifDebug("HumidityHandler: state.HighestHumidity = ${state.HighestHumidity}")
	ifDebug("HumidityHandler: state.HumidityChangeRate = ${state.HumidityChangeRate}")
	ifDebug("HumidityHandler: state.targetHumidity = ${state.targetHumidity}")
}

def humidityHandlerVariablesAfter() {
    ifDebug("HumidityHandler: After")
    ifDebug("HumidityHandler: state.OverThreshold = ${state.OverThreshold}")
	ifDebug("HumidityHandler: state.AutomaticallyTurnedOn = ${state.AutomaticallyTurnedOn}")
	ifDebug("HumidityHandler: state.TurnOffLaterStarted = ${state.TurnOffLaterStarted}")
	ifDebug("HumidityHandler: state.lastHumidity = ${state.lastHumidity}")
	ifDebug("HumidityHandler: state.lastHumidityDate = ${state.lastHumidityDate}")
	ifDebug("HumidityHandler: state.currentHumidity = ${state.currentHumidity}")
	ifDebug("HumidityHandler: state.currentHumidityDate = ${state.currentHumidityDate}")
	ifDebug("HumidityHandler: state.StartingHumidity = ${state.StartingHumidity}")
	ifDebug("HumidityHandler: state.HighestHumidity = ${state.HighestHumidity}")
	ifDebug("HumidityHandler: state.HumidityChangeRate = ${state.HumidityChangeRate.round(2)}")
	ifDebug("HumidityHandler: state.targetHumidity = ${state.targetHumidity}")
	ifDebug("HumidityHandler: FanSwitch.current state = ${FanSwitch.currentValue("switch")}")
}

//Label Updates
void updateLabel() {
    if (getAllOk == false) {
        ifTrace("updateLabel: getAllOk = ${getAllOk()}")
        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
    } else {
        if ((state?.pause == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
        if (state?.disabled == true) {
            state.status = "(Disabled)"
            appStatus = "<span style=color:red>(Disabled)</span>"
        } else if (state?.paused == true) {
            state.status = "(Paused)"
            appStatus = "<span style=color:red>(Paused)</span>"
        } else if (FanSwitch?.currentValue("switch") == "on") {
            state.status = "(On)"
            appStatus = "<span style=color:green>(On)</span>"
        } else if (FanSwitch?.currentValue("switch") == "off") {
            state.status = "(Off)"
            appStatus = "<span style=color:blue>(Off)</span>"
        } else {
            state.paused = false
            state.disabled = false
            state.pausedOrDisabled = false
            state.status = " "
            appStatus = "<span style=color:white>(Unknown)</span>"
        }
    }
    app.updateLabel("${state.thisName} ${appStatus}")
}

//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Disabled by Switch") {
        state.disabled = false
        unschedule()
        unsubscribe()
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(FanSwitch, "switch", diagnosticHandler)
        subscribe(HumiditySensor, "humidity", diagnosticHandler)
        subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(FanSwitch, "switch", diagnosticHandler)
        subscribe(HumiditySensor, "humidity", diagnosticHandler)
        subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
            subscribe(disabledSwitch, "switch", disabledHandler)
            subscribe(FanSwitch, "switch", diagnosticHandler)
            subscribe(HumiditySensor, "humidity", diagnosticHandler)
            subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (FanSwitch?.currentValue("switch") == "on") {
                ifTrace("appButtonHandler: App was enabled or unpaused and fan was on. Turning off the fan.")
                TurnOffFanSwitch()
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
        subscribe(FanSwitch, "switch", diagnosticHandler)
        subscribe(HumiditySensor, "humidity", diagnosticHandler)
        subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
        updateLabel()
    } else if (state?.paused == true) {
        state.pauseButtonName = "Resume"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(FanSwitch, "switch", diagnosticHandler)
        subscribe(HumiditySensor, "humidity", diagnosticHandler)
        subscribe(CompareHumiditySensor, "humidity", diagnosticHandler)
        updateLabel()
    } else {
        state.pauseButtonName = "Pause"
        initialize()
        updateLabel()
    }
}


// Application Page settings
private hideComparisonSensorSection() {
	(CompareHumiditySensor || CompareHumiditySensorOffset) ? false : true
}

private hideLoggingSection() {
	(isInfo || isDebug || isTrace || ifLevel) ? true : true
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? true : true
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

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

// Logging functions
@Field static List<Map<String,String>> getLogLevels = [
    ["0": "None"],
    ["1": "Info"],
    ["2": "Debug"],
    ["3": "Trace"]
]

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
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL == 1 && isInfo == false) {return}//bail
    else if (logL > 0) {
		log.info "${thisName}: ${msg}"
	}
}

def ifDebug(msg) {
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 2 && isDebug == false) {return}//bail
    else if (logL > 1) {
		log.debug "${thisName}: ${msg}"
    }
}

def ifTrace(msg) {       
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 3 && isTrace == false) {return}//bail
    else if (logL > 2) {
		log.trace "${thisName}: ${msg}"
    }
}
