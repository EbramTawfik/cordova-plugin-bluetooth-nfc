var exec = require('cordova/exec');

    exports.init = function(options) {
        /*
            options:
                scanCallback:
                connectCallback:
                authenticateCallback:
                pollingCallback:
                absentCallback:
                presentCallback:
                apduCallback:
                escapeCallback:
                cardCallback:
        */
        exec(
            function(obj) {
                if (obj.callback == "scan") {
                    options.scanCallback(obj);
                } else if (obj.callback == "connect") {
                    options.connectCallback(obj);
                } else if (obj.callback == "authenticate") {
                    options.authenticateCallback(obj);
                } else if (obj.callback == "polling") {
                    options.pollingCallback(obj);
                } else if (obj.callback == "absent") {
                    options.absentCallback(obj);
                } else if (obj.callback == "present") {
                    options.presentCallback(obj);
                } else if (obj.callback == "apdu") {
                    options.apduCallback(obj);
                } else if (obj.callback == "escape") {
                    options.escapeCallback(obj);
                } else if (obj.callback == "card") {
                    options.cardCallback(obj);
                } else {
                    console.log(["unknown callback", obj]);
                }

            },
            function(obj) {
                console.log(["error ", obj]);
            },
            "BluetoothNFC",
            "init", [

            ]);
    };
    exports.connect = function(options) {
        /*
            options:
                address
        */
        exec(
            function(obj) {
                console.log(obj);
            },
            function(obj) {
                console.log(obj);
            },
            "BluetoothNFC",
            "connect", [
                options.address
            ]);
    };

    exports.authenticate = function(options) {
        /*
            options:
                key
        */
        exec(
            function(obj) {
                console.log(obj);
            },
            function(obj) {
                console.log(obj);
            },
            "BluetoothNFC",
            "authenticate", [
                options.key.replace(/ /g, '')
            ]);
    };
    exports.enablePolling = function(options) {
        /*
            options
        */
        exec(
            function(obj) {
                console.log(obj);
            },
            function(obj) {
                console.log(obj);
            },
            "BluetoothNFC",
            "enablePolling", [

            ]);
    };
    exports.sendAPDU = function(options) {
        /*
            options:
                command
        */
        exec(
            function(obj) {
                console.log(obj);
            },
            function(obj) {
                console.log(obj);
            },
            "BluetoothNFC",
            "sendAPDU", [
                options.command.replace(/ /g, '')
            ]);
    };
    exports.getCardStatus = function(options) {
        /*
            options:
                command
        */
        exec(
            function(obj) {
                console.log(obj);
            },
            function(obj) {
                console.log(obj);
            },
            "BluetoothNFC",
            "getCardStatus", [

            ]);
    };


