import {$, _, angular, idiom as lang, model, moment, ng, notify, template, toasts} from 'entcore';
import http from "axios";
import {
    BasketOrder,
    Campaign, Equipment,
    OrderClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    orderWaiting,
    PRIORITY_FIELD,
    Utils
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
                            || ('contract_type' in order ? regex.test(order.contract_type.name.toLowerCase()) : false)
                            || regex.test('contract' in (order as OrderClient)
                                ? order.contract.name.toLowerCase()
                                : order.contract_name)
                            || regex.test('supplier' in (order as OrderClient)
                                ? order.supplier.name.toLowerCase()
                                : order.supplier_name)
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
                            || ('supplier_name' in order ?  regex.test(order.supplier_name.toLowerCase()) : false );
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
        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            if($scope.ordersClient.selectedElements.length > 0) {
                $scope.ordersClient.selectedElements.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                });
            } else {
                $scope.ordersClient.all.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                });
            }


            let {status} = await ordersToCreate.create();
            if (status === 201) {
                toasts.confirm('crre.order.region.create.message');
                $scope.orderToCreate = new OrderRegion();
            }
            else {
                notify.error('crre.admin.order.create.err');
            }
            Utils.safeApply($scope);
        }

        $scope.initPreferences = ()  => {
            if(isPageOrderWaiting)
                if ($scope.preferences && $scope.preferences.preference) {
                    let loadedPreferences = JSON.parse($scope.preferences.preference);
                    if(loadedPreferences.ordersWaitingDisplay)
                        $scope.tableFields.map(table => {
                            table.display = loadedPreferences.ordersWaitingDisplay[table.fieldName]
                        });
                    if(loadedPreferences.searchFields){
                        $scope.search.filterWords = loadedPreferences.searchFields;
                        $scope.filterDisplayedOrders();
                    }
                    $scope.ub.putPreferences("searchFields", []);
                }
        };

        $scope.initPreferences();
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


        $scope.savePreference = () =>{
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements[0])
                elements[0].scrollLeft = $(".vertical-array-scroll").scrollLeft() ;
            Utils.safeApply($scope);
            $scope.ub.putPreferences("ordersWaitingDisplay", $scope.jsonPref($scope.tableFields));
        };

        $scope.jsonPref = (prefs) => {
            let json = {};
            prefs.forEach(pref =>{
                json[pref.fieldName]= pref.display;
            });
            return json;
        };

        $scope.getTotal = () => {
            let total = 0;
            $scope.ordersClient.all.forEach(basket => {
             total += parseFloat(basket.total.replace(/[^0-9.,-]+/g,""));
            });
            return total;
        }



