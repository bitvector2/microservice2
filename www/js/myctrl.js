'use strict'

var MyCtrl = angular.module('MyCtrl', []);

function MyFunc($scope, $http) {
      $scope.init = function() {
           $scope.getProduct()
      }

      $scope.getProduct = function() {
           $http.get('https://www.bitvector.org/products/99')
               .success(function(data, status, headers, config) {
                   $scope.product = data['name']
               })
               .error(function(data, status, header, config) {
                   $scope.product = "ERROR"
               })
     }
}

MyFunc.$inject = ['$scope', '$http']

MyCtrl.controller('MyCtrl', MyFunc)

