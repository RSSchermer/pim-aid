var medicationApp = angular.module('medicationApp', ['restangular']);

medicationApp.controller('medicationListCtrl', function ($scope, Restangular) {
  var drugs = Restangular.all('drugs');

  drugs.getList().then(function (drugs) {
    $scope.drugs = drugs;
  });

  $scope.submitDrug = function () {
    drugs.post({ id: null, userInput: $scope.userInput, source: null, drugType: null }).then(function (drug) {
      $scope.drugs.push(drug);
      $scope.error = null;
      $scope.userInput = null;
    }, function () {
      $scope.error = "Dit medicijn kon niet gevonden worden.";
    })
  };

  $scope.removeDrug = function (drug) {
    drug.remove();
    $scope.drugs.splice($scope.drugs.indexOf(drug), 1);
  }
});
