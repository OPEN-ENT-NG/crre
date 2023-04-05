import {model, moment, ng, toasts} from 'entcore';
import {
    Basket,
    BasketOrder,
    Baskets,
    BasketsOrders,
    Equipment,
    Filters,
    OrderClient,
    OrdersClient,
    Utils
} from '../../../model';
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";
import {ValidatorOrderWaitingFilter} from "../../../model/ValidatorOrderWaitingFilter";
import {IUserModel, UserModel} from "../../../model/UserModel";

export const manageOrderController = ng.controller('manageOrderController',
    ['$scope', '$routeParams', async ($scope, $routeParams) => {

        $scope.display = {
            allOrdersListSelected: false,
            toggle: false
        };
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
        $scope.filtersDate.startDate = moment().add(-6, 'months')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.displayedBasketsOrders = [];
        $scope.loading = true;

        const init = async () => {
            $scope.filters = new Filters();
            $scope.basketsOrders = new BasketsOrders();
            await $scope.getOrders();
        };

        $scope.exportCSV = () : void => {
            let order_selected : OrdersClient = new OrdersClient();
            $scope.displayedBasketsOrders.forEach(function (basket : BasketOrder) {
                basket.selected = false;
                basket.orders.all.forEach(function (order : OrderClient) {
                    if (order.selected) {
                        order_selected.all.push(order);
                    }
                    order.selected = false;
                });
            });
            const statusList: Array<string> = $scope.filter.isOld ? Utils.getOldStatus() : Utils.getCurrentStatus();
            const currentUser = new UserModel({id_user: model.me.userId, user_name: null} as IUserModel)
            if (order_selected.all.length != 0 && !$scope.display.allOrdersListSelected) {
                order_selected.exportCSV([$scope.campaign], [currentUser], $scope.query_name, $scope.current.structure.id, $scope.filtersDate.startDate, $scope.filtersDate.endDate, false, statusList);
            } else {
                order_selected.exportCSV([$scope.campaign], [currentUser], $scope.query_name, $scope.current.structure.id, $scope.filtersDate.startDate, $scope.filtersDate.endDate, true, statusList);
            }
            $scope.display.allOrdersListSelected = false;
            Utils.safeApply($scope);
        };

        $scope.filterByDate = async () : Promise<void> => {
            if ($scope.filter.isDate) {
                if ($scope.filtersDate.startDate && $scope.filtersDate.endDate &&
                    moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchByName(true);
                }
                $scope.filter.isDate = false;
            } else {
                $scope.filter.isDate = true;
            }
        };

        $scope.startInitLoading = () : void => {
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders = new BasketsOrders();
            Utils.safeApply($scope);
        }

        $scope.searchByName = async (init: boolean = false) : Promise<void> => {
            if (init) {
                $scope.startInitLoading();
            }
            let newData : BasketsOrders;
            if (!!$scope.query_name) {
                newData = await $scope.basketsOrders.search($scope.query_name, $scope.campaign.id, $scope.filtersDate.startDate,
                    $scope.filtersDate.endDate, $scope.filter.page, $scope.filter.isOld, $scope.current.structure.id);
            } else {
                newData = await $scope.basketsOrders.getMyOrders($scope.filter.page,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $routeParams.idCampaign, $scope.filter.isOld, $scope.current.structure.id);
            }
            if(newData.length > 0) {
                await $scope.synchroMyBaskets(newData);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            $scope.syncSelected();
            Utils.safeApply($scope);
        }

        $scope.syncSelected = () : void => {
            $scope.displayedBasketsOrders.forEach((basket: Basket) => basket.selected = $scope.display.allOrdersListSelected)
        };

        $scope.onScroll = async (): Promise<void> => {
            if (!$scope.isInCampaignList()) {
                $scope.filter.page++;
                await $scope.searchByName(false);
            }
        };

        $scope.synchroMyBaskets = async (newData : BasketsOrders) : Promise<void> => {
            let basketId : Array<number> = [];
            newData.forEach((basket: BasketOrder) => {
                basketId.push(basket.id);
            });
            $scope.newOrders = new OrdersClient();
            let filter: ValidatorOrderWaitingFilter = new ValidatorOrderWaitingFilter();
            filter.startDate = $scope.filtersDate.startDate;
            filter.endDate = $scope.filtersDate.endDate;
            await $scope.newOrders.syncMyOrder(filter, $routeParams.idCampaign, $scope.current.structure.id, basketId, $scope.filter.isOld);
            formatDisplayedBasketOrders();
            Utils.safeApply($scope);
        };

        const formatDisplayedBasketOrders = (): void => {
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders.forEach(function (basketOrder : BasketOrder) {
                let displayedBasket : BasketOrder = basketOrder;
                $scope.newOrders.arr.forEach(function (order : OrderClient) {
                    if (order.id_basket === basketOrder.id) {
                        displayedBasket.orders.push(order);
                    }
                });
                if (displayedBasket.orders.all.length > 0) {
                    Utils.setStatus(displayedBasket, displayedBasket.orders.all);
                }
                $scope.displayedBasketsOrders.push(displayedBasket);
            });
        };

        $scope.getOrders = async (): Promise<void> => {
            let newData : BasketsOrders = await $scope.basketsOrders.getMyOrders($scope.filter.page,
                $scope.filtersDate.startDate, $scope.filtersDate.endDate, $routeParams.idCampaign, $scope.filter.isOld, $scope.current.structure.id);
            if(newData.length > 0) {
                await $scope.synchroMyBaskets(newData);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        $scope.displayToggle = () : void => {
            $scope.display.toggle = checkOnlyRejected();
            Utils.safeApply($scope);
        };

        const checkOnlyRejected = () : boolean => {
            let displayToggle : boolean = false;
            for(const basket of $scope.displayedBasketsOrders) {
                const selectedOrders : OrdersClient = basket.orders.all.filter((order : OrderClient) => order.selected);
                if (selectedOrders.length > 0) {
                    const statusNoRejected : OrdersClient = basket.orders.all.filter((order: OrderClient) => order.selected && order.status != 'REJECTED');
                    if (statusNoRejected.length > 0) {
                        break;
                    } else {
                        displayToggle = true;
                    }
                }
            }
            return displayToggle;
        }

        $scope.reSubmit = async () : Promise<void> => {
            try {
                let totalAmount : number = 0;
                let baskets : Baskets = new Baskets();
                let ordersToResubmit : OrdersClient = new OrdersClient();
                $scope.displayedBasketsOrders.forEach((basket: BasketOrder) => {
                    basket.orders.all.forEach(async (order: OrderClient) => {
                        if (order.selected) {
                            let equipment : Equipment = new Equipment();
                            equipment.ean = order.equipment_key.toString();
                            let basket : Basket = new Basket(equipment, order.id_campaign, $scope.current.structure.id);
                            basket.amount = order.amount;
                            basket.selected = true;
                            totalAmount += order.amount;
                            baskets.push(basket);
                            ordersToResubmit.push(order);
                        }
                    });
                });

                await ordersToResubmit.resubmitOrderClient(baskets, totalAmount, $scope.current.structure);

                let {status} = await ordersToResubmit.updateStatus('RESUBMIT');
                if (status != 200) {
                        toasts.warning('crre.order.update.err');
                }

                $scope.campaign.nb_order += 1;
                $scope.campaign.order_notification += 1;
                $scope.campaign.nb_order_waiting += baskets.all.length;
                uncheckAll();
                $scope.startInitLoading();
                await $scope.getOrders();
            } catch (e) {
                console.warn(e);
                toasts.warning('crre.order.sync.err');
            }
        };

        const uncheckAll = () => {
            $scope.displayedBasketsOrders.forEach((basket : BasketOrder) => {
                basket.selected = false;
                basket.orders.all.forEach(async (order : OrderClient) => {
                    order.selected = false;
                });
            });
            $scope.display.toggle = false;
            Utils.safeApply($scope);
        };

        await init();
    }])
;
