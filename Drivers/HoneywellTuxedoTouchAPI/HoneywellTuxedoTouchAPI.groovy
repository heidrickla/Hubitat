/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Honeywell Tuxedo Touch API
 *
 *  Author: Lewis Heidrick
 *
 *  Date: 2021-01-31
 */
import groovy.transform.Field
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

metadata {
	definition (name: "Honeywell Tuxedo Touch API", namespace: "heidrickla", author: "Lewis Heidrick") {
		capability "Momentary"
        
        command "armStay"
        command "armAway"
        command "armNight"
        command "disarm"
        command "getStatus"
        command "addDeviceMac"
        command "removeDeviceMac"
        command "revokeKeys"
	}
    preferences {
        input 'tuxedoTouchIP', 'string', title: 'Tuxedo Touch IP', required: true, defaultValue: "192.168.1.1"
        input 'tuxedoTouchPort', 'number', title: 'Port Number', required: true, defaultValue: 80
        input 'partitionNumber', 'number', title: 'Partition Number', required: true, defaultValue: 1
    
        input 'userPin', 'password', title: 'User PIN', required: true, defaultValue: "0000"
        input 'publicKey', 'password', title: 'Public Key', required: true, defaultValue: "ab1c05e6713dcfdf8f522aa7881f9lew"
        input 'privateKey', 'password', title: 'Private Key', required: true, defaultValue: "c710021ef96187cda5ed6221c73fe79532c21b871893bd2edfe4c29327b636faab1c05e6713dcfdf8f522aa7881f9sdf"
        input 'hubitatMac', 'password', title: 'Hubitat MAC', required: true, submitOnChange:true, defaultValue: "34E1D1806BGC"
        if (hubitatMac != null) {
            hubitatMacReplace = hubitatMac.replace(":", "-")
            device.updateSetting("hubitatMac",[value:hubitatMacReplace,type:"string"])
        }
        input name: "debugOutput", type: "bool", title: "Enable Debug Logging?", defaultValue: false
    }
}

def apiRev = "API_REV01"
def apiBasePath = "/system_http_api/" + API_REV
def parse(String description) {}

def addDeviceMac() {
//-- This Allowed to add/enroll authenticated device MAC ID for remote access. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Registration/AdddeviceMAC?MAC=<DeviceMACID>
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/" + apiRev
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Registration/AdddeviceMAC?MAC=${hubitatMac}",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Registration/AdddeviceMAC?MAC=${hubitatMac}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def removeDeviceMac() {
//-- This Allowed to remove the previously added device MAC ID for remote access. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Registration/RemovedeviceMAC?MAC=<DeviceMACID>
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/" + apiRev
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Registration/RemovedeviceMAC?MAC=${hubitatMac}",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Registration/RemovedeviceMAC?MAC=${hubitatMac}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def revokeKeys() {
//-- This service is used to revoke the private and public key associated with a device mac. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Administration/RevokeKeys?devMAC=<MAC ID>&operation=set
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/" + apiRev
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Administration/RevokeKeys?devMAC=${hubitatMac}&operation=set",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/Administration/RevokeKeys?devMAC=${hubitatMac}&operation=set"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def getStatus() {
//Example : http://<tuxedop ip>:<port>/system_http_api/API_REV01/GetSecurityStatus?operation=get
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/" + apiRev
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/GetSecurityStatus?operation=get",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/GetSecurityStatus?operation=get"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armStay() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/${apiRev}"
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=STAY@pID=${partitionNumber}&ucode=${userPin}&operation=set",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=STAY@pID=${partitionNumber}&ucode=${userPin}&operation=set"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armAway() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/${apiRev}"
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=AWAY@pID=${partitionNumber}&ucode=${userPin}&operation=set",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=AWAY@pID=${partitionNumber}&ucode=${userPin}&operation=set"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armNight() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/${apiRev}"
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=NIGHT@pID=${partitionNumber}&ucode=${userPin}&operation=set",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=NIGHT@pID=${partitionNumber}&ucode=${userPin}&operation=set"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def disarm() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/Disarm?pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    getHmac(privateKey)
    def apiRev = "API_REV01"
    def apiBasePath = "/system_http_api/${apiRev}"
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/Disarm?pID=${partitionNumber}&ucode=${userPin}&operation=set",
		requestContentType: 'application/json',
		contentType: 'application/json',
        headers: ['authtoken':"${authorizationToken}",'MACID':"${hubitatMac}"]//,
		//body : ["name": "value"]
	]
    log.debug postParams
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}/AdvancedSecurity/ArmWithCode?arming=NIGHT@pID=${partitionNumber}&ucode=${userPin}&operation=set"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def myCallbackMethod(response, data) {
    if(data["dataitem1"] == "datavalue1") {
        log.debug "data was passed successfully"
        log.debug "status of post call is: ${response.status}"
    }
}

def getHmac(authenticationToken) {
    String result
    String key = "${authenticationToken}"
    String data = "${authenticationToken}"

    try {

        // get an hmac_sha1 key from the raw key bytes
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");

        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);

        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(data.getBytes());
        result= rawHmac.encodeHex()
		log.debug "HMAC_SHA1 result: ${result}"

    } catch (Exception e) {
        log.debug("Failed to generate HMAC : " + e.getMessage())
    }
}
