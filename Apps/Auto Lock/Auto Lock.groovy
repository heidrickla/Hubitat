/*   Auto Lock 
 *
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */
def setVersion(){
	state.version = "1.1.18" // Version number of this app
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
        	section("Create a new Auto Lock Instance.") {
            	app(name: "childApps", appName: "Auto Lock Child", namespace: "heidrickla", title: "New Auto Lock Instance", multiple: true)
        	}
    	}
    }
}

def installed() {
    state.AldInstalled = true
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
}
