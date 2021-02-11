import {_, moment, ng, template, toasts} from 'entcore';
import {
    BasketOrder, Filter, Filters,
    OrderClient,
    OrdersClient,
    orderWaiting,
    Utils
} from '../../model';


declare let window: any;

export const orderPersonnelController = ng.controller('orderPersonnelController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.display = {
            ordersClientOption: [],
            lightbox: {
                deleteOrder: false,
                deleteProject: false,
                udpateProject: false,
            },
            list: $scope.campaign.priority_field
        };
        $scope.allOrdersListSelected = false;
        $scope.tableFields = orderWaiting;
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();

        this.init = () => {
            $scope.users = [];
            $scope.reassorts = [{reassort: "true"}, {reassort: "false"}];
            $scope.filters = new Filters();
            $scope.initPopUpFilters();
        };

        $scope.exportCSV = () => {
            var order_selected = [];
            $scope.displayedBasketsOrders.forEach(function (basket) {
                basket.orders.forEach(function (order) {
                    if(order.selected) {
                        order_selected.push(order);
                    }
                });
            });
            if(order_selected.length == 0) {
                order_selected = $scope.ordersClient.all;
            }
            let params_id_order = Utils.formatKeyToParameter(order_selected, 'id');
            let equipments_key = order_selected.map( (value) => value.equipment_key).filter( (value, index, _arr) => _arr.indexOf(value) == index);
            let params_id_equipment = Utils.formatKeyToParameter(equipments_key.map( s => ({equipment_key:s})), "equipment_key");
            window.location = `/crre/orders/exports?${params_id_order}&${params_id_equipment}`;
            $scope.displayedBasketsOrders.forEach(function (basket) {
                basket.selected = false;
                basket.orders.forEach(function (order) {
                    order.selected = false;
                });
            });
        }

        $scope.initPopUpFilters = (filter?:string) => {
            let value = $scope.$eval(filter);
            $scope.showPopUpColumnsGrade = false;
            $scope.showPopUpColumnsTeacher = false;
            $scope.showPopUpColumnsReassort = false;
            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsGrade': $scope.showPopUpColumnsGrade = true; break;
                    case 'showPopUpColumnsTeacher': $scope.showPopUpColumnsTeacher = true; break;
                    case 'showPopUpColumnsReassort': $scope.showPopUpColumnsReassort = true; break;
                    default: break;
                }
            }
        };

        $scope.getFilter = async (word: string, filter: string) => {
            let newFilter = new Filter();
            newFilter.name = filter;
            newFilter.value = word;
            if ($scope.filters.all.some(f => f.value === word)) {
                $scope.filters.all.splice($scope.filters.all.findIndex(a => a.value === word) , 1)
            } else {
                $scope.filters.all.push(newFilter);
            }
            if($scope.filters.all.length > 0) {
                if (!!$scope.query_name) {
                    await $scope.basketsOrders.filter_order($scope.filters.all, $scope.campaign.id, $scope.query_name);
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                } else {
                    await $scope.basketsOrders.filter_order($scope.filters.all, $scope.campaign.id);
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                }
            } else {
                if (!!$scope.query_name) {
                    await $scope.basketsOrders.search($scope.query_name, $scope.campaign.id)
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                } else {
                    await $scope.basketsOrders.getMyOrders();
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                }
            }
        };

        $scope.getAllFilters = async () => {
            await $scope.basketsOrders.getMyOrders().then(data => {
                $scope.basketsOrders.all.forEach(function (basket) {
                    if (!$scope.users.includes(basket.name_user)) {
                        $scope.users.push({user_name: basket.name_user});
                    }
                });
                $scope.users = $scope.users.filter((v, i, a) => a.findIndex(t => (t.user_name === v.user_name)) === i);
                formatDisplayedBasketOrders();
                Utils.safeApply($scope);
            });
            $scope.equipments.getFilters();
        };

        $scope.searchByName =  async (name: string) => {
            if(name != "") {
                if($scope.filters.all.length == 0) {
                    await $scope.basketsOrders.search(name, $scope.campaign.id);
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                } else {
                    await $scope.basketsOrders.filter_order($scope.filters.all, $scope.campaign.id, name);
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                }
            } else {
                if($scope.filters.all.length == 0) {
                    await $scope.basketsOrders.getMyOrders();
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                } else {
                    await $scope.basketsOrders.filter_order($scope.filters.all, $scope.campaign.id);
                    formatDisplayedBasketOrders();
                    Utils.safeApply($scope);
                }

            }
            Utils.safeApply($scope);
        }

        $scope.hasAProposalPrice = (orderClient: OrderClient) => {

            return (orderClient.price_proposal);
        };

        $scope.switchAllOrders = (allOrdersListSelected : boolean) => {
            $scope.displayedBasketsOrders.map((basket) => {basket.selected = allOrdersListSelected;
                basket.orders.forEach(function (order) {
                    order.selected = allOrdersListSelected;
                });
            });
        };

        $scope.switchAllOrdersBasket = (basket) => {
            basket.orders.map((order) => order.selected = basket.selected);
        };

        $scope.checkParentSwitch = (basket, checker) : void => {
            if (checker) {
                let testAllTrue = true;
                basket.orders.forEach(function (order) {
                    if (!order.selected) {
                        testAllTrue = false;
                    }
                });
                basket.selected = testAllTrue;
            }
            else {
                basket.selected = false;
            }
        };

        $scope.displayEquipmentOption = (index: number) => {
            $scope.display.ordersClientOption[index] = !$scope.display.ordersClientOption[index];
            Utils.safeApply($scope);
        };

        $scope.calculateDelivreryDate = (date: Date) => {
            return moment(date).add(60, 'days').calendar();
        };

        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, true, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.updateComment = (orderClient: OrderClient) => {
            if (!orderClient.comment || orderClient.comment.trim() == " ") {
                orderClient.comment = "";

            }
            orderClient.updateComment();
        };

        $scope.updateAmount = async (basketOrder: BasketOrder, orderClient: OrderClient, amount: number) => {
            if(amount.toString() != 'undefined') {
                await orderClient.updateAmount(amount);
                await basketOrder.getAmount();
                orderClient.amount = amount;
                $scope.$apply()
            }
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            $scope.$apply()
        };

        $scope.displayLightboxDelete = (orderEquipments: OrdersClient) => {
            template.open('orderClient.delete', 'customer/campaign/order/delete-confirmation');
            $scope.ordersEquipmentToDelete = orderEquipments;
            $scope.display.lightbox.deleteOrder = true;
            Utils.safeApply($scope);
        };
        $scope.cancelOrderEquipmentDelete = () => {
            delete $scope.orderEquipmentToDelete;
            $scope.display.lightbox.deleteOrder = false;
            template.close('orderClient.delete');

            Utils.safeApply($scope);
        };


        $scope.deleteOrdersEquipment = async (ordersEquipment: OrdersClient) => {
            for (let i = 0; i < ordersEquipment.length; i++) {
                await $scope.deleteOrderEquipment(ordersEquipment[i]);
            }
            $scope.cancelOrderEquipmentDelete();
            await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id);
            Utils.safeApply($scope);
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
        const currencyFormatter = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

        const synchroMyBaskets = async () : Promise<void> => {
            if($scope.ordersClient.all.length > 0){
                $scope.displayedOrders = $scope.ordersClient.all;
            }else{
                await $scope.displayedOrders.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id);
            }
            Utils.safeApply($scope);
        };
        synchroMyBaskets();

        const formatDisplayedBasketOrders = () : void  => {
            $scope.displayedBasketsOrders = [];
            $scope.basketsOrders.forEach(function (basketOrder) {
                let displayedBasket = basketOrder;
                displayedBasket.name_user = displayedBasket.name_user.toUpperCase();
                displayedBasket.total = currencyFormatter.format(basketOrder.total);
                displayedBasket.created = moment(basketOrder.created).format('DD-MM-YYYY').toString();
                displayedBasket.expanded = false;
                displayedBasket.orders = [];
                $scope.displayedOrders.forEach(function (order) {
                    if (order.id_basket === basketOrder.id) {
                        displayedBasket.orders.push(order);
                    }
                });
                setStatus(displayedBasket, displayedBasket.orders[0]);
                $scope.displayedBasketsOrders.push(displayedBasket);
            });
        };

        function setStatus(displayedBasket, firstOrder) {
            displayedBasket.status = firstOrder.status;
            let partiallyRefused = false;
            let partiallyValided = false;
            if(displayedBasket.orders.length > 1){
                for (const order of displayedBasket.orders) {
                    if (displayedBasket.status != order.status)
                        if (order.status == 'VALID' || displayedBasket.status == 'VALID')
                            partiallyValided = true;
                        else if (order.status == 'REJECTED' || displayedBasket.status == 'REJECTED')
                            partiallyRefused = true;
                }
                if (partiallyRefused || partiallyValided) {
                    for (const order of displayedBasket.orders) {
                        order.displayStatus = true;
                    }
                    if (partiallyRefused && !partiallyValided)
                        displayedBasket.status = "PARTIALLYREJECTED"
                    else
                        displayedBasket.status = "PARTIALLYVALIDED"
                }
            }
        }
        this.init();
    }]);
