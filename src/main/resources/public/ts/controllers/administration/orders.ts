import {_, angular, idiom as lang, model, ng, template, toasts} from 'entcore';
import http from "axios";
import {
    BasketOrder,
    Campaign, OrderClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    orderWaiting,
    PRIORITY_FIELD,
    Utils,
    Filter, Filters
} from '../../model';
import {Mix} from 'entcore-toolkit';


declare let window: any;
export const orderController = ng.controller('orderController',
    ['$scope', '$location', ($scope, $location,) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.allOrdersSelected = false;
        $scope.tableFields = orderWaiting;
        $scope.projects = [];
        let isPageOrderWaiting = $location.path() === "/order/waiting";
        let isPageOrderSent = $location.path() === "/order/sent";

        if(isPageOrderSent)
            $scope.displayedOrdersSent = $scope.displayedOrders;
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



        $scope.filterDisplayedOrders = async () => {
            let searchResult = [];
            let regex;
            const matchStructureGroups = (structureGroups: string[]): boolean => {
                let bool: boolean = false;
                if (typeof structureGroups === 'string') structureGroups = Utils.parsePostgreSQLJson(structureGroups);
                structureGroups.map((groupName) => bool = bool || regex.test(groupName.toLowerCase()));
                return bool;
            };
            if($scope.search.filterWords.length > 0){
                if(isPageOrderWaiting)await $scope.selectCampaignShow($scope.campaign);
                $scope.search.filterWords.map((searchTerm: string, index: number): void => {
                    let searchItems: OrderClient[] = index === 0 ? $scope.displayedOrders.all : searchResult;
                    regex = Utils.generateRegexp([searchTerm]);

                    searchResult = _.filter(searchItems, (order: OrderClient) => {
                        return ('name_structure' in order ? regex.test(order.name_structure.toLowerCase()) : false)
                            || ('structure' in order && order.structure['name'] ? regex.test(order.structure.name.toLowerCase()): false)
                            || ('structure' in order && order.structure['city'] ? regex.test(order.structure.city.toLocaleLowerCase()) : false)
                            || ('structure' in order && order.structure['academy'] ? regex.test(order.structure.academy.toLowerCase()) : false)
                            || ('structure' in order && order.structure['type'] ? regex.test(order.structure.type.toLowerCase()) : false)
                            || ('campaign' in order ? regex.test(order.campaign.name.toLowerCase()) : false)
                            || ('name' in order ? regex.test(order.name.toLowerCase()) : false)
                            || matchStructureGroups(order.structure_groups)
                            || (order.number_validation !== null
                                ? regex.test(order.number_validation.toLowerCase())
                                : false)
                            || (order.order_number !== null && 'order_number' in order
                                ? regex.test(order.order_number.toLowerCase())
                                : false)
                            || (order.label_program !== null && 'label_program' in order
                                ? regex.test(order.label_program.toLowerCase())
                                : false)
                    });
                });
                $scope.displayedOrders.all = searchResult;
            } else {
                if(isPageOrderWaiting)
                    $scope.selectCampaignShow($scope.campaign)
                else {
                    $scope.displayedOrders.all = $scope.ordersClient.all ;
                }

            }
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
            $scope.equipments.getFilters();
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
                    totalPrice += order.priceTotalTTC
                    totalAmount += order.amount
                });
            } else {
                $scope.ordersClient.all.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    totalPrice += order.priceTotalTTC
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
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, false, roundNumber) * orderClient.amount;
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

        $scope.windUpOrders = async (orders: OrderClient[]) => {
            let ordersToWindUp  = new OrdersClient();
            // console.log($scope.displayedOrders.all);
            ordersToWindUp.all = Mix.castArrayAs(OrderClient, orders);
            let { status } = await ordersToWindUp.updateStatus('DONE');
            if (status === 200) {
                toasts.confirm('crre.windUp.notif');
            }
            await $scope.syncOrders('SENT');
            while ($scope.displayedOrders.selected.length > 0){
                Utils.safeApply($scope);
            }
            $scope.allOrdersSelected = false;
            Utils.safeApply($scope);

        };
        $scope.isNotValidated = ( orders:OrderClient[]) =>{

            let order  = orders.find(order => order.status === "SENT")
            return order != undefined
        };

        $scope.validateSentOrders = (orders: OrderClient[]) => {
            if (_.where(orders, { status : 'SENT' }).length > 0) {
                let orderNumber = orders[0].order_number;
                return _.every(orders, (order) => order.order_number === orderNumber);
            } else {
                let id_suppliers = (_.uniq(_.pluck(orders, 'id_contract')));
                return (id_suppliers.length === 1);
            }
        };

        $scope.disableCancelValidation = (orders: OrderClient[]) => {
            return _.where(orders, { status : 'SENT' }).length > 0;
        };

        $scope.prepareSendOrder = async (orders: OrderClient[]) => {
            if ($scope.validateSentOrders(orders)) {
                try {
                    await $scope.programs.sync();
                    await $scope.initOrdersForPreview(orders);
                } catch (e) {
                    console.error(e);
                    toasts.warning('crre.order.pdf.preview.error');
                } finally {
                    if ($scope.orderToSend.hasOwnProperty('preview')) {
                        $scope.redirectTo('/order/preview');
                    }
                    Utils.safeApply($scope);
                }
            }
        };
        $scope.validatePrepareSentOrders = (orderToSend: OrdersClient) => {
            return orderToSend;
        };
        $scope.sendOrders = async (orders: OrdersClient) => {
            let { status } = await orders.updateStatus('SENT');
            if (status === 201) {
                toasts.confirm( 'crre.sent.order');
                toasts.info( 'crre.sent.export.BC');
            }
            $scope.redirectTo('/order/valid');
            Utils.safeApply($scope);
        };

        $scope.saveByteArray = (reportName, data) => {
            let blob = new Blob([data]);
            let link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download =  reportName + '.pdf';
            document.body.appendChild(link);
            link.click();
            setTimeout(function() {
                document.body.removeChild(link);
                window.URL.revokeObjectURL(link.href);
            }, 100);
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


        $scope.getUsername = () => model.me.username;

        $scope.concatOrders = () => {
            let arr = [];
            $scope.orderToSend.preview.certificates.map((certificate) => {
                arr = [...arr, ...certificate.orders];
            });
            return arr;
        };

        $scope.isValidOrdersWaitingSelection = () => {
            const orders: OrderClient[] = $scope.getSelectedOrders();
            if (orders.length > 1) {
                let isValid: boolean = true;
                let contractId = orders[0].id_contract;
                for (let i = 1; i < orders.length; i++) {
                    isValid = isValid && (contractId === orders[i].id_contract);
                }
                return isValid;
            } else {
                return true;
            }
        };

        function isSentOrDone(orders: OrderClient[]) {
            return _.where(orders, {status: 'SENT'}).length === orders.length || (_.where(orders, {status: 'DONE'}).length === orders.length) && $scope.validateSentOrders(orders);
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

        $scope.countColSpan = (field:string):number =>{
            let totaux = $scope.isValidator() ? 1 :0;
            let price = $scope.isValidator() ? 1 : 0;
            let amount_field = 8;
            for (let _i = 0; _i < $scope.tableFields.length; _i++) {
                if(_i < amount_field && $scope.tableFields[_i].display){
                    totaux++;
                }else if(_i> amount_field && $scope.tableFields[_i].display)  {
                    price++;
                }
            }
            return field == 'totaux' ? totaux : price;
        };
        $scope.isOperationsIsEmpty = false;

        $scope.selectOperationForOrder = async () =>{
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true' && !operation.id_instruction);
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.inProgressOrders = async (orders: OrderClient[]) => {
            let ordersToValidat = new OrdersClient();
            ordersToValidat.all = Mix.castArrayAs(OrderClient, orders);
            let {status, data} = await ordersToValidat.updateStatus('IN PROGRESS');
            openLightboxValidOrder(status, data, ordersToValidat);
            await $scope.syncOrders('WAITING');
            Utils.safeApply($scope);
        };

        $scope.orderShow = (order:OrderClient) => {
            if(order.rank !== undefined){
                if(order.campaign.priority_field === PRIORITY_FIELD.ORDER && order.campaign.orderPriorityEnable()){
                    return order.rank = order.rank + 1;
                }
            }
            return order.rank = lang.translate("crre.order.not.prioritized");
        };

        $scope.selectCampaignAndInitFilter = async (campaign: Campaign) =>{
            await $scope.selectCampaignShow(campaign);
            $scope.search.filterWords = [];
        };


        // Functions specific for baskets interactions

        $scope.displayedBasketsOrders = [];
        $scope.displayedOrdersRegionOrders = [];

        $scope.switchAllSublines = (basket) : void => {
            basket.orders.forEach(function (order) {
                order.selected = basket.selected;
            });
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

        $scope.cancelRefuseOrder = () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            Utils.safeApply($scope);
        };


        angular.element(document).ready(function(){
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements[0]) {
                elements[0].scrollLeft = 9000000000000;
            }
            Utils.safeApply($scope);
        });

        this.init();
    }]);