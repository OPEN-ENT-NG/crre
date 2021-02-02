import {_,Behaviours, idiom as lang, model, moment, ng, template, toasts} from 'entcore';
import {
    Agents,
    Basket,
    Baskets,
    BasketsOrders,
    Campaign,
    Campaigns,
    Contracts,
    ContractTypes,
    Equipment,
    Equipments,
    Exports,
    Logs,
    Order,
    OrderClient,
    OrderRegion,
    OrdersClient, OrderUtils,
    StructureGroups,
    Structures,
    Suppliers,
    Taxes,
    Utils, OrdersRegion, Filters,
} from '../model';
import {Mix} from "entcore-toolkit";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$rootScope', '$timeout',
    ($scope, route, $location, $rootScope, $timeout) => {
        template.open('main', 'main');

        $scope.display = {
            equipment: false,
            lightbox: {lightBoxIsOpen: false,}
        };
        $scope.structures = new Structures();
        $scope.current = {};
        $scope.notifications = [];
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.contractTypes = new ContractTypes();
        $scope.contracts = new Contracts();
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.taxes = new Taxes();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.orderClient = new OrderClient();
        $scope.orderRegion = new OrderRegion();
        $scope.displayedOrders = new OrdersClient();
        $scope.displayedOrdersRegion = new OrdersRegion();
        $scope.basketsOrders = new BasketsOrders();
        $scope.users = [];
        $scope.filters = new Filters();
        $scope.exports = new Exports([]);
        //$scope.ub = new Userbook();
        $scope.equipments.eventer.on('loading::true', $scope.$apply);
        $scope.equipments.eventer.on('loading::false', $scope.$apply);
        $scope.loadingArray = false;

        route({
            main: async () => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    $scope.redirectTo("/order/waiting");
                }
                else if ($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor() && !$scope.isManager() && !$scope.isAdministrator()) {
                    $scope.redirectTo(`/equipments/catalog`);
                } else {
                    await $scope.initStructures();
                    await $scope.initCampaign($scope.current.structure);
                    template.open('main-profile', 'customer/campaign/campaign-list');
                }
                Utils.safeApply($scope);
            },
            manageAgents: async () => {
                template.open('administrator-main', 'administrator/agent/manage-agents');
                await $scope.agents.sync();
                Utils.safeApply($scope);
            },
            manageSuppliers: async () => {
                template.open('administrator-main', 'administrator/supplier/manage-suppliers');
                await $scope.suppliers.sync();
                Utils.safeApply($scope);
            },
            manageContracts: async () => {
                template.open('administrator-main', 'administrator/contract/manage-contract');
                await $scope.contracts.sync();
                $scope.agents.sync();
                $scope.suppliers.sync();
                $scope.contractTypes.sync();
                Utils.safeApply($scope);
            },
            manageEquipments: async () => {
                delete $scope.equipment;

                template.open('administrator-main', 'administrator/equipment/equipment-container');
                template.open('equipments-main', 'administrator/equipment/manage-equipments');
                await $scope.equipments.sync();
                await $scope.contracts.sync();
                $scope.taxes.sync();
                Utils.safeApply($scope);
            },
            createEquipment: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/equipments');
                }
                template.open('equipments-main', 'administrator/equipment/equipment-form');
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
            managePurse: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                if (template.isEmpty('administrator-main') || campaign === undefined || !campaign.purse_enabled) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageTitles: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                if (template.isEmpty('administrator-main') || campaign === undefined) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/title/manage-title');
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
                await $scope.selectCatalog();
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                Utils.safeApply($scope);
            },
            showAdminCatalog: async () => {
                await $scope.selectCatalog();
                template.open('administrator-main', 'customer/campaign/catalog/catalog-list');
                Utils.safeApply($scope);
            },
            campaignCatalog: async () => {
                let idCampaign = $scope.campaign.id;
                $scope.fromCatalog=true
                $scope.idIsInteger(idCampaign);
                $scope.equipments = new Equipments();
                if(!$scope.current.structure)
                    await $scope.initStructures() ;
                await $scope.selectCampaign(idCampaign);
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                $scope.display.equipment = false;
                $scope.current.structure ? await $scope.equipments.sync(true, undefined, undefined ) : null;
                Utils.safeApply($scope);
            },
            equipmentDetail: async (params) => {
                await $scope.selectEquipment(params);
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
                $scope.current.structure
                    ? await $scope.ordersClient.sync(null, [], idCampaign, $scope.current.structure.id)
                    : null;
                await $scope.selectCampaign(idCampaign);
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-order');
                $scope.initCampaignOrderView();
                $scope.campaign.order_notification = 0;
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
                //$scope.preferences = await $scope.ub.getPreferences();
                let campaignPref;
                campaignPref = $scope.campaign;
                if (campaignPref) {
                    await $scope.initOrders('WAITING');
                    $scope.selectCampaignShow(campaignPref, "WAITING");
                } else
                    await $scope.openLightSelectCampaign();
                Utils.safeApply($scope);
            },
            orderHistoric: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures();
                await $scope.selectCampaign(idCampaign);
                //$scope.preferences = await $scope.ub.getPreferences();
                let campaignPref;
                campaignPref = $scope.campaign;
                if (campaignPref) {
                    //await $scope.initOrders('ALL');
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
            orderSent: async () => {
                template.open('administrator-main', 'administrator/order/order-sent');
                $scope.structures = new Structures();
                await $scope.initOrders('SENT');
                $scope.orderToSend = null;
                Utils.safeApply($scope);
            },
            orderClientValided: () => {
                $scope.initOrders('VALID');
                template.open('administrator-main', 'administrator/order/order-valided');
                Utils.safeApply($scope);
            },
            previewOrder: async () => {
                template.open('administrator-main', 'administrator/order/order-send-prepare');
                template.open('sendOrder.preview', 'pdf/preview');
            },
            updateOrder: async (params:any):Promise<void> => {
                template.open('administrator-main', 'administrator/order/order-update-form');
                let idOrder = parseInt(params.idOrder);
                $scope.fromWaiting = true;
                await $scope.initOrderStructures();
                $scope.orderToUpdate = await $scope.orderClient.getOneOrderClient(idOrder, $scope.structures.all, "waiting");
                await $scope.equipments.syncAll($scope.orderToUpdate.campaign.id);
                $scope.orderToUpdate.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderToUpdate.equipment_key);
                $scope.orderParent = OrderUtils.initParentOrder($scope.orderToUpdate);
                Utils.safeApply($scope);

            },
            updateLinkedOrder: async (params:any):Promise<void> => {
                template.open('administrator-main', 'administrator/order/order-update-form');
                let idOrder = parseInt(params.idOrder);
                await $scope.initOrderStructures();
                $scope.orderToUpdate = params.typeOrder === 'client' ?
                    await $scope.orderClient.getOneOrderClient(idOrder, $scope.structures.all, "progress") :
                    await $scope.orderRegion.getOneOrderRegion(idOrder, $scope.structures.all);
                await $scope.equipments.syncAll($scope.orderToUpdate.campaign.id);
                $scope.orderToUpdate.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderToUpdate.equipment_key);
                if(params.typeOrder === 'client'){
                    $scope.orderParent = OrderUtils.initParentOrder($scope.orderToUpdate);
                } else {
                    if($scope.orderToUpdate.order_parent){
                        $scope.orderParent = new Order(JSON.parse($scope.orderToUpdate.order_parent), $scope.structures.all);
                        $scope.orderParent.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderParent.equipment_key);
                        $scope.orderParent = OrderUtils.initParentOrder($scope.orderParent);
                    } else {
                        $scope.orderParent = undefined;
                    }
                }
                Utils.safeApply($scope);
            },
            createRegionOrder: async () => {
                $scope.loadingArray = true;
                await  $scope.campaigns.sync();
                await $scope.contractTypes.sync();
                await $scope.structures.sync();
                template.open('administrator-main', 'administrator/orderRegion/order-region-create-form');
                $scope.loadingArray = false;
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
            $scope.equipments = new Equipments();
            $scope.display.equipment = false;
            await $scope.equipments.sync(true, undefined, undefined );
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
            // if( $scope.campaign.priority_enabled == true && $scope.campaign.priority_field == PRIORITY_FIELD.ORDER){
            //     template.open('order-list', 'customer/campaign/order/orders-by-equipment');
            // } else {
            //     template.open('order-list', 'customer/campaign/order/orders-by-project');
            // }
        };

        $scope.initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere($scope.equipments.all, {id: idEquipment});
            if ($scope.equipment === undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                await $scope.equipment.sync(idEquipment);
            }
            $scope.basket = new Basket($scope.equipment, idCampaign, structure);
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

        $scope.isManager = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.manager);
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

        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) =>  {
            return Utils.calculatePriceTTC(price,tax_value,roundNumber);
        };

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {boolean} selectedOptions [Consider selected options or not)
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {string | number}
         */
        $scope.calculatePriceOfEquipment = (equipment: any, selectedOptions: boolean, roundNumber: number = 2) => {
            let price = parseFloat((equipment.price_proposal)? equipment.price_proposal : $scope.calculatePriceTTC(equipment.price, equipment.tax_amount));
            /*            if(!equipment.price_proposal){
                            equipment.options.map((option) => {
                                (option.required === true || (selectedOptions ? option.selected === true : false))
                                    ? price += parseFloat($scope.calculatePriceTTC(option.price, option.tax_amount))
                                    : null;
                            });
                        }*/

            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price) : price;
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
                + basket.equipment.name + '" ' + lang.translate('crre.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.name + '" ' + lang.translate('crre.basket.' + action + '.articles');
            toasts.confirm(basket.amount === 1 ? messageForOne : messageForMany);
        };

        $scope.initCampaign = async (structure) => {
            if (structure) {
                await $scope.campaigns.sync(structure.id);
                Utils.safeApply($scope);
            }
        };

        $scope.syncOrders = async (status: string) =>{
            $scope.displayedOrders.all = [];
            await $scope.ordersClient.sync(status, $scope.structures.all);
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

        if ($scope.isManager() || $scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        }
        else if (($scope.hasAccess() || $scope.isValidator() || $scope.isPrescriptor()) && !$scope.isManager() && !$scope.isAdministrator()) {
            // template.open('main-profile', 'customer/campaign/campaign-list');
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
