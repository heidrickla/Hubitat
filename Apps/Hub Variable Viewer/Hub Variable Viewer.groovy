/*   Hub Variable Viewer 
 *
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Hub%20Variable%20Viewer/Hub%20Variable%20Viewer.groovy
 *
 *   Author Lewis Heidrick
 *   
 *   08/22/2022 - Project Published to GitHub
 */
import groovy.transform.Field

def setVersion(){
	state.version = "1.0.00" // Version number of this app
	state.InternalName = "Hub Variable Viewer"   // this is the name used in the JSON file for this app
}

definition(
    name: "Hub Variable Viewer",
    namespace: "heidrickla",
    singleInstance: true,
    author: "Lewis Heidrick",
    description: "Hub Variable Viewer",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Hub%20Variable%20Viewer/Hub%20Variable%20Viewer.groovy"
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Hub Variable Viewer", uninstall: true, install: true) {
        section() {
            paragraph '<i>Check local hub variable values.</i>'
            label title: "Name", required: false
            variablefun()
            input name: "variable1", type: "string", title: "Variable 1: ${variable1Output}", required: false, submitOnChange: true
            input name: "variable2", type: "string", title: "Variable 2: ${variable2Output}", required: false, submitOnChange: true
            input name: "variable3", type: "string", title: "Variable 3: ${variable3Output}", required: false, submitOnChange: true
            input name: "variable4", type: "string", title: "Variable 4: ${variable4Output}", required: false, submitOnChange: true
            input name: "variable5", type: "string", title: "Variable 5: ${variable5Output}", required: false, submitOnChange: true
            input "refresh", "bool", title: "Click here to refresh the variables", submitOnChange: true, required: false
            app.updateSetting("refresh",[value:"false",type:"bool"])
        }
        section() {
            paragraph '<i>Output values.</i>'
            variablefun()
            if (variable1 != null) paragraph "${variable1Output}"
            if (variable2 != null) paragraph "${variable2Output}"
            if (variable3 != null) paragraph "${variable3Output}"
            if (variable4 != null) paragraph "${variable4Output}"
            if (variable5 != null) paragraph "${variable5Output}"
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	initialize()
    variablefun()
}

def initialize() {
}

def variablefun() {
    variable1Output = "${location.mode}"
    variable2Output = "${variable2}"
    variable3Output = "${variable3}"
    variable4Output = "${variable4}"
    variable5Output = "${variable5}"
}
