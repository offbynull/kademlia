'use strict';

angular.module('ui', [])
        .controller('MainCtrl', function ($scope) {
            var movie = {
                title: 'test1',
                image: 'http://test2/test.jpg',
                description: 'test3'
            };

            $scope.movie = movie;
        });