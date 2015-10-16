var app = angular.module('myApp', []);

app.controller('myCtrl', ['$scope', '$rootScope', '$http', function ($scope, $rootScope, $http) {
    $scope.init = function () {
        $rootScope.showLogin = true;
        $scope.error_message = null;
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('/login', {
            headers: {'Authorization': ' xBasic ' + btoa($scope.credentials.username + ":" + $scope.credentials.password)}
        })
            .success(function (data, status) {
                $rootScope.showLogin = false;
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
                $rootScope.showLogin = true;
                delete $scope.products;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.cancel = function () {
        $rootScope.showLogin = true;
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