/*        $scope.addFilter = (filterWord: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addFilterWords(filterWord);
                $scope.filterDisplayedOrders();
            }
        };*/

        $scope.switchAllOrders = () => {
            $scope.displayedOrders.all.map((order) => order.selected = $scope.allOrdersSelected);
        };

        $scope.getSelectedOrders = () => $scope.displayedOrders.selected;

        $scope.getStructureGroupsList = (structureGroups: string[]): string => {
            return structureGroups.join(', ');
        };

        $scope.searchByName =  async (name: string) => {
            if(name != "") {
                await $scope.ordersClient.search(name, $scope.campaign.id);
                /*await http.get(`/crre/orderRegion/projects`, { params: {
                    storeIds: [1,2,3].join(',')
                }});*/
            } else {
                await $scope.ordersClient.sync('WAITING');
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
                template.open('validOrder.lightbox', 'administrator/order/order-valid-confirmation');
                $scope.display.lightbox.validOrder = true;
            }
        }

        function openLightboxRefuseOrder(status, data, ordersToValidat: OrdersClient) {
                template.open('refuseOrder.lightbox', 'administrator/order/order-refuse-confirmation');
                $scope.display.lightbox.refuseOrder = true;
        }


        $scope.refuseOrders = async () => {
            let ordersToRefuse  = new OrdersClient();
            ordersToRefuse.all = Mix.castArrayAs(OrderClient, $scope.ordersClient.selected);
            let { status, data } = await ordersToRefuse.updateStatus('REFUSED');
            openLightboxRefuseOrder(status, data, $scope.ordersClient.selected);
            $scope.getOrderWaitingFiltered($scope.campaign);
            await $scope.syncOrders('WAITING');
            Utils.safeApply($scope);
        };

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
            let indexToSplice = 0
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

        $scope.cancelRefusedOrder = () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            Utils.safeApply($scope);
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
            $scope.displayedOrders.all = [];
            await $scope.ordersClient.sync(status, $scope.structures.all);
            $scope.displayedOrders.all = $scope.ordersClient.all;
            $scope.displayedOrders.all.map(order => {
                    order.selected = false;
                }
            );

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
            return orderToSend && orderToSend.supplier && orderToSend.bc_number && orderToSend.engagement_number
                && orderToSend.bc_number.trim() !== '' && orderToSend.engagement_number.trim() !== ''
                && orderToSend.id_program !== undefined;
        };
        $scope.sendOrders = async (orders: OrdersClient) => {
            let { status, data } = await orders.updateStatus('SENT');
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
        $scope.exportCSV = async() => {
            let params = Utils.formatKeyToParameter($scope.ordersClient.selected, 'id');
            window.location = `/crre/orders/export?${params}`;
        };


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

        $scope.exportOrder = async (orders: OrderClient[]) => {
            if (isSentOrDone(orders)) {
                let orderNumber = _.uniq(_.pluck(orders, 'order_number'));
                let  {status, data} =  await http.get(`/crre/order?bc_number=${orderNumber}`);
                if(status === 201){
                    toasts.info('crre.sent.export.BC');
                }
            } else {
                $scope.exportValidOrders(orders, 'order');
            }
            $scope.displayedOrders.selected.map(order => {
                order.selected = false;
            });
            Utils.safeApply($scope);
        };

/*        $scope.getOrdersByProject = async(id: number) => {
            try {
                let { data } = await http.get(`/crre/orderRegion/orders/${id}`);
                let orders = Mix.castArrayAs(OrderRegion, data)
                for (let order of orders) {
                    let equipment = new Equipment();
                    await equipment.sync(order.equipment_key);
                    order.price = (equipment.price * (1 + equipment.tax_amount / 100)) * order.amount;
                    order.name = equipment.name;
                    order.image = equipment.image;
                }
                return orders;
            } catch (e) {
                notify.error('crre.basket.sync.err');
            }
        }

        $scope.getProjects = async() => {
            try {
                let { data } = await http.get(`/crre/orderRegion/projects`);
                $scope.projects = data;
            } catch (e) {
                notify.error('crre.basket.sync.err');
            }
        }*/

        $scope.updateAmount = async (orderClient: OrderClient, amount: number) => {
            await orderClient.updateAmount(amount);
            let basket = new BasketOrder();
            basket.setIdBasket(orderClient.id_basket);
            await basket.updateAllAmount();
            $scope.$apply()
        };

        $scope.exportOrderStruct = async (orders: OrderClient[]) => {
            if (isSentOrDone(orders)) {
                let orderNumber = _.uniq(_.pluck(orders, 'order_number'));
                let  {status, data} =  await http.get(`/crre/order/struct?bc_number=${orderNumber}`);
                if(status === 201){
                    toasts.info('crre.sent.export.BC');
                }
            } else {
                let filter = "";
                orders.forEach(order => {
                    filter +="number_validation=" +  order.number_validation + "&";
                });
                filter = filter.substring(filter.length-1,0);
                let  {status, data} = await http.get(`/crre/order/struct?${filter}`);
                if(status === 201){
                    toasts.info('crre.sent.export.BC');
                }
            }
            $scope.displayedOrders.selected.map(order => {
                order.selected = false;
            });
            Utils.safeApply($scope);
        };

        $scope.exportValidOrders = async  (orders: OrderClient[], fileType: string) => {
            let params = '';
            orders.map((order: OrderClient) => {
                params += `number_validation=${order.number_validation}&`;
            });
            params = params.slice(0, -1);
            if(fileType ==='structure_list'){
                toasts.info('crre.sent.export.BC');
                await orders[0].exportListLycee(params);
                $scope.displayedOrders.selected[0].selected = false;
                Utils.safeApply($scope);
            }else
            if(fileType === 'certificates'){
                window.location = `/crre/orders/valid/export/${fileType}?${params}`;
            }
            else{
                let  {status, data} = await http.get(`/crre/orders/valid/export/${fileType}?${params}`);
                if(status === 201){
                    toasts.info('crre.sent.export.BC');
                }
            }
        };

        $scope.cancelValidation = async (orders: OrderClient[]) => {
            try {
                await $scope.displayedOrders.cancel(orders);
                await $scope.syncOrders('VALID');
                toasts.confirm('crre.orders.valid.cancel.confirmation');
            } catch (e) {
                toasts.warning('crre.orders.valid.cancel.error');
            } finally {
                Utils.safeApply($scope);
            }
        };

        $scope.getProgramName = (idProgram: number) => idProgram !== undefined
            ? _.findWhere($scope.programs.all, { id: idProgram }).name
            : '';

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
        $scope.updateOrder = (order: OrderClient) => {
            $scope.ub.putPreferences("searchFields", $scope.search.filterWords);
            $scope.redirectTo(`/order/update/${order.id}`);
        };
        $scope.selectCampaignAndInitFilter = async (campaign: Campaign) =>{
            await $scope.selectCampaignShow(campaign);
            $scope.search.filterWords = [];
        };


        // Functions specific for baskets interactions

        $scope.displayedBasketsOrders = [];
        $scope.displayedOrdersRegionOrders = [];
        const currencyFormatter = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

        const synchroBaskets = async () : Promise<void> => {
            await $scope.basketsOrders.sync($scope.campaign.id);
            //await $scope.displayedOrdersRegion.sync();

            formatDisplayedBasketOrders();
            Utils.safeApply($scope);
        };
        synchroBaskets();

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
                $scope.displayedBasketsOrders.push(displayedBasket);
            });
        };

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



        angular.element(document).ready(function(){
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements[0]) {
                elements[0].scrollLeft = 9000000000000;
            }
            Utils.safeApply($scope);
        });
    }]);