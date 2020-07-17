import {_, moment, ng, template, toasts} from 'entcore';
import {Notification, OrderClient, OrdersClient, PRIORITY_FIELD, Project, Projects, Utils} from '../../model';


declare let window: any;

export const orderPersonnelController = ng.controller('orderPersonnelController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.display = {
            ordersClientOption: [],
            lightbox: {
                deleteOrder: false,
                deleteProject: false,
                udpateProject: false,
            },
            list: $scope.campaign.priority_field
        };
        $scope.menu = [{
            name:'lystore.by.project',
            value:PRIORITY_FIELD.PROJECT
        },{
            name:'lystore.by.equipment',
            value:PRIORITY_FIELD.ORDER
        }];

        $scope.exportCSV = () => {
            let idCampaign = $scope.ordersClient.all[0].id_campaign;
            let idStructure = $scope.ordersClient.all[0].id_structure;
            window.location = `/lystore/orders/export/${idCampaign}/${idStructure}`;
        };

        $scope.hasAProposalPrice = (orderClient: OrderClient) => {

            return (orderClient.price_proposal);
        };

        $scope.displayEquipmentOption = (index: number) => {
            $scope.display.ordersClientOption[index] = !$scope.display.ordersClientOption[index];
            Utils.safeApply($scope);
        };

        $scope.calculateDelivreryDate = (date: Date) => {
            return moment(date).add(60, 'days').calendar();
        };

        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, true, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.updateComment = (orderClient: OrderClient) => {
            if (!orderClient.comment || orderClient.comment.trim() == " ") {
                orderClient.comment = "";

            }
            orderClient.updateComment();
        };


        $scope.displayLightboxDelete = (orderEquipments: OrdersClient) => {
            template.open('orderClient.delete', 'customer/campaign/order/delete-confirmation');
            $scope.ordersEquipmentToDelete = orderEquipments;
            $scope.display.lightbox.deleteOrder = true;
            Utils.safeApply($scope);
        };
        $scope.cancelOrderEquipmentDelete = () => {
            delete $scope.orderEquipmentToDelete;
            $scope.display.lightbox.deleteOrder = false;
            template.close('orderClient.delete');

            Utils.safeApply($scope);
        };


        $scope.deleteOrdersEquipment = async (ordersEquipment: OrdersClient) => {
            for (let i = 0; i < ordersEquipment.length; i++) {
                await $scope.deleteOrderEquipment(ordersEquipment[i]);
            }
            $scope.cancelOrderEquipmentDelete();
            await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id);
            Utils.safeApply($scope);
        };

        $scope.deleteOrderEquipment = async (orderEquipmentToDelete: OrderClient) => {
            let {status, data} = await orderEquipmentToDelete.delete();
            if (status === 200) {
                $scope.campaign.nb_order = data.nb_order;
                $scope.campaign.purse_amount = data.amount;
                ($scope.campaign.purse_enabled) ? toasts.confirm('lystore.orderEquipment.delete.confirm')
                    : toasts.confirm('lystore.requestEquipment.delete.confirm');
            }
        };

        $scope.projectHasBuilding = (project: Project) => {
            if (project)
                return (project.building);
            else
                return false;
        };

        $scope.projectHasRoom = (project: Project) => {
            if (project)
                return (project.room);
            else
                return false;
        };

        $scope.projectHasStair = (project: Project) => {
            if (project)
                return (project.stair);
            else
                return false;
        };

        $scope.projectHasDescription = (project: Project) => {
            if (project)
                return (project.description);
            else
                return false;
        };

        $scope.projectHasSite = (project: Project) => {
            if (project)
                return (project.site);
            else
                return false;
        };


        $scope.displayLightboxProjectDelete = (project: Project) => {
            template.open('orderClient.deleteProject', 'customer/campaign/order/delete-project-confirmation');
            $scope.projectToDelete = project;
            $scope.display.lightbox.deleteProject = true;
            Utils.safeApply($scope);
        };

        $scope.cancelProjectDelete = () => {
            delete $scope.projectsToDelete;
            $scope.display.lightbox.deleteProject = false;
            template.close('orderClient.deleteProject');
            Utils.safeApply($scope);
        };

        $scope.deleteProject = async (projects: Projects) => {
            for (let i = 0; i < projects.length; i++) {
                if ($scope.projectIsDeletable(projects[i])) {
                    let {status, data} = await projects[i].delete($scope.campaign.id, $scope.ordersClient.all[0].id_structure);
                    if (status == 200) {
                        toasts.confirm('lystore.project.delete.confirm');
                    }
                    if (data) {
                        $scope.campaign.nb_order = data.nb_order;
                        $scope.campaign.nb_panier = data.nb_basket;
                        $scope.campaign.purse_amount = data.purse_amount
                    }
                }
            }
            await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id);
            $scope.display.lightbox.deleteProject = false;
            template.close('orderClient.deleteProject');
            Utils.safeApply($scope);

        };

        $scope.openProjectForm = (project: Project) => {
            $scope.display.lightbox.udpateProject = true;
            $scope.projectToUpdate = project;
            template.open('orderClient.updateProject', 'customer/campaign/order/update-project-confirmation');
        };
        $scope.cancelProjectUpdate = () => {
            delete $scope.projectToUpdate;
            $scope.display.lightbox.udpateProject = false;
            template.close('orderClient.updateProject');
            Utils.safeApply($scope);
        };

        $scope.openProjectsDeletion = (projects: Projects) => {
            $scope.projectsToDelete = projects;
            $scope.display.lightbox.deleteProject = true;
            template.open('orderClient.deleteProject', 'customer/campaign/order/delete-project-confirmation');
        };

        $scope.updateProject = async () => {
            await $scope.projectToUpdate.update($scope.campaign.id);
            $scope.projectToUpdate.selected = false;
            delete $scope.projectToUpdate;
            $scope.display.lightbox.udpateProject = false;
            template.close('orderClient.updateProject');
            toasts.confirm('lystore.project.update.success');
            Utils.safeApply($scope);
        };

        $scope.alltheProjectsDeletable = (projects: Projects) => {
            let oneDeletable = true;
            for (let i = 0; i < projects.length; i++) {
                oneDeletable = oneDeletable && $scope.projectIsDeletable(projects[i]);
            }
            return oneDeletable;
        };

        $scope.projectIsDeletable = (project: Project) => {
            let isDeletable = true;
            let orderTemp;
            for (let i = 0; i < $scope.ordersClient.all.length; i++) {
                orderTemp = $scope.ordersClient.all[i];
                if (orderTemp.project.id == project.id && orderTemp.status !== "WAITING") {
                    isDeletable = false;
                }
            }
            return isDeletable;
        };

        $scope.switchOrderClient = async (order: OrderClient, index: number, projectId: number, to: string) =>{
            let ordersJson = await $scope.getOrdersRanksSwitchedToJson( index, to);
            await $scope.ordersClient.updateOrderRanks(ordersJson,order.id_structure, order.id_campaign);
            $scope.ordersClient.all = _.sortBy($scope.ordersClient.all, (order)=> order.rank != null ? order.rank : $scope.ordersClient.all.length );
            Utils.safeApply($scope);
        };

        $scope.getOrdersRanksSwitchedToJson = (index:number, to:string )=>{
            let rang = to == 'up'? -1 : +1;
            $scope.ordersClient.all[index].rank = index + rang;
            $scope.ordersClient.all[index + rang].rank = $scope.ordersClient.all[index].rank - rang ;
            return [{
                id:  $scope.ordersClient.all[index].id,
                rank: $scope.ordersClient.all[index].rank
            },{
                id: $scope.ordersClient.all[index + rang].id,
                rank: $scope.ordersClient.all[index + rang].rank
            }]
        };

        $scope.switchProjectClient = async (index: number, to: string) =>{
            let projectOrderJson = await $scope.getProjectRanksSwitchedToJson(index, to);
            await $scope.ordersClient.updateReference(projectOrderJson, $scope.ordersClient.all[0].id_campaign,
                $scope.ordersClient.projects.all[index].id, $scope.ordersClient.all[0].id_structure);
            $scope.ordersClient.projects.all = _.sortBy($scope.ordersClient.projects.all, (project)=> project.preference != null
                ? project.preference
                : $scope.ordersClient.projects.all.length );
            Utils.safeApply($scope);
        };

        $scope.getProjectRanksSwitchedToJson = (index:number, to:string )=>{
            let rang = to == 'up'? -1 : +1;
            $scope.ordersClient.projects.all[index].preference = index + rang;
            $scope.ordersClient.projects.all[index + rang].preference = $scope.ordersClient.projects.all[index].preference - rang ;
            return [{
                id:  $scope.ordersClient.projects.all[index].id,
                preference: $scope.ordersClient.projects.all[index].preference
            },{
                id:  $scope.ordersClient.projects.all[index + rang].id,
                preference: $scope.ordersClient.projects.all[index + rang].preference
            }]
        };

        $scope.switchView = (display)=> {
            if(display == PRIORITY_FIELD.ORDER){
                template.open('order-list', 'customer/campaign/order/orders-by-equipment');
            }else{
                template.open('order-list', 'customer/campaign/order/orders-by-project');
            }
            Utils.safeApply($scope);
        }

    }]);
