import {ng, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion, Project, Projects,
    Utils,
} from "../../../model";
import {ORDER_STATUS_ENUM} from "../../../enum/order-status-enum";

export const refuseOrderRegionController = ng.controller('refuseOrderRegionController',
    ['$scope', '$timeout', ($scope, $timeout) => {
        $scope.confirmRefuseOrder= async (justification:string) : Promise<void> =>{
            $scope.display.loading = true;
            let selectedOrders : OrdersRegion = new OrdersRegion();
            $scope.display.projects.forEach((project: Project) => {
                project.orders.forEach( async (order: OrderRegion) => {
                    if(order.selected &&
                        order.status != ORDER_STATUS_ENUM.SENT && order.status != ORDER_STATUS_ENUM.DONE &&
                        order.status != ORDER_STATUS_ENUM.REJECTED) {
                        selectedOrders.all.push(order);
                    }
                });
            });
            $scope.display.toggle = false;
            $scope.display.lightbox.waitingAdmin = false;
            template.close('lightbox.waitingAdmin');
            let projectsToShow : Projects = $scope.display.projects;
            $scope.display.projects = new Projects();
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus(ORDER_STATUS_ENUM.REJECTED, justification);
            if(status == 200){
                projectsToShow.all.forEach((project: Project) => {
                    project.orders.forEach( async (order: OrderRegion) => {
                        if(order.selected &&
                            order.status != ORDER_STATUS_ENUM.SENT && order.status != ORDER_STATUS_ENUM.DONE &&
                            order.status != ORDER_STATUS_ENUM.REJECTED) {
                            order.status = ORDER_STATUS_ENUM.REJECTED;
                            order.cause_status = justification;
                        }
                        order.selected = false;
                    });
                    project.selected = false;
                    Utils.setStatus(project, project.orders);
                });
                toasts.confirm('crre.order.refused');
                $scope.display.projects = projectsToShow;
                $scope.display.loading = $scope.display.allOrdersSelected = false;
                $timeout(function() {
                    Utils.safeApply($scope);
                }, 500)
            } else {
                $scope.display.projects = projectsToShow;
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