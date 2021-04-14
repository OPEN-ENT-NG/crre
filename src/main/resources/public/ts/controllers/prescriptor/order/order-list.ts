import {ng} from 'entcore';
import {
    BasketOrder,
    OrderClient,
    Utils
} from '../../../model';
import http from "axios";

export const orderController = ng.controller('orderController',
    ['$scope', async ($scope) => {

        $scope.changeOld = async (old: boolean) => {
            if($scope.isOld !== old) {
                $scope.isOld = old;
                $scope.startInitLoading();
                await $scope.synchroMyBaskets(false, $scope.isOld)
            }
        };

        $scope.switchAllOrders = (allOrdersListSelected: boolean) => {
            $scope.displayedBasketsOrders.map((basket) => {
                basket.selected = allOrdersListSelected;
                basket.orders.forEach(function (order) {
                    order.selected = allOrdersListSelected;
                });
            });
        };

        $scope.switchAllOrdersBasket = (basket) => {
            basket.orders.map((order) => order.selected = basket.selected);
        };

        $scope.updateAmount = async (basketOrder: BasketOrder, orderClient: OrderClient, amount: number) => {
            if (amount.toString() != 'undefined') {
                await orderClient.updateAmount(amount);
                await basketOrder.getAmount();
                orderClient.amount = amount;
                await getEquipment(orderClient).then(equipments => {
                    let equipment = equipments.data;
                    if(equipment.type === "articlenumerique") {
                        orderClient.offers = Utils.computeOffer(orderClient, equipment);
                    }
                });
                Utils.safeApply($scope);
            }
        };

        const getEquipment = (order) :Promise <any> => {
            return http.get(`/crre/equipment/${order.equipment_key}`);
        }

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            Utils.safeApply($scope);
        };
    }]);
