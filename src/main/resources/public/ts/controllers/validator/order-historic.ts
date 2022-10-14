import {ng, toasts} from 'entcore';
import {
    Utils,
    Basket,
    Equipment,
    FilterFront,
    Filter,
    Baskets,
    Campaign,
    OrdersClient,
    OrdersRegion,
    Project,
    OrderRegion,
    OrderClient,
} from "../../model";
import {Mix} from "entcore-toolkit";

export const historicOrderRegionController = ng.controller('historicOrderRegionController',
    ['$scope', async ($scope) => {
        $scope.filter = {
            isOld: false
        };
        $scope.filterChoice = {
            renew: []
        };
        $scope.filterChoiceCorrelation = {
            keys: ["renew"],
            renew: 'renew'
        };
        $scope.renews = [{name: 'true'}, {name: 'false'}];
        $scope.renews.forEach((item : {name : string}) => item.toString = () => $scope.translate(item.name));


        $scope.changeOld = async (old: boolean) : Promise<void> => {
            if ($scope.filter.isOld !== old) {
                $scope.filter.isOld = old;
                $scope.filter.page = 0;
                $scope.filtersFront.all = [];
                $scope.filters.all = []
                $scope.display.loading = true;
                $scope.display.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(null,false, null, $scope.filter.isOld, $scope.filter.page);
            }
        };

        $scope.reSubmit = async () : Promise<void> => {
            let totalAmount : number = 0;
            let baskets : Baskets = new Baskets();
            let ordersRegionToResubmit : OrdersRegion = new OrdersRegion();
            $scope.display.projects.forEach((project : Project) => {
                project.orders.forEach(async (order : OrderRegion) => {
                    if (order.selected) {
                        const campaign : Campaign = Mix.castAs(Campaign, JSON.parse(order.campaign.toString()));
                        let equipment : Equipment = new Equipment();
                        equipment.ean = order.equipment_key.toString();
                        let basket : Basket = new Basket(equipment, campaign.id, $scope.current.structure.id);
                        basket.amount = order.amount;
                        basket.selected = true;
                        totalAmount += order.amount;
                        baskets.push(basket);
                        ordersRegionToResubmit.push(order);
                    }
                });
            });

            await new OrdersClient().resubmitOrderClient(baskets, totalAmount, $scope.current.structure);

            let {status} = await ordersRegionToResubmit.updateStatus('RESUBMIT');
            if (status != 200) {
                toasts.warning('crre.order.update.err');
            }

            $scope.display.projects.forEach((project : Project) => {
                project.orders.forEach(async (order : OrderClient) => {
                    if (order.selected) {
                        order.status = 'RESUBMIT';
                    }
                });
                Utils.setStatus(project, project.orders);

            });

            $scope.campaign.nb_order += 1;
            $scope.campaign.order_notification += 1;
            $scope.campaign.nb_order_waiting += baskets.all.length;
            uncheckAll();
        };

        $scope.getFilter = async () : Promise<void> => {
            $scope.filters.all = [];
            $scope.filtersFront.all = [];
            for (const key of Object.keys($scope.filterChoice)) {
                let newFilterFront : FilterFront = new FilterFront();
                newFilterFront.name = $scope.filterChoiceCorrelation[key];
                newFilterFront.value = [];
                $scope.filterChoice[key].forEach(item => {
                    let newFilter : Filter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    newFilter.value = value;
                    newFilterFront.value.push(value);
                    $scope.filters.all.push(newFilter);
                });
                $scope.filtersFront.all.push(newFilterFront);
            }
            if ($scope.filters.all.length > 0) {
                await $scope.searchProjectAndOrders($scope.filter.isOld, false, false);
            } else {
                await $scope.searchByName($scope.query_name, $scope.filter.isOld);
            }
        };

        const uncheckAll = () : void => {
            $scope.display.projects.forEach((project : Project) => {
                project.selected = false;
                project.orders.forEach(async (order : OrderClient) => {
                    order.selected = false;
                });
            });
            $scope.display.toggle = false;
            Utils.safeApply($scope);
        };
    }
    ]);