import {ng, toasts} from 'entcore';
import {
    BasketOrder,
    OrderClient,
    Utils
} from '../../../model';

export const orderController = ng.controller('orderController',
    ['$scope', async ($scope) => {

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
                if(!!amount && amount > 0) {
                    orderClient.amount = amount;
                    (orderClient.status !== 'REJECTED') ? await orderClient.updateAmount(amount) : null;
                } else if (amount == 0) {
                    toasts.warning('crre.empty.amount.error');
                }
                Utils.safeApply($scope);
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            Utils.safeApply($scope);
        };
    }]);
