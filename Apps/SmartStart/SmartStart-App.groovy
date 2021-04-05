import groovy.json.JsonSlurper
import groovy.transform.Field
//include 'asynchttp_v1'

def setVersion(){
	state.version = "1.1.49" // Version number of this app
	state.InternalName = "SmartStart"   // this is the name used in the JSON file for this app
}

definition(
   name: "SmartStart",
   namespace: "heidrickla",
   author: "Lewis Heidrick",
   description: "SmartStart SmartApp",
   category: "Convenience",
   iconUrl: "",
   iconX2Url: "",
   iconX3Url: "",
   singleInstance: true) 
   {
   appSetting "Username"
   appSetting "Password"
   }


def getApiEndpoint() {
 return "https://www.vcp.cloud"
}

preferences {

    page(name: "mainPage", title: "Simple Automations", install: true, uninstall: true,submitOnChange: true) {
        section("SmartStart Credentials") {
            input("Username", "text", title:"SmartStart Username", description: "Your SmartStart Username" , required: true, defaultValue: "guarddog13@yahoo.com", displayDuringSetup: true)
            input("Password", "password", title:"SmartStart Password", description: "Your SmartStart Password", required: true, defaultValue: "Spr!ng2021", displayDuringSetup: true)
        }
        section("Current Settings") {
            setButtonName()
            input name: "Get New Token", type: "button", title: state.buttonName, submitOnChange:true
        }
        displayFooter()
        
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    Date latestdate = new Date();
	log.debug "Current Token: ${state.token} Lifespan: ${state.tokenexpiry}"
    
    if(state.token == null ) {
        //Need new SmartStartToken
        GetToken()
    } else if(state?.tokenexpiry == null){
        //Need new SmartStartToken
        GetToken()
    } else if(state?.tokenexpiry - latestdate.getTime() <0) {
    log.debug "MS left on token: ${state.tokenexpiry - latestdate.getTime()}"
        //Need new SmartStartToken
        GetToken()
    }
    
    //Get Devices From SmartStart
	GetCars()
    
    def carList = state.carChild ?: [:]
    def cars = state.carList.collect {
    
    dni ->
        // log.debug "PLEASE HERE: ${dni}"
        def children = app.getChildDevices()
        log.debug children.find{iee -> iee.deviceNetworkId == String.valueOf(dni.id)}
        def childDevice = children.find{it.deviceNetworkId == String.valueOf(dni.id)}

        if (!childDevice) {
            childDevice = addChildDevice("heidrickla", "SmartStartCar", String.valueOf(dni.id), null, [name: "SmartStart.${dni.id}", label: "${dni.vehicleMake} ${dni.vehicleModel}" ?: "nullname", completedSetup: true])
            childDevice.doRefresh()
            log.debug "Device Creation - created ${childDevice.displayName} with id $dni.id"
        } else {
            log.debug "Device Creation - ${childDevice.displayName} with id $dni.id already exists"
        }
    return childDevice
    }
}

//Called when smartapp is uninstalled
def uninstalled() {
	removeChildDevices(getChildDevices())
}

def GetCars(){
    //Check if token needed
    Date latestdate = new Date();
    if(state?.token == null ) {GetToken()
    } else if (state?.tokenexpiry == null) {GetToken()
    } else if (state?.tokenexpiry - latestdate.getTime() <0){
        log.debug "MS left on token: ${state.tokenexpiry - latestdate.getTime()}"
        //Need new SmartStartToken
        GetToken()
    }

    def getCarListParams = [
        uri: apiEndpoint + "/v1/devices/search/null?limit=100&deviceFilter=Installed&subAccounts=false",
        headers: ["Authorization": "Bearer ${state.token}"]]
        try{
            httpGet(getCarListParams) {
                resp ->
                    log.debug "Cars http status: ${resp.status}"
                if (resp.status == 200) {
                    def carList = resp.data
            		state.carList = carList.results.devices
                } else {
                	log.error "Unable to get cars!"
                }
            }
        } catch (e) {
            log.debug "something went wrong:  e:${e}, data:${e.response?.data}"
        }
}

def GetToken() {
    def getTokenParams = [
        uri: apiEndpoint + "/v1/auth/login",
        contentType: "application/json",
		body : ["username": Username, "password": Password]]
    try{
    httpPost(getTokenParams)  {
        resp ->
            log.debug "Token http status: ${resp.status}"
            if (resp.status == 200) {
                def token = resp.data
                state.token = token.results.authToken.accessToken
                state.tokenexpiry = token.results.authToken.expiration
                log.debug token
            } else {
                log.error "Unable to get token!"
            }
    }
    } catch (e) {
        log.debug "something went wrong: e:${e}, data:${e.response?.data}"
    }
}

//Helpful Debugger Method
def GetRandomNumber() {
     new Random().nextInt(10100)
}

//Response Method for Async HTTP Request
def processCallBack(response, data) {
    log.debug "response = ${response} data = ${data}"
    
    if (response.hasError()) {
        log.error response.getErrorData()
        log.error "RemoteStart - Response has data: ${response.errorJson?.'ResponseStatus'}"
    } else {
        def results
        try {
            results = response.json.results.device
        } catch (e) {
        	log.error "RemoteStart - Error parsing json from response: $e"
        }
        log.debug results
        if (results.find {it.toString().contains('securitySystemArmed')} as Boolean) { // Then this means its a status response
            def statusdata = [
                locked: results?.deviceStatus.securitySystemArmed,
                running : results?.deviceStatus.remoteStarterActive,
                latitude: results?.latitude,
                longitude: results?.longitude
            ]
            log.debug "RemoteStart - Locked Status $statusdata.locked"
            log.debug "RemoteStart - Start Status $statusdata.running"
            log.debug "RemoteStart - LatLong $statusdata.latitude / $statusdata.longitude"
            log.debug "RemoteStart - HEADERS: $data.deviceNetworkId"

            def devices = getChildDevices()
            devices.findAll({it.deviceNetworkId == data.deviceNetworkId}).each {
            	it.parseEventData(statusdata)
            }
            return statusdata
        } else if (results.find {it.toString().contains('action')} as Boolean) {
        	//No action needed, Device will send refresh cmd to update itself
        } else {
        	log.warn "RemoteStart - Did not get expected results from response. Body: $response.data"
        }
    }
}

def pollStatus(id) {
    //Check if token needed
    log.debug "id = ${id}"
    Date latestdate = new Date();
    if (state.tokenexpiry == null) {
        GetToken()
    } else if (state.tokenexpiry - latestdate.getTime() <0) {
        log.debug "MS left on token: ${state.tokenexpiry - latestdate.getTime()}"
        //Need new SmartStartToken
        GetToken()
    }
    def taco = id 
    def statusuri = "${apiEndpoint}/v1/devices/command"
    def pollParams = [
        //method: 'POST',
        uri: statusuri,
		contentType: "application/json",
        //requestContentType: "application/x-www-form-urlencoded; charset=utf-8",
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.token}"],
        body: ['deviceId': taco, 'command': 'read_current']
    ]
    //Data to help the resposne function know what was being asked at the time, and from who (what child device)
    def data = ["deviceNetworkId": taco, "request": "readStatus"]
    asynchttpPost(processCallBack, pollParams, data)
}

