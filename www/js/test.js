var app = angular.module('myApp', ['ngCookies']);

app.controller('myCtrl', ['$scope', '$cookies', '$http', function ($scope, $cookies, $http) {
    $scope.init = function () {
        $scope.error_message = null;
        $scope.showLogin = $cookies['showLogin'];
        if ($scope.showLogin != true && $scope.showLogin != false) {
            $scope.showLogin = true;
        }
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('/login', {
            headers: {'Authorization': ' xBasic ' + btoa($scope.credentials.username + ":" + $scope.credentials.password)}
        })
            .success(function (data, status) {
                $cookies['showLogin'] = false;
                $scope.showLogin = $cookies['showLogin'];
                delete $scope.credentials;
                $scope.getProducts();
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.logout = function () {
        $scope.error_message = "";
        $http.get('/logout')
            .success(function (data, status) {
                $cookies['showLogin'] = true;
                $scope.showLogin = $cookies['showLogin'];
                delete $scope.products;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.cancel = function () {
        $cookies['showLogin'] = true;
        $scope.showLogin = $cookies['showLogin'];
    };

    $scope.getProducts = function () {
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
