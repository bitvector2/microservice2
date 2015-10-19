var app = angular.module('myApp', []);

app.controller('myCtrl', ['$scope', '$http', '$window', function ($scope, $http, $window) {
    $scope.$watch(function () {
        if ($window.sessionStorage.getItem('showLogin') == 'true') {
            return true;
        } else if ($window.sessionStorage.getItem('showLogin') == 'false') {
            return false;
        } else {
            return null;
        }
    }, function (value) {
        $scope.showLogin = value;
    });

    $scope.watch($window.sessionStorage['showLogin'], function () {
        $scope.showLogin = $window.sessionStorage['showLogin'] == 'false' ? false : true
    });

    $scope.init = function () {
        $scope.error_message = null;
        if ($window.sessionStorage.getItem('showLogin') == null) {
            ($window.sessionStorage.getItem('showLogin') == 'true')
        }
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('/login', {
            headers: {'Authorization': ' xBasic ' + btoa($scope.credentials.username + ":" + $scope.credentials.password)}
        })
            .success(function (data, status) {
                $window.sessionStorage.setItem('showLogin', 'false');
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
                $window.sessionStorage.setItem('showLogin', 'true');
                delete $scope.products;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.cancel = function () {
        $scope.showLogin = true;
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