def sendCommand(cmd, id) {
    log.debug "cmd = ${cmd} id = ${id}"
    //Check if token needed
    Date latestdate = new Date();
    if (!state.tokenexpiry) {
        GetToken()
    } else if(state?.tokenexpiry - latestdate.getTime() <0) {
        log.debug "MS left on token: ${state.tokenexpiry - latestdate.getTime()}"
        //Need new SmartStartToken
        GetToken()
    }
    def taco2 = id
    def cmdString
    switch (cmd) {
        case "Lock":
        	cmdString = 'arm'
        	break
        case "Unlock":
        	cmdString = 'disarm'
        	break
        case "Start":
        	cmdString = 'remote'
        	break
        default:
        	return
    }
    def cmduri = "${apiEndpoint}/v1/devices/command"
    def params = [
        //method: 'POST',
        uri: cmduri,
		contentType: "application/json",
        //requestContentType: "application/x-www-form-urlencoded; charset=utf-8",
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.token}"],
        body: ['deviceId': taco2, 'command': cmdString]
    ]
    //Data to help the resposne function know what was being asked at the time, and from who (what child device)
    def data = ["deviceNetworkId": taco2, "request": "cmd", "cmd": cmd]
    asynchttpPost(processCallBack, params, data)
}

def appButtonHandler(btn) {
    if (btn == "Get New Token") {
        state.fetchToken = true
        GetToken()
    }
}

def setButtonName() {
    if (state?.fetchToken == true) {
        state.buttonName = "Get New Token"
    }
}

def displayFooter(){
	section() {
		paragraph "<div style='color:#1A77C9;text-align:center'>Smart Start<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=3MPZ3GU5XL8RS&item_name=Hubitat+Development&currency_code=USD' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br>Buy me a beer!</div>"
	}       
}
