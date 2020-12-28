/*
 *	Auto Lock (Parent)
 *
 *  Code based on Chris Sader's Auto Lock Door app and he gets full credit.
 * 
 * 
 */
def setVersion(){
	state.version = "1.0.5" // Version number of this app
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
            	app(name: "childApps", appName: "Auto Lock Child", namespace: "chris.sader", title: "New Auto Lock Instance", multiple: true)
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
