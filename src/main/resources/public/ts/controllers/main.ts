import {_,Behaviours, idiom as lang, model, moment, ng, template, toasts} from 'entcore';
import {
    Basket,
    Baskets,
    BasketsOrders,
    Campaign,
    Campaigns,
    Equipment,
    Equipments,
    Exports,
    Logs,
    OrderClient,
    OrderRegion,
    OrdersClient,
    StructureGroups,
    Structures,
    Utils,
    Filters,
    Student
} from '../model';
import {Mix} from "entcore-toolkit";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$rootScope',
    ($scope, route, $location, $rootScope) => {
        template.open('main', 'main');

        $scope.display = {
            equipment: false,
            lightbox: {lightBoxIsOpen: false,}
        };
        $scope.structures = new Structures();
        $scope.current = {};
        $scope.notifications = [];
        $scope.lang = lang;
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.orderClient = new OrderClient();
        $scope.orderRegion = new OrderRegion();
        $scope.displayedOrders = new OrdersClient();
        $scope.basketsOrders = new BasketsOrders();
        $scope.users = [];
        $scope.filters = new Filters();
        $scope.student = new Student();
        $scope.total_licence = 0;
        $scope.exports = new Exports([]);
        $scope.loadingArray = false;

        route({
            main: async () => {
                if ($scope.isAdministrator()) {
                    $scope.redirectTo("/order/waiting");
                }
                else if ($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor() && !$scope.isAdministrator()) {
                    $scope.redirectTo(`/equipments/catalog`);
                } else {
                    await $scope.initStructures();
                    await $scope.initCampaign($scope.current.structure);
                    template.open('main-profile', 'customer/campaign/campaign-list');
                }
                Utils.safeApply($scope);
            },
            viewLogs: async () => {
                $scope.loadingArray = true;
                $scope.logs.reset();
                template.open('administrator-main', 'administrator/log/view-logs');
                await $scope.logs.loadPage($scope.current.page);
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            manageCampaigns: async () => {
                template.open('administrator-main', 'administrator/campaign/campaign_container');
                template.open('campaigns-main', 'administrator/campaign/manage-campaign');
                await $scope.campaigns.sync();
                Utils.safeApply($scope);
            },
            createOrUpdateCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: async () => {
                template.open('administrator-main', 'administrator/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageStructureGroups: async () => {
                template.open('administrator-main', 'administrator/structureGroup/structureGroup-container');
                await $scope.structureGroups.sync();
                template.open('structureGroups-main', 'administrator/structureGroup/manage-structureGroup');
                $scope.structures = new Structures();
                await $scope.structures.sync();
                Utils.safeApply($scope);
            },
            createStructureGroup: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/structureGroups');
                }
                template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            showCatalog: async () => {
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                await $scope.selectCatalog();
                Utils.safeApply($scope);
            },
            showAdminCatalog: async () => {
                template.open('administrator-main', 'customer/campaign/catalog/catalog-list');
                await $scope.selectCatalog();
                Utils.safeApply($scope);
            },
            equipmentDetail: async (params) => {
                await $scope.selectEquipment(params);
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/equipment-detail');
                Utils.safeApply($scope);
            },
            adminEquipmentDetail: async (params) => {
                await $scope.selectEquipment(params);
                template.open('administrator-main', 'customer/campaign/catalog/equipment-detail');
                Utils.safeApply($scope);
            },
            campaignOrder: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures() ;
                $scope.initCampaignOrderView();
                await $scope.selectCampaign(idCampaign);
                $scope.campaign.order_notification = 0;
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-order');
                Utils.safeApply($scope);
            },
            campaignBasket: async (params) => {
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/basket/manage-basket');
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures() ;
                if($scope.current.structure) {
                    await $scope.baskets.sync(idCampaign, $scope.current.structure.id);
                }
                await $scope.selectCampaign(idCampaign);
                Utils.safeApply($scope);
            },
            orderWaiting: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures();
                await $scope.selectCampaign(idCampaign);
                let campaignPref;
                campaignPref = $scope.campaign;
                $scope.loading = true;
                Utils.safeApply($scope);
                if (campaignPref) {
                    await $scope.initOrders('WAITING');
                    $scope.selectCampaignShow(campaignPref, "WAITING");
                    $scope.loading = false;
                    Utils.safeApply($scope);
                } else {
                    await $scope.openLightSelectCampaign();
                    $scope.loading = false;
                    Utils.safeApply($scope);
                }
            },
            orderHistoric: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures();
                await $scope.selectCampaign(idCampaign);
                let campaignPref;
                campaignPref = $scope.campaign;
                if (campaignPref) {
                    $scope.selectCampaignShow(campaignPref, "HISTORIC");
                } else
                    await $scope.openLightSelectCampaign();
                $scope.campaign.historic_etab_notification = 0;
                Utils.safeApply($scope);
            },
            orderWaitingAdmin: async () => {
                $scope.displayedOrders.all = $scope.ordersClient.all;
                template.open('administrator-main', 'administrator/order/order-waiting');
                Utils.safeApply($scope);
            },
            orderHistoricAdmin: async () => {
                $scope.displayedOrders.all = $scope.ordersClient.all;
                template.open('administrator-main', 'administrator/order/order-sent-library');
                Utils.safeApply($scope);
            },
            exportList: async () => {
                $scope.loadingArray = true;
                await $scope.exports.getExports();
                template.open('administrator-main', 'administrator/exports/export-list');
                $scope.loadingArray = false;
                Utils.safeApply($scope);

            }
        });

        $scope.selectCatalog = async function (){
            $scope.fromCatalog=true
            $scope.display.equipment = false;
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments();
            Utils.safeApply($scope);
        }

        $scope.selectEquipment = async function (params){
            let idEquipment = params.idEquipment;
            $scope.idIsInteger(idEquipment);
            if($scope.campaign.id) {
                await $scope.initBasketItem(parseInt(idEquipment), $scope.campaign.id, $scope.current.structure.id);
            } else {
                await $scope.initBasketItem(parseInt(idEquipment));
            }
            window.scrollTo(0, 0);
            $scope.display.equipment = true;
        }

        $scope.selectCampaign = async function (idCampaign) {
            if (!$scope.campaign.id) {
                await $scope.campaigns.sync($scope.current.structure.id);
                $scope.campaigns.all.forEach(campaign => {
                    if (campaign.id == idCampaign) {
                        $scope.campaign = campaign;
                    }
                });
            }
        };

        $scope.initInstructions = async ()=>{
            $scope.loadingArray = true;
            await $scope.instructions.sync();
            $scope.loadingArray = false;
        };
        $scope.initCampaignOrderView=()=>{
            template.open('order-list', 'customer/campaign/order/orders-list');
        };

        $scope.initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere($scope.equipments.all, {id: idEquipment});
            if ($scope.equipment === undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                $scope.equipment.loading = true;
                Utils.safeApply($scope);
                await $scope.equipment.sync(idEquipment);
            }
            $scope.basket = new Basket($scope.equipment, idCampaign, structure);
            Utils.safeApply($scope);
        };

        $scope.idIsInteger = (id) => {
            try {
                id = parseInt(id);
                if (isNaN(id)) {
                    $scope.redirectTo(`/`);
                    Utils.safeApply($scope);
                }
            } catch (e) {
                $scope.redirectTo(`/`);
                Utils.safeApply($scope);
            }
        };

        $scope.hasAccess = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.access);
        };

        $scope.isValidator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.validator);
        };

        $scope.isAdministrator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator);
        };

        $scope.isPrescriptor = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor);
        };

        $scope.canUpdateReassort = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.reassort);
        };

        $scope.redirectTo = (path: string) => {
            $scope.selectedType = path;
            $location.path(path);
        };

        $rootScope.$on('eventEmitedCampaign', function (event, data) {
            $scope.campaign = data;
        });

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {boolean} selectedOptions [Consider selected options or not)
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {string | number}
         */
        $scope.calculatePriceOfEquipment = (equipment: any, roundNumber?: number) => {
            let price = Utils.calculatePriceTTC(equipment, roundNumber);
            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price) : price;
        };

        $scope.openEquipment = (equipment: Equipment) => {
            $scope.redirectTo(`/equipments/catalog/equipment/${equipment.id}`);
        };

        $scope.openEquipmentId = (equipmentId: string) => {
            $scope.redirectTo(`/equipments/catalog/equipment/${equipmentId}`);
        };

        $scope.initStructures = async () => {
            await $scope.structures.syncUserStructures();
            $scope.current.structure = $scope.structures.all[0];
        };

        $scope.avoidDecimals = (event) => {
            return event.charCode >= 48 && event.charCode <= 57;
        };

        $scope.notifyBasket = (action: String, basket: Basket) => {
            let messageForOne = basket.amount + ' ' + lang.translate('article') + ' "'
                + basket.equipment.titre + '" ' + lang.translate('crre.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.titre + '" ' + lang.translate('crre.basket.' + action + '.articles');
            toasts.confirm(basket.amount === 1 ? messageForOne : messageForMany);
        };

        $scope.initCampaign = async (structure) => {
            if (structure) {
                await $scope.campaigns.sync(structure.id);
                Utils.safeApply($scope);
            }
        };

        $scope.syncOrders = async (status: string) =>{
            $scope.ordersClient.all = [];
            await $scope.ordersClient.sync(status, $scope.structures.all, null, null, null, 0);
            $scope.displayedOrders.all = $scope.ordersClient.all;
        };

        $scope.initOrders = async (status: string) => {
            await $scope.initOrderStructures();
            await $scope.syncOrders(status);
            Utils.safeApply($scope);
        };

        $scope.initOrderStructures = async () => {
            $scope.loadingArray = true;
            $scope.structures = new Structures();
            await $scope.structures.sync();
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.initOrdersForPreview = async (orders: OrderClient[]) => {
            $scope.orderToSend = new OrdersClient();
            $scope.orderToSend.all = Mix.castArrayAs(OrderClient, orders);
            $scope.orderToSend.preview = await $scope.orderToSend.getPreviewData();
            $scope.orderToSend.preview.index = 0;
        };
        $scope.openLightSelectCampaign = async ():Promise<void> => {
            template.open('administrator-main');
            template.open('selectCampaign', 'validator/select-campaign');
            $scope.display.lightbox.lightBoxIsOpen = true;
            $scope.initOrders('WAITING');
            Utils.safeApply($scope);
        };
        $scope.selectCampaignShow = (campaign?: Campaign, type?: string): void => {
            $scope.display.lightbox.lightBoxIsOpen = false;
            template.close('selectCampaign');
            if(campaign){
                $scope.campaign = campaign;
                $scope.displayedOrders.all = $scope.ordersClient.all
                    .filter( order => order.id_campaign === campaign.id || campaign.id === -1);
                $scope.cancelSelectCampaign(false, type);
            } else {
                $scope.cancelSelectCampaign(true, type);
            }
        };
        $scope.getOrderWaitingFiltered = async (campaign:Campaign):Promise<void> =>{
            await $scope.initOrders('WAITING');
            $scope.selectCampaignShow(campaign, "WAITING");
        };
        $scope.cancelSelectCampaign = (initOrder: boolean, type:string):void => {
            if(initOrder) {
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
            template.close('campaign-main')
            template.open('main-profile', 'customer/campaign/campaign-detail');
            if(type == "WAITING") {
                template.open('administrator-main', 'validator/order-waiting');
            } else {
                template.open('administrator-main', 'validator/order-historic');
            }
            Utils.safeApply($scope);
        };

        if ($scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        }
        Utils.safeApply($scope);

        $scope.formatDate = (date) => {
            if(date)
                return moment(date).format("DD/MM/YYYY");
            else{
                return lang.translate("no.date");
            }
        }
    }]);
