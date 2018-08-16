/********* BluetoothNFC.m Cordova Plugin Implementation *******/

#import "BluetoothNFC.h"

@interface BluetoothNFC : CDVPlugin <CBCentralManagerDelegate, ABTBluetoothReaderManagerDelegate, ABTBluetoothReaderDelegate>{
  // Member variables go here.
    
    CBCentralManager *_centralManager;
    CBPeripheral *_peripheral;
    NSMutableArray *_peripherals;
    
    ABTBluetoothReaderManager *_bluetoothReaderManager;
    ABTBluetoothReader *_bluetoothReader;
    
    NSString *callbackId;
}

- (void)init:(CDVInvokedUrlCommand*)command;
- (void)connect:(CDVInvokedUrlCommand*)command;
- (void)authenticate:(CDVInvokedUrlCommand*)command;
- (void)enablePolling:(CDVInvokedUrlCommand*)command;
- (void)getCardStatus:(CDVInvokedUrlCommand*)command;
- (void)sendAPDU:(CDVInvokedUrlCommand*)command;
@end

@implementation BluetoothNFC

- (void)pluginInitialize
{
    _centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    _peripherals = [NSMutableArray array];
    
    _bluetoothReaderManager = [[ABTBluetoothReaderManager alloc] init];
    _bluetoothReaderManager.delegate = self;
    
    _bluetoothReader = [[ABTBluetoothReader alloc] init];
    
    callbackId = @"";
    
    [self centralManagerDidUpdateState:_centralManager];
    
}
- (void)init:(CDVInvokedUrlCommand*)command
{
    callbackId = command.callbackId;
    NSLog(@">> init %@ - %@", command.arguments, command.callbackId);
    
    //    [self startScanBLE];
    [NSTimer scheduledTimerWithTimeInterval:0.01f target:self selector:@selector(startScanBLE) userInfo:nil repeats:NO];
    
    
    return;
    //    CDVPluginResult* pluginResult = nil;
    //    NSString* echo = [command.arguments objectAtIndex:0];
    //
    //    if (echo != nil && [echo length] > 0) {
    //        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    //    } else {
    //        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    //    }
    //
    //    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)connect:(CDVInvokedUrlCommand*)command
{
    //    callbackId = command.callbackId;
    NSLog(@">> connect %@ - %@", command.arguments, command.callbackId);
    
    for(CBPeripheral *device in _peripherals)
    {
        if([[device identifier] UUIDString] != nil && [[[device identifier] UUIDString] isEqualToString:[command.arguments firstObject]])
        {
            NSLog(@">> now connect %@", device);
            _peripheral = device;
            [_centralManager connectPeripheral:_peripheral options:nil];
        }
    }
}

- (void)authenticate:(CDVInvokedUrlCommand*)command
{
    //    callbackId = command.callbackId;
    NSLog(@">> authenticate %@ - %@", command.arguments, command.callbackId);
    
    [_bluetoothReader authenticateWithMasterKey:[ABDHex byteArrayFromHexString:[command.arguments firstObject]]];
}
- (void)enablePolling:(CDVInvokedUrlCommand*)command
{
    //    callbackId = command.callbackId;
    NSLog(@">> enablePolling %@ - %@", command.arguments, command.callbackId);
    
    uint8_t cmd[] = { 0xE0, 0x00, 0x00, 0x40, 0x01 };
    
    [_bluetoothReader transmitEscapeCommand:cmd length:sizeof(cmd)];
}
- (void)getCardStatus:(CDVInvokedUrlCommand*)command
{
    //    callbackId = command.callbackId;
    NSLog(@">> getCardStatus %@ - %@", command.arguments, command.callbackId);
    
    
    [_bluetoothReader getCardStatus];
}
- (void)sendAPDU:(CDVInvokedUrlCommand*)command
{
    //    callbackId = command.callbackId;
    NSLog(@">> sendAPDU %@ - %@", command.arguments, command.callbackId);
    
    [_bluetoothReader transmitApdu:[ABDHex byteArrayFromHexString:[command.arguments firstObject]]];
}

- (void)startScanBLE {
    NSLog(@"start scan");
    [_peripherals removeAllObjects];
    if(_peripheral != nil)
    {
        [_centralManager cancelPeripheralConnection:_peripheral];
    }
    
    [_centralManager scanForPeripheralsWithServices:nil options:nil];
    
    
    [NSTimer scheduledTimerWithTimeInterval:3.0f target:self selector:@selector(stopScanBLE) userInfo:nil repeats:NO];
}
- (void)stopScanBLE {
    NSLog(@"stop scan");
    [_centralManager stopScan];
    
    NSMutableDictionary *rs = [NSMutableDictionary dictionary];
    [rs setObject:@"scan" forKey:@"callback"];
    
    NSMutableArray *devices = [NSMutableArray array];
    for(CBPeripheral *device in _peripherals)
    {
        if([device name] != nil)
        {
            
            NSMutableDictionary *d = [NSMutableDictionary dictionary];
            
            [d setObject:[[device identifier] UUIDString] forKey:@"id"];
            [d setObject:[device name] forKey:@"name"];
            [d setObject:[[device identifier] UUIDString] forKey:@"address"];
            
            [devices addObject:d];
        }
    }
    
    [rs setObject:devices forKey:@"devices"];
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    
    NSString *stateString = nil;
    switch(central.state)
    {
        case CBCentralManagerStateResetting:
            stateString = @"The connection with the system service was momentarily lost, update imminent.";
            break;
            
        case CBCentralManagerStateUnsupported:
            stateString = @"The platform doesn't support Bluetooth Low Energy.";
            break;
            
        case CBCentralManagerStateUnauthorized:
            stateString = @"The app is not authorized to use Bluetooth Low Energy.";
            break;
        case CBCentralManagerStatePoweredOff:
            stateString = @"Bluetooth is currently powered off.";
            break;
        case CBCentralManagerStatePoweredOn:
            stateString = @"Bluetooth is currently powered on and available to use.";
            break;
        default:
            stateString = @"State unknown, update imminent.";
            break;
    }
    
    
    NSLog(@"Bluetooth State %@",stateString);
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI {
    
    // If the peripheral is not found, then add it to the array.
    if ([_peripherals indexOfObject:peripheral] == NSNotFound) {
        NSLog(@"Found %@", peripheral);
        // Add the peripheral to the array.
        [_peripherals addObject:peripheral];
        
    }
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    // Detect the Bluetooth reader.
    NSLog(@"Connected %@", peripheral);
    [_bluetoothReaderManager detectReaderWithPeripheral:peripheral];
}

- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"didFailToConnectPeripheral %@", error);
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"didDisconnectPeripheral %@", error);
}



