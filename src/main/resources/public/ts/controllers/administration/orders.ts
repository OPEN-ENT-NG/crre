import {_, ng, template, toasts} from 'entcore';
import {
    BasketOrder,
    OrderClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    Utils,
    Filter, Filters
} from '../../model';
import {Mix} from 'entcore-toolkit';


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
        this.init = () => {
            $scope.users = [];
            $scope.reassorts = [{reassort: true}, {reassort: false}];
            $scope.filters = new Filters();
            $scope.initPopUpFilters();
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
                    await $scope.ordersClient.filter_order($scope.filters.all, $scope.campaign.id, $scope.query_name);
                    Utils.safeApply($scope);
                } else {
                    await $scope.ordersClient.filter_order($scope.filters.all, $scope.campaign.id);
                    Utils.safeApply($scope);
                }
            } else {
                if (!!$scope.query_name) {
                    await $scope.ordersClient.search($scope.query_name, $scope.campaign.id)
                    Utils.safeApply($scope);
                } else {
                    await $scope.ordersClient.sync('WAITING');
                    Utils.safeApply($scope);
                }
            }
        };

        $scope.getAllFilters = () => {
            $scope.ordersClient.all.forEach(function (order) {
                if(!$scope.users.includes(order.user_name)) {
                    $scope.users.push({user_name: order.user_name});
                }
            });
            $scope.users = $scope.users.filter((v, i, a) => a.findIndex(t=> (t.user_name === v.user_name)) === i)
        };

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            let totalPrice = 0;
            let totalAmount = 0;
            if($scope.ordersClient.selectedElements.length > 0) {
                $scope.ordersClient.selectedElements.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    totalPrice += order.price*order.amount
                    totalAmount += order.amount
                });
            } else {
                $scope.ordersClient.all.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
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
                    $scope.orderToCreate = new OrderRegion();
                    await $scope.syncOrders('WAITING');
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

        $scope.searchByName =  async (name: string) => {
            if(name != "") {
                if($scope.filters.all.length == 0) {
                    await $scope.ordersClient.search(name, $scope.campaign.id);
                    Utils.safeApply($scope);
                } else {
                    await $scope.ordersClient.filter_order($scope.filters.all, $scope.campaign.id, name);
                    Utils.safeApply($scope);
                }
            } else {
                if($scope.filters.all.length == 0) {
                    await $scope.ordersClient.sync('WAITING');
                    Utils.safeApply($scope);
                } else {
                    await $scope.ordersClient.filter_order($scope.filters.all, $scope.campaign.id);
                    Utils.safeApply($scope);
                }

            }
            Utils.safeApply($scope);
        }


        function openLightboxValidOrder(status, data, ordersToValidat: OrdersClient) {
            if (status === 200) {
                $scope.orderValidationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                    structures: _.uniq(_.pluck(ordersToValidat.all, 'name_structure'))
                };
                template.open('validOrder.lightbox', 'validator/order-valid-confirmation');
                $scope.display.lightbox.validOrder = true;
            }
        }

        $scope.openLightboxRefuseOrder = () => {
                template.open('refuseOrder.lightbox', 'validator/order-refuse-confirmation');
                $scope.display.lightbox.refuseOrder = true;
        }

        $scope.validateOrders = async (orders: OrderClient[]) => {
            let ordersToValidat  = new OrdersClient();
            ordersToValidat.all = Mix.castArrayAs(OrderClient, orders);
            let { status, data } = await ordersToValidat.updateStatus('VALID');
            openLightboxValidOrder(status, data, ordersToValidat);
            $scope.getOrderWaitingFiltered($scope.campaign);
            Utils.safeApply($scope);
        };
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
                $scope.getOrderWaitingFiltered($scope.campaign);
                await $scope.syncOrders('WAITING');
                toasts.confirm('crre.order.refused.succes');
                Utils.safeApply($scope);
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

        $scope.syncOrders = async (status: string) =>{
            await $scope.ordersClient.sync(status, $scope.structures.all);
            $scope.displayedOrders.all = $scope.ordersClient.all;
            $scope.displayedOrders.all.map(order => {
                    order.selected = false;
            });
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
        $scope.displayedOrdersRegionOrders = [];

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

        this.init();
    }]);