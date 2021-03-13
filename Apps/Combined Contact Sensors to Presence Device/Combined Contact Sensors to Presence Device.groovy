/*   Auto Lock 
 *
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Combined%20Contact%20To%20Presence/Combined%20Contact%20To%20Presence.groovy"
 *   
 *   3/13/2021 - Project Published to GitHub
 */
import groovy.transform.Field

def setVersion(){
	state.version = "1.1.47" // Version number of this app
	state.InternalName = "Combined Contact Sensors to Presence Device"   // this is the name used in the JSON file for this app
}

definition(
    name: "Combined Contact Sensors to Presence Device",
    namespace: "heidrickla",
    singleInstance: true,
    author: "Lewis Heidrick",
    description: "Combined Contact Sensors to Presence Device",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Combined%20Contact%20To%20Presence/Combined%20Contact%20To%20Presence.groovy"
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
    installed()
    return dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        section("<b>Create a virtual presence sensor from one or more contact sensor(s).</b>") {
            paragraph "Presence will update from present to not present when the contact is open. It will update from not present to present when the contact closes."
			input "contactSensors", "capability.contactSensor", title: "Select Contact Sensors", submitOnChange: true, required: true, multiple: true
            paragraph "If any sensors are closed then it will set the presence to present."
            input name: "Create", type: "button", title: state.createCombinedSensorButtonName, submitOnChange:true, width: 5
    	}
        displayFooter()
    }
}

def installed() {
    state.C2PInstalled = true
	initialize()
}

def updated() {
    unsubscribe()
	initialize()
    if (state?.created == null) {(state.created = false)}
}

def initialize() {
    if (contactSensors) {subscribe(contactSensors, "contact.open", handler)}
    if (contactSensors) {subscribe(contactSensors, "contact.closed", handler)}
    setCreateCombinedSensorButtonName()
}

def handler(evt) {
	def present = false
    contactSensors.each { contactSensor ->
        if (contactSensor.currentValue("contact") == "closed") {present = true}
    }
    def previousPresent = (getChildDevice("CombinedContact_${app.id}")).currentValue("presence")
	if (present) {(getChildDevice("CombinedContact_${app.id}")).arrived()}
	else {(getChildDevice("CombinedContact_${app.id}")).departed()}
}

def setCreateCombinedSensorButtonName() {
    if (getChildDevice("CombinedContact_${app.id}")) {
        state.createCombinedSensorButtonName = "Delete Combined Contact Sensor"
    } else if (!getChildDevice("CombinedContact_${app.id}")) {
        state.createCombinedSensorButtonName = "Create Combined Contact Sensor"
    }
}

def appButtonHandler(btn) {
    state.created = !state.created
    if (!getChildDevice("CombinedContact_${app.id}")) {
        addChildDevice("hubitat", "Virtual Presence", "CombinedContact_${app.id}", null, [label: thisName, name: thisName])
        (getChildDevice("CombinedContact_${app.id}")).updateSetting("txtEnable",[value:false, type:"bool"])
    } else if (state.createCombinedSensorButtonName =="Delete Combined Contact Sensor") {
        (deleteChildDevice("CombinedContact_${app.id}"))
    }
setCreateCombinedSensorButtonName()
}

def displayFooter(){
	section() {
		paragraph "<div style='color:#1A77C9;text-align:center'>Combined Contact Sensors to Presence Device<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=3MPZ3GU5XL8RS&item_name=Hubitat+Development&currency_code=USD' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br>Buy me a beer!</div>"
	}       
}
