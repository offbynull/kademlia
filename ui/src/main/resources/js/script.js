'use strict';

angular.module('ui', [])
        .controller('MainCtrl', function ($scope) {
            $scope.loginUsername = '';
            $scope.loginBootstrap = '';
            $scope.errorMessage = '';

            $scope.workingMessage = '';

            $scope.inMessageRate = 0;
            $scope.outMessageRate = 0;

            $scope.loginAction = function () {
                messageSender.loginAction($scope.loginUsername, $scope.loginBootstrap);
            };

            $scope.logoutAction = function () {
                messageSender.logoutAction();
            };

            $scope.chooseDevicesAction = function () {
                messageSender.chooseDevicesAction();
            };
        });

window.goToLogin = function (message, reset) {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;

    $scope.$apply(function () {
        $scope.state = 'LOGIN';
        $scope.errorMessage = message;

        if (reset === true) {
            $scope.loginUsername = '';
            $scope.loginBootstrap = '';
        }
    });
};

window.goToWorking = function (message) {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;

    $scope.$apply(function () {
        $scope.state = 'WORKING';
        $scope.workingMessage = message;
    });
};

window.goToIdle = function () {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;

    $scope.$apply(function () {
        $scope.state = 'ACTIVE_IDLE';
        $scope.workingMessage = message;
    });
};

window.showDeviceSelection = function (inputDevices, outputDevices) {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;

    $scope.$apply(function () {
        $scope.inputDevices = inputDevices;
        $scope.outputDevices = outputDevices;
        $('#audio-config-dialog').modal({
            backdrop: 'static',
            keyboard: false
        });
    });
};