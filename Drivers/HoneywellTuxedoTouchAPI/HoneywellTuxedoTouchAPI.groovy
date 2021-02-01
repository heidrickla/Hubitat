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
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.Cipher
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
        input 'tuxedoTouchIP', 'string', title: 'Tuxedo Touch IP', required: true, defaultValue: "192.168.1.240"
        input 'tuxedoTouchPort', 'number', title: 'Port Number', required: true, defaultValue: 80
        input 'partitionNumber', 'number', title: 'Partition Number', required: true, defaultValue: 1
        input 'userPin', 'password', title: 'User PIN', required: true, defaultValue: "0078"
        input 'publicKey', 'password', title: 'Public Key', required: true, defaultValue: "067af58e978ad9ce297d998ad8a869dc"
        input 'privateKey', 'password', title: 'Private Key', required: true, defaultValue: "c710021ef96187cda5ed6221c73fe79532c21b871893bd2edfe4c29327b636faab1c05e6713dcfdf8f522aa7881f9cef"
        input 'hubitatMac', 'password', title: 'Hubitat MAC', required: true, submitOnChange:true, defaultValue: "34E1D1806FBC"
        if (hubitatMac != null) {
            hubitatMacReplace = hubitatMac.replace(":", "-")
            device.updateSetting("hubitatMac",[value:hubitatMacReplace,type:"string"])
        }
        input name: "debugOutput", type: "bool", title: "Enable Debug Logging?", defaultValue: false
    }
}

def apiBasePath = "/system_http_api/API_REV01"
def parse(String description) {}

def calcParams(apiCommandPath, queryParamsMap) {
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def paramsStr = (queryParamsMap.collect { k,v -> "$k=$v" }).join('&')
    def paramsEnc = encrypt(new groovy.json.JsonOutput().toJson(queryParamsMap), _api_key_enc, _api_iv_enc)
    
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}?${paramsStr}",
		    requestContentType: 'application/json',
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
		body: body
        ]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
    return postParams
}

def addDeviceMac() {
//-- This Allowed to add/enroll authenticated device MAC ID for remote access. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Registration/AdddeviceMAC?MAC=<DeviceMACID>
    def apiCommandPath = "/Registration/AdddeviceMAC"
    def postParams = calcParams(apiCommandPath, [MAC: hubitatMac.toString()])

    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}?MAC=${hubitatMac}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

/*
def addDeviceMac() {
//-- This Allowed to add/enroll authenticated device MAC ID for remote access. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Registration/AdddeviceMAC?MAC=<DeviceMACID>
    def apiCommandPath = "/Registration/AdddeviceMAC"
    def apiBasePath = "/system_http_api/API_REV01"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([MAC: hubitatMac.toString()]), _api_key_enc, _api_iv_enc)
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
		    requestContentType: 'application/json',
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
		body: body
	]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
}
*/

def removeDeviceMac() {
//-- This Allowed to remove the previously added device MAC ID for remote access. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Registration/RemovedeviceMAC?MAC=<DeviceMACID>
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/Registration/RemovedeviceMAC"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([MAC: hubitatMac.toString()]), _api_key_enc, _api_iv_enc)
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
		    requestContentType: 'application/json',
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
		body: body
	]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
}

def revokeKeys() {
//-- This service is used to revoke the private and public key associated with a device mac. This service only accessible in Local Area network. This command requires Admin authorization.
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/Administration/RevokeKeys?devMAC=<MAC ID>&operation=set
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/Administration/RevokeKeys"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([devMac: hubitatMac.toString(), operation: "set"]), _api_key_enc, _api_iv_enc)
    def body = new groovy.json.JsonOutput().toJson([param: params, len: params.length(), tstamp: new Date().getTime().toString()])
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
		    requestContentType: 'application/json',
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
		    body: body
	]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
}

