import {ng, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion, Project, Projects,
    Utils,
} from "../../../model";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', '$timeout', ($scope, $timeout) => {
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
            let projectsToShow : Projects = $scope.projects;
            $scope.projects = new Projects();
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus('REJECTED', justification);
            if(status == 200){
                projectsToShow.all.forEach((project: Project) => {
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
                toasts.confirm('crre.order.refused');
                $scope.projects = projectsToShow;
                $scope.display.loading = false;
                $timeout(function() {
                    Utils.safeApply($scope);
                }, 500)
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