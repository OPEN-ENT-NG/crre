import { ng } from 'entcore';

export const campaignMainController = ng.controller('campaignMainController',
    ['$scope', ($scope) => {

        $scope.openCatalog = (id: number) => {
            $scope.redirectTo(`/structure/${$scope.current.structure.id}/campaign/${id}/catalog`);
        };

        $scope.openBasket = (id: number) => {
            $scope.redirectTo(`/structure/${$scope.current.structure.id}/campaign/${id}/basket`);
        };

        $scope.openOrder = (id: number) => {
            $scope.redirectTo(`/structure/${$scope.current.structure.id}/campaign/${id}/order`);
        };

        $scope.openWaitingOrder = () => {
            $scope.redirectTo(`/structure/${$scope.current.structure.id}/order/waiting`);
        };

        $scope.openHistoric = () => {
            $scope.redirectTo(`/structure/${$scope.current.structure.id}/order/historic`);
        };

        $scope.backHome = () => {
            $scope.redirectTo(`/`);
        };

    }]);


