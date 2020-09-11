import { ng } from 'entcore';

export const campaignMainController = ng.controller('campaignMainController',
    ['$scope', ($scope) => {
        $scope.openCatalog = () => {
            $scope.redirectTo(`/equipments/catalog`);
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
        $scope.backHome = () => {
            $scope.redirectTo(`/`);
        };
        $scope.returnPath = (s: string) => {
            return /[^\/]+[\/]?$/.exec(s);
        }

    }]);


