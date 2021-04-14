import {ng, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion,
    Utils,
} from "../../../model";
import {Mix} from "entcore-toolkit";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', ($scope) => {
        $scope.confirmRefuseOrder= async (justification:string) =>{
            $scope.display.lightbox.waitingAdmin = false;
            template.close('lightbox.waitingAdmin');
            let selectedOrders = [];
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {
                        selectedOrders.push(order);
                    }
                });
            });
            let ordersToRefuse  = new OrdersRegion();
            ordersToRefuse.all = Mix.castArrayAs(OrderRegion, selectedOrders);
            let {status} = await ordersToRefuse.updateStatus('REJECTED', justification);
            if(status == 200){
                $scope.projects.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {
                            order.status="REJECTED";
                            order.selected = false;
                        }
                        project.selected =false;
                    });
                    Utils.setStatus(project, project.orders[0]);
                });
                toasts.confirm('crre.order.refused.succes');
                $scope.displayToggle = false;
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.refused.error');
            }
        };
    }
    ]);