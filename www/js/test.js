var app = angular.module('myApp', ['ngCookies']);

app.controller('myCtrl', ['$scope', '$http', '$cookies', '$timeout', function ($scope, $http, $cookies, $timeout) {
    $scope.init = function () {
        $scope.showLogin = true;
        $scope.error_message = null;
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('/login', {
            headers: {'Authorization': ' xBasic ' + btoa($scope.credentials.username + ":" + $scope.credentials.password)}
        })
            .success(function (data, status) {
                $scope.showLogin = false;
                delete $scope.credentials;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.logout = function () {
        $scope.error_message = "";
        $http.get('/logout')
            .success(function (data, status) {
                $scope.showLogin = true;
                delete $scope.products;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.cancel = function () {
        $scope.showLogin = true;
    };

    $scope.getAll = function () {
        $scope.error_message = "";
        $http.get('/products')
            .success(function (data, status) {
                $scope.products = data;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };
}]);
