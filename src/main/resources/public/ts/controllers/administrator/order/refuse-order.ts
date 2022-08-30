import {ng, template, toasts} from 'entcore';
import {
    OrdersRegion,
    Utils,
} from "../../../model";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', ($scope) => {
        $scope.confirmRefuseOrder= async (justification:string) =>{
            $scope.display.loading = true;
            let selectedOrders = new OrdersRegion();
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {selectedOrders.all.push(order);}
                });
            });
            $scope.display.toggle = false;
            $scope.display.lightbox.waitingAdmin = false;
            template.close('lightbox.waitingAdmin');
            const projectsToShow = $scope.projects;
            $scope.projects = [];
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus('REJECTED', justification);
            if(status == 200){
                projectsToShow.all.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {
                           order.status="REJECTED";
                           order.cause_status = justification;
                        }
                        order.selected = false;
                    });
                    project.selected = false;
                    Utils.setStatus(project, project.orders[0]);
                });
                $scope.projects = projectsToShow;
                $scope.display.loading = false;
                toasts.confirm('crre.order.refused');
                Utils.safeApply($scope);
            } else {
                $scope.projects = projectsToShow;
                $scope.display.loading = false;
                Utils.safeApply($scope);
                if (status == 401){
                    toasts.warning('crre.order.error.purse');
                } else {
                    toasts.warning('crre.order.refused.error');
                }
            }
        };
    }
    ]);