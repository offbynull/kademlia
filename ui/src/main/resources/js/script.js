'use strict';

angular.module('ui', [])
        .controller('MainCtrl', function ($scope) {
            $scope.loginUsername = '';
            $scope.loginBootstrap = '';
            $scope.errorMessage = '';
            
            $scope.workingMessage = '';
            
            $scope.loginAction = function() {
                messageSender.loginAction($scope.loginUsername, $scope.loginBootstrap);
            };
        });
        
window.goToLogin = function(message) {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;
    
    $scope.$apply(function() {
        $scope.state = 'LOGIN';
        $scope.errorMessage = message;
    });
};

window.goToWorking = function(message) {
    var appElement = document.querySelector('[ng-app=ui]');
    var $scope = angular.element(appElement).scope();
    $scope = $scope.$$childHead;
    
    $scope.$apply(function() {
        $scope.state = 'WORKING';
        $scope.workingMessage = message;
    });
};