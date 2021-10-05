import {ng, template, toasts} from 'entcore';
import {
    OrdersRegion,
    Utils,
} from "../../../model";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', ($scope) => {
        $scope.confirmRefuseOrder= async (justification:string) =>{
            let selectedOrders = new OrdersRegion();
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {selectedOrders.all.push(order);}
                });
            });
            let {status} = await selectedOrders.updateStatus('REJECTED', justification);
            if(status == 200){
                $scope.projects.all.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {
                           order.status="REJECTED";
                           order.cause_status = justification;
                        }
                        order.selected = false;
                    });
                    project.selected =false;
                    Utils.setStatus(project, project.orders[0]);
                });
                toasts.confirm('crre.order.refused');
                $scope.display.toggle = false;
                $scope.display.lightbox.waitingAdmin = false;
                template.close('lightbox.waitingAdmin');
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.refused.error');
            }
        };
    }
    ]);