- (void)bluetoothReaderManager:(ABTBluetoothReaderManager *)bluetoothReaderManager didDetectReader:(ABTBluetoothReader *)reader peripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"didDetectReader %@", reader);
    
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        
        // Store the Bluetooth reader.
        _bluetoothReader = reader;
        _bluetoothReader.delegate = self;
        
        // Attach the peripheral to the Bluetooth reader.
        [_bluetoothReader attachPeripheral:peripheral];
    }
}

#pragma mark - Bluetooth Reader

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didAttachPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"didAttachPeripheral");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
    }else{
        
        NSMutableDictionary *rs = [NSMutableDictionary dictionary];
        [rs setObject:@"connect" forKey:@"callback"];
        [rs setObject:@"Acr1255uj1Reader" forKey:@"reader"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }
//    else {
//        
//        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Information" message:@"The reader is attached to the peripheral successfully." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles: nil];
//        [alert show];
//    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didReturnDeviceInfo:(NSObject *)deviceInfo type:(ABTBluetoothReaderDeviceInfo)type error:(NSError *)error {
    
    NSLog(@"didReturnDeviceInfo");
    // Show the error
    if (error != nil) {
        
        [self ABD_showError:error];
        
    } else {
        
//        if (_deviceInfoViewController != nil) {
//            
//            switch (type) {
//                    
//                case ABTBluetoothReaderDeviceInfoSystemId:
//                    // Show the system ID.
//                    _deviceInfoViewController.systemIdLabel.text = [ABDHex hexStringFromByteArray:(NSData *)deviceInfo];
//                    break;
//                    
//                case ABTBluetoothReaderDeviceInfoModelNumberString:
//                    // Show the model number.
//                    _deviceInfoViewController.modelNumberLabel.text = (NSString *) deviceInfo;
//                    break;
//                    
//                case ABTBluetoothReaderDeviceInfoSerialNumberString:
//                    // Show the serial number.
//                    _deviceInfoViewController.serialNumberLabel.text = (NSString *) deviceInfo;
//                    break;
//                    
//                case ABTBluetoothReaderDeviceInfoFirmwareRevisionString:
//                    // Show the firmware revision.
//                    _deviceInfoViewController.firmwareRevisionLabel.text = (NSString *) deviceInfo;
//                    break;
//                    
//                case ABTBluetoothReaderDeviceInfoHardwareRevisionString:
//                    // Show the hardware revision.
//                    _deviceInfoViewController.hardwareRevisionLabel.text = (NSString *) deviceInfo;
//                    break;
//                    
//                case ABTBluetoothReaderDeviceInfoManufacturerNameString:
//                    // Show the manufacturer name.
//                    _deviceInfoViewController.manufacturerNameLabel.text = (NSString *) deviceInfo;
//                    break;
//                    
//                default:
//                    break;
//            }
//            
//            [_deviceInfoViewController.tableView reloadData];
//        }
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didAuthenticateWithError:(NSError *)error {
    
    NSLog(@"didAuthenticateWithError");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    }else{
        NSMutableDictionary *rs = [NSMutableDictionary dictionary];
        [rs setObject:@"authenticate" forKey:@"callback"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }
//    else {
//        
//        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Information" message:@"The reader is authenticated successfully." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles: nil];
//        [alert show];
//    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didReturnAtr:(NSData *)atr error:(NSError *)error {
    NSLog(@"didReturnAtr");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        
        // Show the ATR string.
//        self.atrLabel.text = [ABDHex hexStringFromByteArray:atr];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didPowerOffCardWithError:(NSError *)error {
    NSLog(@"didPowerOffCardWithError");
    // Show the error
    if (error != nil) {
        [self ABD_showError:error];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didReturnCardStatus:(ABTBluetoothReaderCardStatus)cardStatus error:(NSError *)error {
    NSLog(@"didReturnCardStatus");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        NSMutableDictionary *rs = [NSMutableDictionary dictionary];
        [rs setObject:@"card" forKey:@"callback"];
        [rs setObject:[self ABD_stringFromCardStatus:cardStatus] forKey:@"result"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        
        // Show the card status.
//        self.cardStatusLabel.text = [self ABD_stringFromCardStatus:cardStatus];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didReturnResponseApdu:(NSData *)apdu error:(NSError *)error {
    NSLog(@"didReturnResponseApdu");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        NSMutableDictionary *rs = [NSMutableDictionary dictionary];
        [rs setObject:@"apdu" forKey:@"callback"];
        [rs setObject:[ABDHex hexStringFromByteArray:apdu] forKey:@"result"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        // Show the response APDU.
//        self.responseApduLabel.text = [ABDHex hexStringFromByteArray:apdu];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didReturnEscapeResponse:(NSData *)response error:(NSError *)error {
    NSLog(@"didReturnEscapeResponse");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        
        // Show the escape response.
//        self.escapeResponseLabel.text = [ABDHex hexStringFromByteArray:response];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didChangeCardStatus:(ABTBluetoothReaderCardStatus)cardStatus error:(NSError *)error {
    NSLog(@"didChangeCardStatus");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        if(cardStatus == ABTBluetoothReaderCardStatusAbsent)
        {
            NSMutableDictionary *rs = [NSMutableDictionary dictionary];
            [rs setObject:@"absent" forKey:@"callback"];
            
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        }else if(cardStatus == ABTBluetoothReaderCardStatusPresent)
        {
            NSMutableDictionary *rs = [NSMutableDictionary dictionary];
            [rs setObject:@"present" forKey:@"callback"];
            
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:rs];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        }
        // Show the card status.
//        self.cardStatusLabel.text = [self ABD_stringFromCardStatus:cardStatus];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didChangeBatteryStatus:(ABTBluetoothReaderBatteryStatus)batteryStatus error:(NSError *)error {
    NSLog(@"didChangeBatteryStatus");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        
        // Show the battery status.
//        self.batteryStatusLabel.text = [self ABD_stringFromBatteryStatus:batteryStatus];
//        [self.tableView reloadData];
    }
}

- (void)bluetoothReader:(ABTBluetoothReader *)bluetoothReader didChangeBatteryLevel:(NSUInteger)batteryLevel error:(NSError *)error {
    NSLog(@"didChangeBatteryLevel");
    if (error != nil) {
        
        // Show the error
        [self ABD_showError:error];
        
    } else {
        
        // Show the battery level.
//        self.batteryLevelLabel.text = [NSString stringWithFormat:@"%lu%%", (unsigned long) batteryLevel];
//        [self.tableView reloadData];
    }
}

#pragma mark - Private Methods

/**
 * Returns the description from the card status.
 * @param cardStatus the card status.
 * @return the description.
 */
- (NSString *)ABD_stringFromCardStatus:(ABTBluetoothReaderCardStatus)cardStatus {
    
    NSString *string = nil;
    
    switch (cardStatus) {
            
        case ABTBluetoothReaderCardStatusUnknown:
            string = @"Unknown";
            break;
            
        case ABTBluetoothReaderCardStatusAbsent:
            string = @"Absent";
            break;
            
        case ABTBluetoothReaderCardStatusPresent:
            string = @"Present";
            break;
            
        case ABTBluetoothReaderCardStatusPowered:
            string = @"Powered";
            break;
            
        case ABTBluetoothReaderCardStatusPowerSavingMode:
            string = @"Power Saving Mode";
            break;
            
        default:
            string = @"Unknown";
            break;
    }
    
    return string;
}

/**
 * Returns the description from the battery status.
 * @param batteryStatus the battery status.
 * @return the description.
 */
- (NSString *)ABD_stringFromBatteryStatus:(ABTBluetoothReaderBatteryStatus)batteryStatus {
    
    NSString *string = nil;
    
    switch (batteryStatus) {
            
        case ABTBluetoothReaderBatteryStatusNone:
            string = @"No Battery";
            break;
            
        case ABTBluetoothReaderBatteryStatusFull:
            string = @"Full";
            break;
            
        case ABTBluetoothReaderBatteryStatusUsbPlugged:
            string = @"USB Plugged";
            break;
            
        default:
            string = @"Low";
            break;
    }
    
    return string;
}

/**
 * Shows the error.
 * @param error the error.
 */
- (void)ABD_showError:(NSError *)error {
    NSLog(@"ABD_showError %@", error);
//    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:[NSString stringWithFormat:@"Error %ld", (long)[error code]] message:[error localizedDescription] delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
//    [alert show];
}


@end
