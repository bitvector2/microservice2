var app = angular.module('myApp', []);

app.controller('myCtrl', ['$scope', '$rootScope', '$http', '$window', function ($scope, $rootScope, $http, $window) {

    $scope.init = function () {
        $scope.error_message = "";

        if (!$window.sessionStorage.hasOwnProperty('showLogin')) {
            $window.sessionStorage.setItem('showLogin', 'true');
        }

        $rootScope.$watch(function () {
            if ($window.sessionStorage.getItem('showLogin') == 'true') {
                return true;
            } else if ($window.sessionStorage.getItem('showLogin') == 'false') {
                return false;
            }
        }, function (value) {
            $scope.showLogin = value;
        });
    };

    $scope.login = function (username, password) {
        $scope.error_message = "";

        $http.get('/login', {
            headers: {'Authorization': ' xBasic ' + btoa(username + ":" + password)}
        })
            .success(function (data, status) {
                $window.sessionStorage.setItem('showLogin', 'false');
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
                $window.sessionStorage.setItem('showLogin', 'true');
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.addProduct = function (newname) {
        $scope.error_message = "";
        var payload = {};
        payload['name'] = newname;

        $http.post('/products', payload)
            .success(function (data, status) {
                $scope.getProducts();
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.getProducts = function () {
        $scope.error_message = "";
        $scope.products = "";

        $http.get('/products')
            .success(function (data, status) {
                $scope.products = data;
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.deleteProduct = function (product) {
        $scope.error_message = "";

        $http.delete('/products/' + product.id)
            .success(function (data, status) {
                $scope.getProducts();
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.updateProduct = function (product, rename) {
        $scope.error_message = "";
        var payload = {};
        payload['name'] = rename;

        $http.put('/products/' + product.id, payload)
            .success(function (data, status) {
                $scope.getProducts();
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

}]);
