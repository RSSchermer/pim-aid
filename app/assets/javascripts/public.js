var medicationApp = angular.module('medicationApp', ['restangular']);

medicationApp.controller('medicationListCtrl', function ($scope, Restangular) {
  var drugs = Restangular.all('drugs');

  drugs.getList().then(function (drugs) {
    $scope.drugs = drugs;
  });

  $scope.submitDrug = function () {
    drugs.post({ userInput: $scope.userInput }).then(function (drug) {
      $scope.drugs.push(drug);
    }, function (error) {
      console.log(error);
    })
  }
});
