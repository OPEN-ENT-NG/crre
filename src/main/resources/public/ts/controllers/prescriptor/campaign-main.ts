import { ng } from 'entcore';

export const campaignMainController = ng.controller('campaignMainController',
    ['$scope', ($scope) => {

        $scope.openCatalog = (id: number) => {
            $scope.redirectTo(`/equipments/catalog/${id}`);
        };

        $scope.openBasket = (id: number) => {
            $scope.redirectTo(`/campaign/${id}/basket`);
        };

        $scope.openOrder = (id: number) => {
            $scope.redirectTo(`/campaign/${id}/order`);
        };

        $scope.openWaitingOrder = (id: number) => {
            $scope.redirectTo(`/order/${id}/waiting`);
        };

        $scope.openHistoric = (id: number) => {
            $scope.redirectTo(`/order/${id}/historic`);
        };

        $scope.backHome = () => {
            $scope.redirectTo(`/`);
        };

    }]);


