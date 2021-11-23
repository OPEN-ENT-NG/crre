import {moment, ng, toasts} from 'entcore';
import {
    BasketsOrders,
    Filters,
    OrderClient,
    OrdersClient,
    Utils
} from '../../../model';
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";

export const manageOrderController = ng.controller('manageOrderController',
    ['$scope', '$routeParams', async ($scope, $routeParams) => {

        $scope.allOrdersListSelected = false;
        $scope.show = {
            comment: false
        };
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.filter = {
            page: 0,
            isDate: false,
            isOld: false
        };
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.displayedBasketsOrders = [];
        $scope.loading = true;

        this.init = async () => {
            $scope.reassorts = [{reassort: "true"}, {reassort: "false"}];
            $scope.filters = new Filters();
            $scope.basketsOrders = new BasketsOrders();
            await $scope.getOrders();
        };

        $scope.exportCSV = () => {
            let order_selected = new OrdersClient();
            let all_order = new OrdersClient();
            $scope.displayedBasketsOrders.forEach(function (basket) {
                basket.selected = false;
                basket.orders.forEach(function (order) {
                    if (order.selected) {
                        order_selected.all.push(order);
                    }
                    all_order.all.push(order);
                    order.selected = false;
                });
            });
            $scope.allOrdersListSelected = false;
            if (order_selected.length != 0) {
                order_selected.exportCSV($scope.filter.isOld);
            } else {
                all_order.exportCSV($scope.filter.isOld);
            }
        };

        $scope.filterByDate = async () => {
            if ($scope.filter.isDate) {
                if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchByName(true);
                } else {
                    toasts.warning('crre.date.err');
                }
                $scope.filter.isDate = false;
            } else {
                $scope.filter.isDate = true;
            }
        };

        $scope.startInitLoading = () => {
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders = new BasketsOrders();
            Utils.safeApply($scope);
        }

        $scope.searchByName = async (init: boolean = false) => {
            if (init) {
                $scope.startInitLoading();
            }
            let newData: boolean;
            if (!!$scope.query_name) {
                newData = await $scope.basketsOrders.search($scope.query_name, $scope.campaign.id, $scope.filtersDate.startDate,
                    $scope.filtersDate.endDate, $scope.filter.page, $scope.filter.isOld);
            } else {
                newData = await $scope.basketsOrders.getMyOrders($scope.filter.page,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $routeParams.idCampaign, $scope.filter.isOld);
            }
            if(newData) {
                await $scope.synchroMyBaskets();
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        $scope.onScroll = async (): Promise<void> => {
            if(!$scope.isInCampaignList()) {
                $scope.filter.page++;
                await $scope.searchByName(false);
            }
        };

        $scope.synchroMyBaskets = async (): Promise<void> => {
            if ($scope.basketsOrders.all.length != 0) {
                let ordersId = [];
                $scope.basketsOrders.forEach((order) => {
                    ordersId.push(order.id);
                });
                $scope.newOrders = new OrdersClient();
                await $scope.newOrders.sync(null, $scope.filtersDate.startDate, $scope.filtersDate.endDate, [],
                    $routeParams.idCampaign, $scope.current.structure.id, ordersId, null, $scope.filter.isOld);
                formatDisplayedBasketOrders();
            }
            Utils.safeApply($scope);
        };

        const formatDisplayedBasketOrders = (): void => {
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders.forEach(function (basketOrder) {
                let displayedBasket = basketOrder;
                $scope.newOrders.arr.forEach(function (order) {
                    if (order.id_basket === basketOrder.id) {
                        displayedBasket.orders.push(order);
                    }
                });
                if (displayedBasket.orders.length > 0) {
                    Utils.setStatus(displayedBasket, displayedBasket.orders[0]);
                }
                $scope.displayedBasketsOrders.push(displayedBasket);
            });
        };

        $scope.getOrders = async (): Promise<void> => {
            let newData = await $scope.basketsOrders.getMyOrders($scope.filter.page,
                $scope.filtersDate.startDate, $scope.filtersDate.endDate, $routeParams.idCampaign, $scope.filter.isOld);
            if(newData) {
                await $scope.synchroMyBaskets();
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }
        this.init();
    }])
;
