import {ng, template, toasts} from 'entcore';
import {
    OrdersClient,
    Utils,
} from '../../model';

export const orderRefuseController = ng.controller('orderRefuseController',
    ['$scope', ($scope,) => {

        $scope.confirmRefuseOrder = async () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            let ordersToRefuse  = new OrdersClient();
            ordersToRefuse.all = $scope.ordersClient.selected;
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
                await $scope.getAllFilters();
                toasts.confirm('crre.order.refused.succes');
                Utils.safeApply($scope);
                $scope.allOrdersSelected = false;
                $scope.onScroll(true);
            } else {
                toasts.warning('crre.order.refused.error');
            }
        };

        $scope.cancelRefuseOrder = () => {
            $scope.display.lightbox.refuseOrder = false;
            template.close('refuseOrder.lightbox');
            Utils.safeApply($scope);
        };

    }]);