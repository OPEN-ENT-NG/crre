import {moment, ng, toasts} from 'entcore';
import {
    BasketOrder,
    BasketsOrders,
    Filters, Offer, Offers,
    OrderClient,
    OrdersClient,
    Utils
} from '../../model';
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";
import http from "axios";


declare let window: any;

export const orderValidatorController = ng.controller('orderValidatorController',
    ['$scope', '$routeParams', async ($scope, $routeParams) => {

        $scope.display = {
            lightbox: {
                deleteOrder: false,
            },
        };
        $scope.allOrdersListSelected = false;
        $scope.show = {
            comment: false
        };
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.filter = {
            page: 0
        };
        $scope.isDate = false;
        $scope.isOld = false;
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.displayedBasketsOrders = [];
        this.init = () => {
            $scope.users = [];
            $scope.reassorts = [{reassort: "true"}, {reassort: "false"}];
            $scope.filters = new Filters();
        };

        $scope.exportCSV = () => {
            var order_selected = [];
            $scope.displayedBasketsOrders.forEach(function (basket) {
                basket.orders.forEach(function (order) {
                    if (order.selected) {
                        order_selected.push(order);
                    }
                });
            });
            if (order_selected.length == 0) {
                order_selected = $scope.ordersClient.all;
            }
            let params_id_order = Utils.formatKeyToParameter(order_selected, 'id');
            let equipments_key = order_selected.map((value) => value.equipment_key).filter((value, index, _arr) => _arr.indexOf(value) == index);
            let params_id_equipment = Utils.formatKeyToParameter(equipments_key.map(s => ({equipment_key: s})), "equipment_key");
            if($scope.isOld) {
                window.location = `/crre/orders/old/exports?${params_id_order}&${params_id_equipment}`;
            } else {
                window.location = `/crre/orders/exports?${params_id_order}&${params_id_equipment}`;
            }
            $scope.displayedBasketsOrders.forEach(function (basket) {
                basket.selected = false;
                basket.orders.forEach(function (order) {
                    order.selected = false;
                });
            });
        }

        $scope.changeOld = async (old: boolean) => {
            $scope.isOld = old;
            $scope.filter.page = 0;
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders = new BasketsOrders();
            await synchroMyBaskets(false, $scope.isOld)
            Utils.safeApply($scope);
        }

        $scope.filterByDate = async () => {
            if($scope.isDate) {
                if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchByName(false);
                } else {
                    toasts.warning('crre.date.err');
                }
                $scope.isDate = false;
            } else {
                $scope.isDate = true;
            }
        }
        $scope.searchByName = async (noInit?: boolean) => {
            let isEmpty: boolean;
            if (!noInit) {
                $scope.loading = true;
                $scope.filter.page = 0;
                $scope.displayedBasketsOrders = [];
                $scope.basketsOrders = new BasketsOrders();
                Utils.safeApply($scope);
            }
            if ($scope.query_name && $scope.query_name != "") {
                if ($scope.filters.all.length == 0) {
                    await $scope.newBasketsOrders.search($scope.query_name, $scope.campaign.id, $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $scope.filter.page, $scope.isOld);
                    isEmpty = await synchroMyBaskets(true, $scope.isOld);
                } else {
                    await $scope.newBasketsOrders.filter_order($scope.filters.all, $scope.campaign.id, $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $scope.query_name, $scope.filter.page, $scope.isOld);
                    isEmpty = await synchroMyBaskets(true, $scope.isOld);
                }
            } else {
                if ($scope.filters.all.length == 0) {
                    isEmpty = await synchroMyBaskets(false, $scope.isOld);
                } else {
                    await $scope.newBasketsOrders.filter_order($scope.filters.all, $scope.campaign.id, $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $scope.filter.page, $scope.isOld);
                    isEmpty = await synchroMyBaskets(true, $scope.isOld);
                }
            }
            Utils.safeApply($scope);
            return isEmpty;
        }

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

        $scope.displayEquipmentOption = (index: number) => {
            $scope.display.ordersClientOption[index] = !$scope.display.ordersClientOption[index];
            Utils.safeApply($scope);
        };

        $scope.calculateDelivreryDate = (date: Date) => {
            return moment(date).add(60, 'days').calendar();
        };

        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.updateComment = (orderClient: OrderClient) => {
            if (!orderClient.comment || orderClient.comment.trim() == " ") {
                orderClient.comment = "";
            }
            orderClient.updateComment();
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
                $scope.$apply()
            }
        };

        const getEquipment = (order) :Promise <any> => {
            return http.get(`/crre/equipment/${order.equipment_key}`);
        }

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            $scope.$apply()
        };

        $scope.deleteOrderEquipment = async (orderEquipmentToDelete: OrderClient) => {
            let {status, data} = await orderEquipmentToDelete.delete();
            if (status === 200) {
                $scope.campaign.nb_order = data.nb_order;
                $scope.campaign.purse_amount = data.amount;
                ($scope.campaign.purse_enabled) ? toasts.confirm('crre.orderEquipment.delete.confirm')
                    : toasts.confirm('crre.requestEquipment.delete.confirm');
            }
        };

        // Functions specific for baskets interactions

        $scope.displayedBasketsOrders = [];
        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.searchByName(true, $scope.isOld);
        };

        const synchroMyBaskets = async (search?: boolean, old = false): Promise<boolean> => {
            let isEmpty = true;
            if (!search) {
                $scope.newBasketsOrders = new BasketsOrders();
                if ($routeParams.idCampaign) {
                    await $scope.newBasketsOrders.getMyOrders($scope.filter.page, $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $routeParams.idCampaign, old);
                }
            }
            if ($scope.newBasketsOrders.all.length != 0) {
                isEmpty = false;
                let ordersId = [];
                $scope.newBasketsOrders.forEach((order) => {
                    ordersId.push(order.id);
                });
                $scope.newOrders = new OrdersClient();
                await $scope.newOrders.sync(null, $scope.filtersDate.startDate, $scope.filtersDate.endDate, [], $routeParams.idCampaign, $scope.current.structure.id, ordersId, null, old);
                formatDisplayedBasketOrders();
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
            return isEmpty;
        };
        $scope.loading = true;
        synchroMyBaskets(false);

        const formatDisplayedBasketOrders = (): void => {
            $scope.newBasketsOrders.forEach(function (basketOrder, index) {
                let displayedBasket = basketOrder;
                displayedBasket.name_user = displayedBasket.name_user.toUpperCase();
                displayedBasket.total = currencyFormatter.format(basketOrder.total);
                displayedBasket.created = moment(basketOrder.created).format('DD-MM-YYYY').toString();
                displayedBasket.expanded = false;
                displayedBasket.orders = [];
                $scope.newOrders.forEach(function (order) {
                    if (order.id_basket === basketOrder.id) {
                        displayedBasket.orders.push(order);
                    }
                });
                if (displayedBasket.orders.length > 0) {
                    Utils.setStatus(displayedBasket, displayedBasket.orders[0]);
                }
                $scope.displayedBasketsOrders.push(displayedBasket);
                $scope.basketsOrders.all.push(basketOrder);
            });
        };
        this.init();
    }]);