def getStatus() {
//Example : http://<tuxedop ip>:<port>/system_http_api/API_REV01/GetSecurityStatus?operation=get
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/GetSecurityStatus"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([operation: "get"]), _api_key_enc, _api_iv_enc)
    def body = new groovy.json.JsonOutput().toJson([param: params, len: params.length(), tstamp: new Date().getTime().toString()])
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
            requestContentType: "application/json",
            ignoreSSLIssues: true,
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
            body: body
        ]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armStay() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/AdvancedSecurity/ArmWithCode"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([arming: "STAY", pID: partitionNumber.toString(), ucode: userPin, operation: "set"]), _api_key_enc, _api_iv_enc)
    def body = new groovy.json.JsonOutput().toJson([param: params, len: params.length(), tstamp: new Date().getTime().toString()])
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
            requestContentType: 'application/json',
            ignoreSSLIssues: true,
            headers:
            [
                contentType: 'application/json',
                authtoken: authToken,
                identity: _api_iv_encStr
            ],
            body: body
        ]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armAway() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/AdvancedSecurity/ArmWithCode"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([arming: "AWAY", pID: partitionNumber.toString(), ucode: userPin, operation: "set"]), _api_key_enc, _api_iv_enc)
    def body = new groovy.json.JsonOutput().toJson([param: params, len: params.length(), tstamp: new Date().getTime().toString()])
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
		    requestContentType: "application/json",
            ignoreSSLIssues: true,
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
            body: body
        ]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def armNight() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/ArmWithCode?arming=AWAY,STAY,NIGHT&pID=1 or 2 or 3...&ucode=Valid User Code&operation=set

    def apiCommandPath = "/AdvancedSecurity/ArmWithCode"
    def postParams = calcParams(apiCommandPath, [arming: "NIGHT", pID: partitionNumber.toString(), ucode: userPin, operation: "set"])

	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def disarm() {
//Example : http://<Tuxedo IP>:<port>/system_http_api/API_REV01/AdvancedSecurity/Disarm?pID=1 or 2 or 3...&ucode=Valid User Code&operation=set
//Authentication token should be added as part of authtoken http header (Authentication token recieved during registeration operation. Not applicable for browser clients)
    def apiBasePath = "/system_http_api/API_REV01"
    def apiCommandPath = "/AdvancedSecurity/Disarm"
    def header = "MACID:" + hubitatMac + ",Path:" + apiBasePath + apiCommandPath
    
    def privateKeyBytes = hubitat.helper.HexUtils.hexStringToByteArray(privateKey)
    def _api_key_enc = subBytes(privateKeyBytes, 0, 32)
    def authToken = signString(header, _api_key_enc)
    def _api_iv_enc = subBytes(privateKeyBytes, 32, privateKeyBytes.size() - 32)
    def _api_iv_encStr = hubitat.helper.HexUtils.byteArrayToHexString(_api_iv_enc)
    
    def params = encrypt(new groovy.json.JsonOutput().toJson([pID: partitionNumber.toString(), ucode: userPin, operation: "set"]), _api_key_enc, _api_iv_enc)
    def body = new groovy.json.JsonOutput().toJson([param: params, len: params.length(), tstamp: new Date().getTime().toString()])
    def postParams = [
        uri: "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}",
		    requestContentType: "application/json",
            ignoreSSLIssues: true,
            headers:
            [
                "contentType": 'application/json',
                "authtoken": authToken,
                "identity": _api_iv_encStr,
            ],
            body: body
        ]
    
    log.debug "postParams = ${postParams}"
    params = decrypt(params, _api_key_enc, _api_iv_enc)
    log.debug "params = ${params}"
    log.debug "http://${tuxedoTouchIP}:${tuxedoTouchPort}${apiBasePath}${apiCommandPath}${params}"
	asynchttpPost('myCallbackMethod', postParams, [dataitem1: "datavalue1"])
}

def myCallbackMethod(response, data) {
    if(data["dataitem1"] == "datavalue1") {
        if (response.status == 200) {
            log.debug "data was passed successfully"
            log.debug "status of post call is: ${response.status}"
        } else if (response.status == 302) {
            // Redirected
            log.error "Redirected: ${response.status}"
        } else if (response.status == 401) {
            log.error "Unauthorized: ${response.status}"
        } else if (response.status == 405) {
            // Method Not Allowed
            log.error "Method Not Allowed: ${response.status}"
        } else if (response.status == 408) {
            // Request Timeout
            log.error "Request Timeout: ${response.status}"
        } else {
            log.error "Unknown error: ${response.status}"
        }

    }
}

private subBytes(arr, start, length)
{
    return arr.toList().subList(start, start + length) as byte[]
}

def signString(plainText, keyenc)
{
    SecretKeySpec signingKey = new SecretKeySpec(keyenc, "HmacSHA1")
    
    // get an hmac_sha1 Mac instance and initialize with the signing key
    Mac mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)
    
    // compute the hmac on input data bytes
    byte[] rawHmac = mac.doFinal(plainText.getBytes())
    return rawHmac?.encodeHex()
}

def encrypt(plainText, keyenc, ivenc)
{
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE")
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES")
    IvParameterSpec iv = new IvParameterSpec(ivenc)
    
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    def result = cipher.doFinal(plainText.getBytes("UTF-8")).encodeBase64().toString()
}

def decrypt(cypherText, keyenc, ivenc)
{
    byte[] decodedBytes = cypherText.decodeBase64()
    
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE")
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES")
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivenc))
    
    return new String(cipher.doFinal(decodedBytes), "UTF-8")
}

// Not currently in use
def getHmac() {
    String result
    String key = "${privateKey}"
    String data = "${privateKey}"

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
        return result

    } catch (Exception e) {
        log.debug("Failed to generate HMAC : " + e.getMessage())
    }
}
