import {ng} from 'entcore';
import {
    BasketOrder,
    OrderClient,
    Utils
} from '../../../model';

export const orderController = ng.controller('orderController',
    ['$scope', async ($scope) => {

        $scope.changeOld = async (old: boolean) => {
            if ($scope.filter.isOld !== old) {
                $scope.filter.isOld = old;
                $scope.startInitLoading();
                await $scope.getOrders();
            }
        };

        $scope.switchAllOrders = (allOrdersListSelected: boolean) => {
            $scope.displayedBasketsOrders.map((basket) => {
                basket.selected = allOrdersListSelected;
                basket.orders.all.forEach(function (order) {
                    order.selected = allOrdersListSelected;
                });
            });
        };

        $scope.switchAllOrdersBasket = (basket) => {
            basket.orders.all.map((order) => order.selected = basket.selected);
        };

        $scope.checkSwitchAll = (): void => {
            let testAllTrue = true;
            $scope.displayedBasketsOrders.forEach(function (basket) {
                if (!basket.selected) {
                    testAllTrue = false;
                }
            });
            $scope.display.allOrdersListSelected = testAllTrue;
            Utils.safeApply($scope);
        };

        $scope.updateAmount = async (basketOrder: BasketOrder, orderClient: OrderClient, amount: number) => {
            if (amount.toString() != 'undefined') {
                orderClient.amount = amount;
                (orderClient.status !== 'REJECTED') ? await orderClient.updateAmount(amount) : null;
                Utils.safeApply($scope);
            }
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            Utils.safeApply($scope);
        };
    }]);
