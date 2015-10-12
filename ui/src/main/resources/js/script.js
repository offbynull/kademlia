'use strict';

angular.module('ui', [])
        .controller('MainCtrl', function ($scope) {
            $scope.loginUsername = '';
            $scope.loginBootstrap = '';
            $scope.errorMessage = '';

            $scope.workingMessage = '';

            $scope.inMessageRate = 0;
            $scope.outMessageRate = 0;

            $scope.blockInput = false;

            //
            // METHODS INVOKED BY THE UI (FROM THE HTML VIA ANGULARJS) THAT GET FORWARDED TO JAVA
            //
            $scope.loginAction = function () {
                messageSender.loginAction($scope.loginUsername, $scope.loginBootstrap);
                $scope.blockInput = true;
            };

            $scope.logoutAction = function () {
                messageSender.logoutAction();
                $scope.blockInput = true;
            };

            $scope.resetDevicesAction = function () {
                messageSender.resetDevicesAction();
                $scope.blockInput = true;
            };

            $scope.chooseDevicesAction = function () {
                messageSender.chooseDevicesAction($scope.selectedInputDevice, $scope.selectedOutputDevice);
                // asynch operation, do not block input
                // $scope.blockInput = true;
            };

            $scope.devicesChosenAction = function () {
                messageSender.devicesChosenAction();
                $scope.blockInput = true;
            };

            $scope.callAction = function () {
                messageSender.callAction($scope.callUsername);
                $scope.blockInput = true;
            };

            $scope.acceptIncomingCallAction = function () {
                messageSender.acceptIncomingCallAction();
                $scope.blockInput = true;
            };

            $scope.rejectIncomingCallAction = function () {
                messageSender.rejectIncomingCallAction();
                $scope.blockInput = true;
            };

            $scope.hangupCallAction = function () {
                messageSender.hangupCallAction();
                $scope.blockInput = true;
            };

            $scope.errorAcknowledgedAction = function () {
                messageSender.errorAcknowledgedAction();
                $scope.blockInput = true;
            };
        });



//
// CALLBACKS INVOKED FROM JAVA TO FORCE THE UI TO GO TO A DIFFERENT STATE
//
window.goToLogin = function (reset) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'LOGIN';

        if (reset === true) {
            $scope.loginUsername = '';
            $scope.loginBootstrap = '';
        }
    });
};

window.goToError = function (message, critical) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'ERROR';
        $scope.errorMessage = message;
        $scope.errorCritical = critical;
    });
};

window.goToWorking = function (message) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'WORKING';
        $scope.workingMessage = message;
    });
};

window.goToIdle = function () {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'ACTIVE_IDLE';
    });
};

window.goToOutgoingCall = function (username) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'ACTIVE_OUTGOING_CALL';
        $scope.callUsername = username;
    });
};

window.goToIncomingCall = function (username) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'ACTIVE_INCOMING_CALL';
        $scope.callUsername = username;
    });
};

window.goToEstablishedCall = function () {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.blockInput = false;
        $scope.state = 'ACTIVE_CALL';
    });
};

window.showDeviceSelection = function (inputDevices, outputDevices) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.inputDevices = inputDevices;
        $scope.outputDevices = outputDevices;

        $scope.selectedInputDevice = getFirstKey(inputDevices);
        $scope.selectedOutputDevice = getFirstKey(outputDevices);

        if ($scope.selectedInputDevice === undefined || $scope.selectedOutputDevice === undefined) {
            console.error("UNDEFINED DEVICE (" + $scope.selectedInputDevice + ") / (" + $scope.selectedOutputDevice + ")");
        }

        $scope.chooseDevicesAction(); // Let Java know that the default values are the ones currently selected

        $scope.blockInput = false;
        $scope.state = 'DEVICE_SELECTION';
    });
};

window.updateMessageRate = function (incomingMessageRate, outgoingMessageRate) {
    var $scope = getAngularScope();

    $scope.$apply(function () {
        $scope.inMessageRate = incomingMessageRate;
        $scope.outMessageRate = outgoingMessageRate;
    });
};


//
// UTILITY METHODS
//
window.getAngularScope = function () {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    return $scope.$$childHead;
};

// http://stackoverflow.com/questions/909003/javascript-getting-the-first-index-of-an-object
window.getFirstKey = function (obj) {
    for (var i in obj) {
        if (obj.hasOwnProperty(i) && typeof (i) !== 'function') {
            return i;
        }
    }

    return undefined;
};

window.printObjectToConsole = function (obj) {
    for (var key in Object.keys(obj)) {
        console.warn(key);
        console.warn(obj[key]);
    }
};
