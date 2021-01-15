metadata {
    definition(name: "NorthQ Q-Power", namespace: "heidrickla", author: "Lewis Heidrick") {
        capability "Energy Meter"
        capability "Power Meter"
        capability "Configuration"
        capability "Sensor"
        capability "Battery"

        fingerprint mfr: "0096", prod: "0001", model: "0001"
    }

    // simulator metadata
    simulator {
        for (int i = 0; i <= 100; i += 10) {
            status "energy  ${i} kWh": new hubitat.zwave.Zwave().meterV1.meterReport(
                    scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
        }
    }

    preferences {
        input name: "pulsesPerKwh", type: "number", title: "Pulses/kWh", description: "The number of pulses pr. kWh on your meter", required: true, defaultValue: 1000
        input name: "wakeUpSeconds", type: "number", title: "Seconds between reports", description: "How many seconds before reporting back. WARNING: Lowering this value will impact battery life.", required: true, defaultValue: 900
        input name: "baseKwh", type: "number", title: "Meter start value", description: "The number on your meter before you add this device.", required: false, defaultValue: 0
    }
}

def parse(String description) {
    log.debug("Event received parsing: '${description}'")
    def result = null
    if (description == "updated") return
    def cmd = zwave.parse(description, [0x20: 1, 0x32: 1, 0x72: 2])
    if (cmd) {
        log.debug "$device.displayName: Command received: $cmd"
        result = zwaveEvent(cmd)
    }
    return result
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    def allCommands = [
            zwave.meterV1.meterGet().format(),
            zwave.batteryV1.batteryGet().format(),
            zwave.wakeUpV1.wakeUpNoMoreInformation().format()
    ]
    if (state.configurationCommands) {
        allCommands = (state.configurationCommands + allCommands)
    }

    state.configurationCommands = null

    log.debug("Sent ${allCommands.size} commands in response to wake up")

    return [
            createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false),
            response(allCommands)
    ]
}

def zwaveEvent(hubitat.zwave.commands.meterv1.MeterReport cmd) {
    def events = []
    def commandTime = new Date()
    if (cmd.scale == 0) {
        Double newValue = cmd.scaledMeterValue + baseKwh
        events << createEvent(name: "energy", value: newValue, unit: "kWh")

        if (state.previousValue) {
            def diffTime = commandTime.getTime() - state.previousValueDate
            Double diffValue = newValue - state.previousValue

            Double diffHours = diffTime / 1000.0 / 60.0 / 60.0
            Double watt = 1000.0 * diffValue / diffHours

            events << createEvent(name: "power", value: Math.round(watt), unit: "W")
        }
        state.previousValue = newValue
        state.previousValueDate = commandTime.getTime()
    } else {
        log.error("Received meter report with scale ${cmd.scale} , don't know how to interpret that")
    }
    return events
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    return createEvent(name: "battery", value: cmd.batteryLevel, unit: "percent")
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    log.debug("Configuration changed. Parameter number: ${cmd.parameterNumber}")
    return []
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "$device.displayName: Unhandled: $cmd"
    return []
}

def updated() {
    return configure()
}

def configure() {
    log.debug("Preparing configuration. It will be sent next time the device wakes up and checks in...")

    state.configurationCommands = [
            zwave.configurationV1.configurationSet(parameterNumber: 1, size: 4, scaledConfigurationValue: pulsesPerKwh.toInteger() * 10).format(),    // The number of blinks pr. kwh
            zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1).format(),                                // The type of meter, mechanical/electric pulse
            zwave.wakeUpV1.wakeUpIntervalSet(seconds: wakeUpSeconds, nodeid: zwaveHubNodeId).format()                                                 // Set the interval between wake ups
    ]

    return []
}
