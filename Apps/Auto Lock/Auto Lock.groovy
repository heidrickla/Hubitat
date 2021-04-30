/*   Auto Lock 
 *
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */
import groovy.transform.Field

def setVersion(){
	state.version = "1.1.51" // Version number of this app
	state.InternalName = "Auto Lock"   // this is the name used in the JSON file for this app
}

definition(
    name: "Auto Lock",
    namespace: "heidrickla",
    singleInstance: true,
    author: "Lewis Heidrick",
    description: "Auto Lock - Parent Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock.groovy"
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.AldInstalled) {
            section("Hit Done to install Auto Lock App") {
        	}
        }
        else {
        	section("<b>Create a new Auto Lock Instance.</b>") {
            	app(name: "childApps", appName: "Auto Lock Child", namespace: "heidrickla", title: "New Auto Lock Instance", multiple: true)
        	}
            section("<b>Create a combined presence sensor.</b>") {
                paragraph "Presence will update from present to not present once all devices are away. It will update from not present to present when any device arrives."
			    input "presSensors", "capability.presenceSensor", title: "Select Presence Sensors", submitOnChange: true, required: false, multiple: true
                input name: "Create", type: "button", title: state.createCombinedSensorButtonName, submitOnChange:true, width: 5
                displayFooter()
            }
    	}
    }
}

def installed() {
    state.AldInstalled = true
	initialize()
}

def updated() {
	initialize()
    if (state?.created == null) {(state.created = false)}
}

def initialize() {
    if (presSensors) {subscribe(presSensors, "presence", handler)}
    setCreateCombinedSensorButtonName()
}

def handler(evt) {
	def present = false
    presSensors.each { presSensor ->
		if (presSensor.currentValue("presence") == "present") {present = true}
    }
    def previousPresent = (getChildDevice("CombinedPres_${app.id}")).currentValue("presence")
	if (present) {(getChildDevice("CombinedPres_${app.id}")).arrived()}
	else {(getChildDevice("CombinedPres_${app.id}")).departed()}
    
    //if (getChildDevice("CombinedPres_${app.id}")) {(getChildDevice("CombinedPres_${app.id}")).setHumidity(averageHumid())}
}

def setCreateCombinedSensorButtonName() {
    if (getChildDevice("CombinedPres_${app.id}")) {
        state.createCombinedSensorButtonName = "Delete Combined Presence Sensor"
    } else if (!getChildDevice("CombinedPres_${app.id}")) {
        state.createCombinedSensorButtonName = "Create Combined Presence Sensor"
    }
}

def appButtonHandler(btn) {
    state.created = !state.created
    if (!getChildDevice("CombinedPres_${app.id}")) {
        addChildDevice("hubitat", "Virtual Presence", "CombinedPres_${app.id}", null, [label: thisName, name: thisName])
        (getChildDevice("CombinedPres_${app.id}")).updateSetting("txtEnable",[value:false, type:"bool"])
    } else if (state.createCombinedSensorButtonName =="Delete Combined Presence Sensor") {
        (deleteChildDevice("CombinedPres_${app.id}"))
    }
setCreateCombinedSensorButtonName()
}

def displayFooter(){
	section() {
		paragraph "<div style='color:#1A77C9;text-align:center'>Auto Lock<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=3MPZ3GU5XL8RS&item_name=Hubitat+Development&currency_code=USD' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br>Buy me a beer!</div>"
	}       
}
