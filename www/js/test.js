var app = angular.module('myApp', ['ngCookies']);

app.controller('myCtrl', ['$scope', '$http', '$cookies', '$timeout', function ($scope, $http, $cookies, $timeout) {
    $scope.init = function () {
        $scope.showLogIn = false;
        $scope.token = "";
        $scope.error_message = "";

        $scope.getAll();
    };

    $scope.getAll = function () {
        $scope.error_message = "";
        $http.get('https://www.bitvector.org:443/products', {headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}})
            .success(function (data, status) {
                $scope.products = data;
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogIn = true;
                }
            });
    };

    $scope.create = function () {
        $scope.error_message = "";
        if ($cookies.hasOwnProperty('access_token')) {
            $scope.token = $cookies['access_token']
        }
        var max_id = 0;
        angular.forEach($scope.products, function (template) {
            if (template.TemplateId > max_id) {
                max_id = template.TemplateId;
            }
        });
        $http.post('https://www.bitvector.org:443/products', {"Name": "Template" + (max_id + 1)}, {headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}})
            .success(function (data, status) {
                $scope.getAll();
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogIn = true;
                }
            });

    };

    $scope.update = function (template) {
        $scope.error_message = "";
        var basic_auth = "";
        if ($cookies.hasOwnProperty('access_token')) {
            $scope.token = $cookies['access_token'];
            basic_auth = 'Basic ' + btoa($scope.token);
        }
        $http.put('https://www.bitvector.org:443/products/' + template['TemplateId'], {"Name": template['Name']}, {headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}})
            .success(function (data, status) {
                $scope.getAll();
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogIn = true;
                }
            });
    };

    $scope.delete = function (template) {
        $scope.error_message = "";
        var basic_auth = "";
        if ($cookies.hasOwnProperty('access_token')) {
            $scope.token = $cookies['access_token'];
            basic_auth = 'Basic ' + btoa($scope.token);
        }
        $http.delete('https://www.bitvector.org:443/products/' + template['TemplateId'], {headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}})
            .success(function (data, status) {
                $scope.getAll();
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogIn = true;
                }
            });
    };

    $scope.login = function () {
        $scope.error_message = "";
        $http.get('https://www.bitvector.org:443/login', '', {
            cache: false,
            headers: {'Authorization': 'Basic ' + btoa($scope.credentials.username + ":" + $scope.credentials.password)}
        })
            .success(function (data, status) {
                $scope.showLogIn = false;
                $timeout($scope.getAll, 10)
            })
            .error(function (data, status) {
                $scope.error_message = data;
            });
    };

    $scope.logout = function () {
        $scope.showLogIn = true;
        delete $cookies['access_token'];

        $scope.error_message = "";
        $http.get('https://www.bitvector.org:443/products', {headers: {'Cookie': $cookies['access_token'] == null ? "" : $cookies['access_token']}})
            .success(function (data, status) {
                $scope.products = data;
            })
            .error(function (data, status) {
                $scope.error_message = data;
                if (status == 403 || status == 401) {
                    $scope.showLogIn = true;
                }
            });
    };

}]);
