import {ng, toasts} from 'entcore';
import {
    Basket,
    Baskets,
    Campaign,
    Equipment,
    OrderClient,
    OrderRegion,
    OrdersClient,
    Project,
    Utils,
} from "../../model";
import {Mix} from "entcore-toolkit";
import {AxiosResponse} from "axios";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";
import {StatusFilter} from "../../model/StatusFilter";

export const historicOrderRegionController = ng.controller('historicOrderRegionController',
    ['$scope', async ($scope) => {
        $scope.statusFilterValue = [new StatusFilter(ORDER_STATUS_ENUM.VALID), new StatusFilter(ORDER_STATUS_ENUM.IN_PROGRESS),
            new StatusFilter(ORDER_STATUS_ENUM.REJECTED), new StatusFilter(ORDER_STATUS_ENUM.SENT),
            new StatusFilter(ORDER_STATUS_ENUM.DONE), new StatusFilter(ORDER_STATUS_ENUM.ARCHIVED)];

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

                    $scope.display.projects.all.flatMap((project : Project) => project.orders)
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
            await $scope.searchProjectAndOrders(null, false, false);
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