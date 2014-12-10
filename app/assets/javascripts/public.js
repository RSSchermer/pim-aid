var medicationApp = angular.module('medicationApp', ['restangular']);

medicationApp.controller('medicationListCtrl', function ($scope, Restangular) {
  var drugs = Restangular.all('drugs');

  drugs.getList().then(function (drugs) {
    $scope.drugs = drugs.reverse();
  });

  $scope.submitDrug = function () {
    drugs.post({
      id: null,
      userInput: $scope.userInput,
      source: null,
      drugType: null,
      resolvedMedicationProductId: null,
      resolvedMedicationProductName: null,
      unresolvable: false
    }).then(function (drug) {
      $scope.drugs.unshift(drug);
      $scope.unresolved = false;
      $scope.userInput = null;
    }, function (error) {
      $scope.unresolved = true;
      $scope.alternatives = error.data.alternatives
    });
  };

  $scope.resolveDrug = function (drug) {
    drugs.post(drug).then(function (drug) {
      $scope.drugs.unshift(drug);
      $scope.unresolved = false;
      $scope.userInput = null;
    });
  };

  $scope.handleUnresolvableDrug = function () {
    drugs.post({
      id: null,
      userInput: $scope.userInput,
      source: null,
      drugType: null,
      resolvedMedicationProductId: null,
      resolvedMedicationProductName: null,
      unresolvable: true
    }).then(function (drug) {
      $scope.drugs.unshift(drug);
      $scope.unresolved = false;
      $scope.userInput = null;
    });
  };

  $scope.removeDrug = function (drug) {
    drug.remove();
    $scope.drugs.splice($scope.drugs.indexOf(drug), 1);
  }
});
