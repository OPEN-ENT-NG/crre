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
    Project,
    OrderRegion,
    OrderClient,
} from "../../model";
import {Mix} from "entcore-toolkit";
import {AxiosResponse} from "axios";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";

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
                $scope.projectFilter.page = 0;
                $scope.filtersFront.all = [];
                $scope.filters.all = []
                $scope.display.loading = true;
                $scope.display.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(null,false, null, $scope.filter.isOld);
            }
        };

        $scope.canResubmit = () : boolean => {
            return $scope.display.projects.all.flatMap((project : Project) => project.orders)
                .filter((order : OrderRegion) => order.selected && (!order.equipment || !order.equipment.valid || order.status == ORDER_STATUS_ENUM.ARCHIVED)).length == 0;
        }

        $scope.reSubmit = async () : Promise<void> => {
            let totalAmount : number = 0;
            let baskets : Baskets = new Baskets();
            let ordersToResubmit : OrdersClient = new OrdersClient();
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
                        let orderClient : OrderClient = new OrderClient();
                        orderClient.id = order.id_order_client_equipment;
                        ordersToResubmit.push(orderClient);
                    }
                });
            });

            new OrdersClient().resubmitOrderClient(baskets, totalAmount, $scope.current.structure)
                .then(() => ordersToResubmit.updateStatus(ORDER_STATUS_ENUM.RESUBMIT))
                .then((res: AxiosResponse) => {
                    if (res.status != 200) {
                        toasts.warning('crre.order.update.err');
                    }

                    $scope.display.projects.all.flatMap((project : Project) => project.orders.all)
                        .filter((order : OrderClient) => order.selected)
                        .forEach((order : OrderClient) => order.status = ORDER_STATUS_ENUM.RESUBMIT)

                    $scope.display.projects.all.forEach((project : Project) => Utils.setStatus(project, project.orders));

                    $scope.campaign.nb_order += 1;
                    $scope.campaign.order_notification += 1;
                    $scope.campaign.nb_order_waiting += baskets.all.length;
                    uncheckAll();
                })
                .catch(error => {
                    toasts.warning('crre.order.update.err');
                    console.error(error);
                    uncheckAll();
                })
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
            await $scope.searchProjectAndOrders($scope.filter.isOld, false, false);
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