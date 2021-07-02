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

        $scope.filterByDate = async () => {
            if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                await $scope.searchByName(false);
            } else {
                toasts.warning('crre.date.err');
            }
        };

        $scope.licencesAvailable = () => {
            return $scope.campaign.nb_licences_available - $scope.displayedOrders.calculTotalAmount() < 0;
        };

        function updateOrders(totalPrice: number, totalAmount: number, ordersToRemove: OrdersClient) {
                $scope.campaign.purse_amount -= totalPrice;
                $scope.campaign.nb_licences_available -= totalAmount;
            if ($scope.ordersClient.selectedElements.length > 0) {
                $scope.campaign.nb_order_waiting -= $scope.ordersClient.selectedElements.length;
                $scope.campaign.historic_etab_notification += $scope.ordersClient.selectedElements.length;
            } else {
                $scope.campaign.nb_order_waiting -= $scope.ordersClient.all.length;
                $scope.campaign.historic_etab_notification += $scope.ordersClient.all.length;
            }
            ordersToRemove.all.forEach(order => {
                $scope.ordersClient.all.forEach((orderClient, i) => {
                    if (orderClient.id == order.id) {
                        $scope.ordersClient.all.splice(i, 1);
                    }
                });
            });
        }

        function reformatOrders(order, ordersToCreate: OrdersRegion, ordersToRemove: OrdersClient, totalPrice: number,
                                totalAmount: number) {
            let orderRegionTemp = new OrderRegion();
            orderRegionTemp.createFromOrderClient(order);
            ordersToCreate.all.push(orderRegionTemp);
            ordersToRemove.all.push(order);
            if(order.campaign.use_credit) {
                totalPrice += order.price * order.amount;
                totalAmount += order.amount;
            }
            return {totalPrice, totalAmount};
        }

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            let ordersToRemove = new OrdersClient();
            let totalPrice = 0;
            let totalAmount = 0;

            if($scope.ordersClient.selectedElements.length > 0) {
                $scope.ordersClient.selectedElements.forEach(order => {
                    const __ret = reformatOrders(order, ordersToCreate, ordersToRemove, totalPrice, totalAmount);
                    totalPrice = __ret.totalPrice;
                    totalAmount = __ret.totalAmount;
                });
            } else {
                $scope.ordersClient.all.forEach(order => {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    ordersToRemove.all.push(order);
                    if(order.campaign.use_credit) {
                        totalPrice += order.price * order.amount;
                        totalAmount += order.amount;
                    }

                });
            }
            ordersToCreate.create().then(async data =>{
                if (data.status === 201) {
                    toasts.confirm('crre.order.region.create.message');
                    updateOrders(totalPrice, totalAmount, ordersToRemove);
                    $scope.displayedOrders.all = $scope.ordersClient.all;
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
            $scope.displayedOrders.all.map((order) => order.selected = $scope.allOrdersSelected);
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
            Utils.safeApply($scope);
        };

        $scope.openLightboxRefuseOrder = () => {
                template.open('refuseOrder.lightbox', 'validator/order-refuse-confirmation');
                $scope.display.lightbox.refuseOrder = true;
        };

        $scope.exportCSV = () => {
            if($scope.ordersClient.selectedElements.length == 0) {
                $scope.ordersClient.exportCSV(false);
            } else {
                let selectedOrders = new OrdersClient();
                selectedOrders.all = $scope.ordersClient.selectedElements;
                selectedOrders.exportCSV(false);
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