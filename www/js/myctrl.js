'use strict';

var MyCtrl = angular.module('MyCtrl', []);

function MyFunc($scope, $http) {
      $scope.init = function() {
           $scope.getProduct()
      };

      $scope.getProduct = function() {
          $http.get('/products/1')
              .success(function (data) {
                   $scope.product = data['name']
               })
              .error(function () {
                   $scope.product = "ERROR"
               })
     }
}

MyFunc.$inject = ['$scope', '$http'];

MyCtrl.controller('MyCtrl', MyFunc);
