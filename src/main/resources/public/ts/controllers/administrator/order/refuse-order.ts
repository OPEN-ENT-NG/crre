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
            let selectedOrders = new OrdersRegion();
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {selectedOrders.all.push(order);}
                });
            });
            let {status} = await selectedOrders.updateStatus('REJECTED', justification);
            if(status == 200){
                $scope.projects.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) { order.status="REJECTED";}
                        order.selected = false;
                    });
                    project.selected =false;
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