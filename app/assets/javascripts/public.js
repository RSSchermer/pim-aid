var medicationApp = angular.module('medicationApp', ['restangular']);

medicationApp.controller('medicationListCtrl', function ($scope, Restangular) {
  var drugs = Restangular.all('drugs');

  drugs.getList().then(function (drugs) {
    $scope.drugs = drugs;
  });

  $scope.submitDrug = function () {
    drugs.post({
      id: null,
      userInput: $scope.userInput,
      source: null,
      drugType: null,
      resolvedMedicationProductId: null,
      resolvedMedicationProductName: null
    }).then(function (drug) {
      $scope.drugs.push(drug);
      $scope.unresolved = false;
      $scope.userInput = null;
    }, function (error) {
      console.log(error);
      $scope.unresolved = true;
      $scope.alternatives = error.data.alternatives
    })
  };

  $scope.resolveDrug = function (drug) {
    drugs.post(drug).then(function (drug) {
      $scope.drugs.push(drug);
      $scope.unresolved = false;
      $scope.userInput = null;
    });
  };

  $scope.removeDrug = function (drug) {
    drug.remove();
    $scope.drugs.splice($scope.drugs.indexOf(drug), 1);
  }
});
