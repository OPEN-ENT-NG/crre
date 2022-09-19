import {ng, template, toasts} from 'entcore';
import {
    OrderClient, OrderRegion,
    OrdersRegion, Project,
    Utils,
} from "../../../model";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', ($scope) => {
        $scope.confirmRefuseOrder= async (justification:string) : Promise<void> =>{
            $scope.display.loading = true;
            let selectedOrders : OrdersRegion = new OrdersRegion();
            $scope.projects.forEach((project: Project) => {
                project.orders.forEach( async (order: OrderRegion) => {
                    if(order.selected) {selectedOrders.all.push(order);}
                });
            });
            $scope.display.toggle = false;
            $scope.display.lightbox.waitingAdmin = false;
            template.close('lightbox.waitingAdmin');
            const projectsToShow : Array<Project> = $scope.projects.all;
            $scope.projects.all = [];
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus('REJECTED', justification);
            if(status == 200){
                projectsToShow.forEach((project: Project) => {
                    project.orders.forEach( async (order: OrderRegion) => {
                        if(order.selected) {
                           order.status = "REJECTED";
                           order.cause_status = justification;
                        }
                        order.selected = false;
                    });
                    project.selected = false;
                    Utils.setStatus(project, project.orders);
                });
                $scope.projects.all = projectsToShow;
                $scope.display.loading = false;
                toasts.confirm('crre.order.refused');
                Utils.safeApply($scope);
            } else {
                $scope.projects.all = projectsToShow;
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