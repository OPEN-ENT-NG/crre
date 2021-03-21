import {_, ng, template, toasts} from 'entcore';
import {
    BasketOrder,
    OrderClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    Utils,
    Filter, Filters, BasketsOrders
} from '../../model';
import {Mix} from 'entcore-toolkit';
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";


declare let window: any;
export const orderController = ng.controller('orderController',
    ['$scope', '$location', ($scope, $location,) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.allOrdersSelected = false;
        $scope.show = {
            comment:false
        };
        $scope.projects = [];
        $location.path() === "/order/waiting";
        $scope.sort = {
            order : {
                type: 'created',
                reverse: false
            }
        }
        $scope.filter = {
            page: 0
        };
        // @ts-ignore
        this.init = async () => {
            $scope.users = [];
            $scope.reassorts = [{reassort: true}, {reassort: false}];
            $scope.filters = new Filters();
            if($scope.equipments.grades.length === 0){
                $scope.equipments.loading = true;
                $scope.equipments.all = [];
                await $scope.equipments.sync(true, undefined, undefined);
            }
            $scope.initPopUpFilters();
            await $scope.getAllFilters();
            $scope.loading = false;
            Utils.safeApply($scope);
        };

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

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.searchByName(true);
        };

        $scope.getFilter = async (word: string, filter: string) => {
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.ordersClient = new OrdersClient();
            Utils.safeApply($scope);
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
                    const newData = await $scope.ordersClient.filter_order($scope.filters.all, null, $scope.query_name, $scope.filter.page );
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.filter_order($scope.filters.all, null, $scope.filter.page );
                    endLoading(newData);
                }
            } else {
                if (!!$scope.query_name) {
                    const newData = await $scope.ordersClient.search($scope.query_name, null, $scope.filter.page );
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.sync('WAITING', $scope.structures.all, null, null, null, $scope.filter.page);
                    endLoading(newData);
                }
            }
        };

        $scope.getAllFilters = async () => {
            $scope.users = await $scope.ordersClient.getUsers('WAITING');
        };

        $scope.licencesAvailable = () => {
            return $scope.campaign.nb_licences_available - $scope.displayedOrders.calculTotalAmount() < 0;
        }

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            let ordersToRemove = new OrdersClient();
            let totalPrice = 0;
            let totalAmount = 0;
            if($scope.ordersClient.selectedElements.length > 0) {
                $scope.ordersClient.selectedElements.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    ordersToRemove.all.push(order);
                    totalPrice += order.price*order.amount
                    totalAmount += order.amount
                });
            } else {
                $scope.ordersClient.all.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    ordersToRemove.all.push(order);
                    totalPrice += order.price*order.amount
                    totalAmount += order.amount
                });
            }
            ordersToCreate.create().then(async data =>{
                if (data.status === 201) {
                    toasts.confirm('crre.order.region.create.message');
                    $scope.campaign.purse_amount -= totalPrice;
                    $scope.campaign.nb_licences_available -= totalAmount;
                    if($scope.ordersClient.selectedElements.length > 0) {
                        $scope.campaign.nb_order_waiting -= $scope.ordersClient.selectedElements.length;
                        $scope.campaign.historic_etab_notification += $scope.ordersClient.selectedElements.length;
                    } else {
                        $scope.campaign.nb_order_waiting -= $scope.ordersClient.all.length;
                        $scope.campaign.historic_etab_notification += $scope.ordersClient.all.length;
                    }
                    ordersToRemove.all.forEach(order =>{
                        $scope.ordersClient.all.forEach((orderClient, i) => {
                            if(orderClient.id == order.id){
                                $scope.ordersClient.all.splice(i,1);
                            }
                        });
                    });
                    $scope.displayedOrders.all = $scope.ordersClient.all;
                    await $scope.getAllFilters();
                    Utils.safeApply($scope);
                    $scope.onScroll();
                }
                else {
                    toasts.warning('crre.admin.order.create.err');
                }
            })
        }

        $scope.display = {
            ordersClientOptionOption : [],
            lightbox : {
                deleteOrder : false,
                sendOrder : false,
                validOrder : false,
            },
            generation: {
                type: 'ORDER'
            }
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.jsonPref = (prefs) => {
            let json = {};
            prefs.forEach(pref =>{
                json[pref.fieldName]= pref.display;
            });
            return json;
        };

        $scope.getTotalAmount = () => {
            let total = 0;
            $scope.basketsOrders.all.forEach(basket => {
                total += parseFloat(basket.amount);
            });
            return total;
        }

        $scope.switchAllOrders = () => {
            $scope.displayedOrders.all.map((order) => order.selected = $scope.allOrdersSelected);
        };

        $scope.getSelectedOrders = () => $scope.displayedOrders.selected;

        $scope.getStructureGroupsList = (structureGroups: string[]): string => {
            return structureGroups.join(', ');
        };

        function endLoading(newData: any) {
            if (newData)
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        $scope.searchByName =  async (noInit?:boolean) => {
            if(!noInit){
                $scope.loading = true;
                $scope.filter.page = 0;
                $scope.ordersClient = new OrdersClient();
                Utils.safeApply($scope);
            }

            if($scope.query_name && $scope.query_name != "") {
                if($scope.filters.all.length == 0) {
                    const newData = await $scope.ordersClient.search($scope.query_name, null, $scope.filter.page );
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.filter_order($scope.filters.all, null, $scope.query_name, $scope.filter.page );
                    endLoading(newData);
                }
            } else {
                if($scope.filters.all.length == 0) {
                    const newData = await $scope.ordersClient.sync('WAITING',  $scope.structures.all, null, null, null, $scope.filter.page);
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.filter_order($scope.filters.all, null, null, $scope.filter.page );
                    endLoading(newData);
                }

            }
            Utils.safeApply($scope);
        }
        $scope.openLightboxRefuseOrder = () => {
                template.open('refuseOrder.lightbox', 'validator/order-refuse-confirmation');
                $scope.display.lightbox.refuseOrder = true;
        }

        $scope.cancelBasketDelete = () => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
            if($scope.operationId) {
                $scope.ordersClient.selected.map(orderSelected =>{
                    $scope.displayedOrders.all = $scope.displayedOrders.all.filter(order => order.id !== orderSelected.id)
                    $scope.ordersClient.all = $scope.ordersClient.all.filter(order => order.id !== orderSelected.id)
                    orderSelected.selected = false ;
                })
                $scope.operationId = undefined;
            }
            Utils.safeApply($scope);
        };

        $scope.confirmRefuseOrder = async () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            let ordersToRefuse  = new OrdersClient();
            ordersToRefuse.all = Mix.castArrayAs(OrderClient, $scope.ordersClient.selected);
            let {status} = await ordersToRefuse.updateStatus('REFUSED');
            if(status == 200){
                $scope.campaign.nb_order_waiting = $scope.campaign.nb_order_waiting - $scope.ordersClient.selected.length;
                ordersToRefuse.all.forEach(order =>{
                    $scope.ordersClient.all.forEach((orderClient, i) => {
                        if(orderClient.id == order.id){
                            $scope.ordersClient.all.splice(i,1);
                        }
                    });
                });
                $scope.displayedOrders.all = $scope.ordersClient.all;
                $scope.getAllFilters();
                toasts.confirm('crre.order.refused.succes');
                Utils.safeApply($scope);
                $scope.onScroll();
            } else {
                toasts.warning('crre.order.refused.error');
            }
        };

        $scope.closedLighbtox= () =>{
            $scope.display.lightbox.validOrder = false;
            if($scope.operationId) {
                $scope.redirectTo(`/operation/order/${$scope.operationId}`)
                $scope.operationId = undefined;
            }
            Utils.safeApply($scope);

        };

        $scope.exportCSV = () => {
            let order_selected;
            if($scope.ordersClient.selectedElements.length == 0) {
                order_selected = $scope.ordersClient.all;
            } else {
                order_selected = $scope.ordersClient.selectedElements;
            }
            let params_id_order = Utils.formatKeyToParameter(order_selected, 'id');
            let equipments_key = order_selected.map( (value) => value.equipment_key).filter( (value, index, _arr) => _arr.indexOf(value) == index);
            let params_id_equipment = Utils.formatKeyToParameter(equipments_key.map( s => ({equipment_key:s})), "equipment_key");
            window.location = `/crre/orders/exports?${params_id_order}&${params_id_equipment}`;
            $scope.ordersClient.selectedElements.forEach(function (order) {
                order.selected = false;
            });
        }

        $scope.updateAmount = async (orderClient: OrderClient, amount: number) => {
            if(amount.toString() != 'undefined') {
                await orderClient.updateAmount(amount);
                let basket = new BasketOrder();
                basket.setIdBasket(orderClient.id_basket);
                await basket.getAmount();
                orderClient.amount = amount;
                $scope.$apply()
            }
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            $scope.$apply()
        };

        // Functions specific for baskets interactions

        $scope.displayedBasketsOrders = [];

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

        $scope.cancelRefuseOrder = () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            Utils.safeApply($scope);
        };

        // @ts-ignore
        this.init();
    }]);