import {_, moment, ng, template, toasts} from 'entcore';
import {OrderClient, OrdersClient, orderWaiting, Utils} from '../../model';


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

        $scope.tableFields = orderWaiting;

        const initBaskets = async () => {
            await $scope.basketsOrders.sync($scope.campaign.id);
            Utils.safeApply($scope);
        };
        initBaskets();

        $scope.exportCSV = () => {
            let idCampaign = $scope.ordersClient.all[0].id_campaign;
            let idStructure = $scope.ordersClient.all[0].id_structure;
            window.location = `/crre/orders/export/${idCampaign}/${idStructure}`;
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
                ($scope.campaign.purse_enabled) ? toasts.confirm('crre.orderEquipment.delete.confirm')
                    : toasts.confirm('crre.requestEquipment.delete.confirm');
            }
        };

        $scope.switchOrderClient = async (order: OrderClient, index: number, to: string) =>{
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

    }]);
