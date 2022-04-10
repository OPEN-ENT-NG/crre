import {_, moment, ng, template, toasts} from 'entcore';
import {
    OrderClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    Utils,
    Filter,
    Filters
} from '../../model';
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";

export const waitingValidatorOrderController = ng.controller('waitingValidatorOrderController',
    ['$scope', ($scope,) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.allOrdersSelected = false;
        $scope.show = {
            comment:false
        };
        $scope.projects = [];
        $scope.sort = {
            order : {
                type: 'created',
                reverse: false
            }
        }
        $scope.filter = {
            page: 0
        };
        $scope.displayedBasketsOrders = [];
        // @ts-ignore
        this.init = async () => {
            $scope.filterChoice = {
                users : [],
                reassorts : [],
                type_campaign: []
            }
            $scope.filterChoiceCorrelation = {
                keys : ["users", "reassorts", "type_campaign"],
                users : 'id_user',
                reassorts : 'reassort',
                type_campaign : 'id_campaign'
            }
            $scope.users = [];
            $scope.type_campaign = [];

            $scope.filtersDate = [];
            $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
            $scope.filtersDate.endDate = moment()._d;

            $scope.reassorts = [{name: 'true'}, {name: 'false'}];
            $scope.reassorts.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.filters = new Filters();
            await $scope.getAllFilters();
            $scope.users.forEach((item) => item.toString = () => item.user_name);
            let distinctOrdersCampaign = $scope.ordersClient.all.reduce((acc, x) =>
                    acc.concat(acc.find(y => y.campaign.id === x.campaign.id) ? [] : [x])
                , []);
            distinctOrdersCampaign.forEach(order => {
                $scope.type_campaign.push(order.campaign);
            });
            $scope.type_campaign.forEach((item) => item.toString = () => item.name);
            await $scope.getAllAmount();
            Utils.safeApply($scope);
        };

        $scope.dropElement = (item,key): void => {
            $scope.filterChoice[key] = _.without($scope.filterChoice[key], item);
            $scope.getFilter();
        };

        $scope.onScroll = async (init?:boolean): Promise<void> => {
            if(init){
                await $scope.searchByName(false)
            }else{
                $scope.filter.page++;
                await $scope.searchByName(true);
            }
        };

        $scope.getFilter = async () => {
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.ordersClient = new OrdersClient();
            Utils.safeApply($scope);
            $scope.filters = new Filters();
            for (const key of Object.keys($scope.filterChoice)) {
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    if(key === "users"){
                        value = item.id_user;
                    }
                    if(key === "type_campaign"){
                        value = item.id;
                    }
                    newFilter.value = value;
                    $scope.filters.all.push(newFilter);
                });
            }
            if($scope.filters.all.length > 0) {
                const newData = await $scope.ordersClient.filter_order($scope.filters.all, null,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.query_name);
                endLoading(newData);
            } else {
                if (!!$scope.query_name) {
                    const newData = await $scope.ordersClient.search($scope.query_name, null, $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $scope.filter.page );
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.sync('WAITING', $scope.filtersDate.startDate,
                        $scope.filtersDate.endDate, $scope.structures.all, null, null, null, $scope.filter.page);
                    endLoading(newData);
                }
            }
        };

        $scope.getAllFilters = async () => {
            $scope.users = await $scope.ordersClient.getUsers('WAITING');
        };

        $scope.getAllAmount = async () => {
            $scope.amountTotal = await $scope.ordersClient.calculTotal('WAITING', $scope.filtersDate.startDate, $scope.filtersDate.endDate);
        }

        $scope.filterByDate = async () => {
            if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                await $scope.searchByName(false);
            } else {
                toasts.warning('crre.date.err');
            }
        };

        $scope.remainAvailable = () => {
            let nbLicences = $scope.campaign.nb_licences_available ? $scope.campaign.nb_licences_available : 0;
            nbLicences += $scope.campaign.nb_licences_consumable_available ? $scope.campaign.nb_licences_consumable_available : 0

            let purseAmount = $scope.campaign.purse_amount ? $scope.campaign.purse_amount : 0

            return $scope.ordersClient.all.length == 0 || nbLicences - $scope.ordersClient.calculTotalAmount() < 0 ||
                purseAmount - $scope.ordersClient.calculTotalPriceTTC() < 0;
        };

        function updateOrders(totalPrice: number, totalAmount: number, totalAmountConsumable: number, ordersToRemove: OrdersClient, numberOrdered : number) {
            $scope.campaign.nb_licences_available -= totalAmount;
            $scope.campaign.nb_licences_consumable_available -= totalAmountConsumable;
            $scope.campaign.purse_amount -= totalPrice;

            $scope.campaign.nb_order_waiting -= numberOrdered;
            $scope.campaign.historic_etab_notification += numberOrdered;

            ordersToRemove.all.forEach(order => {
                $scope.ordersClient.all.forEach((orderClient, i) => {
                    if (orderClient.id == order.id) {
                        $scope.ordersClient.all.splice(i, 1);
                    }
                });
            });
        }

        function reformatOrders(order, ordersToCreate: OrdersRegion, ordersToRemove: OrdersClient, totalPrice: number,
                                totalAmount: number, totalAmountConsumable: number) {
            let orderRegionTemp = new OrderRegion();
            orderRegionTemp.createFromOrderClient(order);
            ordersToCreate.all.push(orderRegionTemp);
            ordersToRemove.all.push(order);
            if(order.campaign.use_credit == "credits") {
                totalPrice += order.price * order.amount;
            }else if(order.campaign.use_credit == "licences") {
                totalAmount += order.amount;
            }else if(order.campaign.use_credit == "consumable_licences") {
                totalAmountConsumable += order.amount;
            }
        }

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            let ordersToRemove = new OrdersClient();
            let totalPrice = 0;
            let totalAmount = 0;
            let totalAmountConsumable = 0;
            let ordersToReformat;

            if($scope.allOrdersSelected) {
                await $scope.searchByName(false, true);
                ordersToReformat = $scope.ordersClient.all;
            } else {
                ordersToReformat = ($scope.ordersClient.selectedElements.length > 0) ? $scope.ordersClient.selectedElements : $scope.ordersClient.all;
            }
            ordersToReformat.forEach(order => {
                reformatOrders(order, ordersToCreate, ordersToRemove, totalPrice, totalAmount, totalAmountConsumable);
            });

            ordersToCreate.create().then(async data =>{
                if (data.status === 201) {
                    toasts.confirm('crre.order.region.create.message');
                    updateOrders(totalPrice, totalAmount, totalAmountConsumable, ordersToRemove, ordersToReformat.length);
                    await $scope.getAllFilters();
                    Utils.safeApply($scope);
                    $scope.allOrdersSelected = false;
                    $scope.onScroll(true);
                }
                else {
                    toasts.warning('crre.admin.order.create.err');
                }
            })
        };

        $scope.switchAllOrders = () => {
            $scope.ordersClient.all.map((order) => order.selected = $scope.allOrdersSelected);
        };

        $scope.checkSwitchAll = (): void => {
            let testAllTrue = true;
            $scope.ordersClient.all.forEach(function (order) {
                if (!order.selected) {
                    testAllTrue = false;
                }
            });
            $scope.allOrdersSelected = testAllTrue;
            Utils.safeApply($scope);
        };

        function endLoading(newData: any) {
            if (newData)
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        $scope.searchByName =  async (noInit?:boolean, all?: boolean) => {
            if(!noInit){
                $scope.loading = true;
                $scope.filter.page = 0;
                $scope.ordersClient = new OrdersClient();
                Utils.safeApply($scope);
            }
            all ? $scope.filter.page = null : 0;
            if($scope.filters.all.length == 0) {
                if ($scope.query_name && $scope.query_name != "") {
                    const newData = await $scope.ordersClient.search($scope.query_name, null,
                        $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page);
                    endLoading(newData);
                } else {
                    const newData = await $scope.ordersClient.sync('WAITING', $scope.filtersDate.startDate, $scope.filtersDate.endDate,
                        $scope.structures.all, null, null, null, $scope.filter.page);
                    endLoading(newData);
                }
            }else{
                const newData = await $scope.ordersClient.filter_order($scope.filters.all, null, $scope.filtersDate.startDate,
                    $scope.filtersDate.endDate, $scope.query_name, $scope.filter.page);
                endLoading(newData);
            }
            $scope.syncSelected();
            Utils.safeApply($scope);
        };

        $scope.syncSelected = () : void => {
            $scope.ordersClient.all.forEach(order => order.selected = $scope.allOrdersSelected)
        };

        $scope.openLightboxRefuseOrder = () => {
            template.open('refuseOrder.lightbox', 'validator/order-refuse-confirmation');
            $scope.display.lightbox.refuseOrder = true;
        };

        $scope.exportCSV = () => {
            let selectedOrders = new OrdersClient();
            if($scope.ordersClient.selectedElements.length == 0 || $scope.allOrdersSelected) {
                selectedOrders.exportCSV(false, null, $scope.filtersDate.startDate, $scope.filtersDate.endDate, true, "WAITING")
            } else {
                selectedOrders.all = $scope.ordersClient.selectedElements;
                selectedOrders.exportCSV(false, null, $scope.filtersDate.startDate, $scope.filtersDate.endDate, false, "WAITING");
            }
            $scope.ordersClient.forEach(function (order) {order.selected = false;});
            $scope.allOrdersSelected = false;
        }

        $scope.updateAmount = async (orderClient: OrderClient, amount: number) => {
            if(amount.toString() != 'undefined') {
                orderClient.amount = amount;
                await orderClient.updateAmount(amount);
                Utils.safeApply($scope);
            }
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            Utils.safeApply($scope);
        };

        // @ts-ignore
        this.init();
    }]);