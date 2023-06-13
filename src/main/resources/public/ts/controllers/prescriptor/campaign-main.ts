import { ng } from 'entcore';

export const campaignMainController = ng.controller('campaignMainController',
    ['$scope', ($scope) => {

        $scope.openCatalog = (id: number) => {
            if (!id) {
                id = 0;
            }
            $scope.redirectTo(`/equipments/catalog/${id}`);
        };

        $scope.openBasket = (id: number) => {
            $scope.redirectTo(`/campaign/${id}/basket`);
        };

        $scope.openOrder = (id: number) => {
            $scope.redirectTo(`/campaign/${id}/order`);
        };

        $scope.openWaitingOrder = () => {
            $scope.redirectTo(`/order/waiting`);
        };

        $scope.openHistoric = () => {
            $scope.redirectTo(`/order/historic`);
        };

        $scope.backHome = () => {
            $scope.redirectTo(`/`);
        };

    }]);


