var app = angular.module('myApp', ['ngCookies']);

app.controller('myCtrl', ['$scope', '$http', '$cookies', '$timeout', function ($scope, $http, $cookies, $timeout) {
    $scope.init = function () {
        $scope.showLogin = true;
        $scope.error_message = "";
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('https://www.bitvector.org/login', '', {
            cache: false,
            headers: {'Authorization': 'Basic ' + btoa("root:secret")}
        })
            .success(function (data, status) {
                $scope.showLogin = false;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.logout = function () {
        $scope.error_message = "";
        $http.get('https://www.bitvector.org/logout', {
            cache: false,
            headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}
        })
            .success(function (data, status) {
                delete $cookies['access_token'];
                $scope.showLogin = true;
            })
            .error(function (data, status) {
                $scope.error_message = data;

            });
    };

    $scope.getAll = function () {
        $scope.error_message = "";
        $http.get('https://www.bitvector.org:443/products', {
            cache: false,
            headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}
        })
            .success(function (data, status) {
                $scope.products = data;
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogin = true;
                }
            });
    };

}]);
