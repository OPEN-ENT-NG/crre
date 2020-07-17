import { ng } from 'entcore';

export const campaignMainController = ng.controller('campaignMainController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.openCatalog = () => {
            $scope.redirectTo(`/campaign/${$routeParams.idCampaign}/catalog`);
        };
        $scope.openBasket = () => {
            $scope.redirectTo(`/campaign/${$routeParams.idCampaign}/basket`);
        };
        $scope.openOrder = () => {
            $scope.redirectTo(`/campaign/${$routeParams.idCampaign}/order`);
        };
        $scope.backHome = () => {
            $scope.redirectTo(`/`);
        };
    }]);