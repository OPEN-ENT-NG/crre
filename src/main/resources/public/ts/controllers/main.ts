import {_,notify, $, Behaviours, idiom as lang, model, moment, ng, template, toasts} from 'entcore';
import {
    Agents,
    Basket,
    Baskets,
    Campaign,
    Campaigns,
    Contracts,
    ContractTypes,
    Equipment,
    Equipments,
    EquipmentTypes,
    Exercises,
    Exports,
    Instructions,
    labels,
    Logs,
    Notification,
    Operations, Order,
    OrderClient,
    OrderRegion,
    OrdersClient, OrderUtils,
    PRIORITY_FIELD,
    Programs,
    StructureGroups,
    Structures,
    Supplier,
    Suppliers,
    Tags,
    Taxes,
    Titles, Userbook,
    Utils,
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
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.contractTypes = new ContractTypes();
        $scope.programs = new Programs();
        $scope.contracts = new Contracts();
        $scope.tags = new Tags();
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.taxes = new Taxes();
        $scope.operations= new Operations();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.orderClient = new OrderClient();
        $scope.orderRegion = new OrderRegion();
        $scope.displayedOrders = new OrdersClient();
        $scope.equipmentTypes = new EquipmentTypes();
        $scope.instructions = new Instructions();
        $scope.exercises = new Exercises();
        $scope.exports = new Exports([]);
        $scope.ub = new Userbook();
        $scope.equipments.eventer.on('loading::true', $scope.$apply);
        $scope.equipments.eventer.on('loading::false', $scope.$apply);
        $scope.loadingArray = false;

        route({
            main: async () => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    $scope.redirectTo('/campaigns');
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
            manageEquipmentTags: async () => {
                template.open('administrator-main', 'administrator/tag/manage-tags');
                await $scope.tags.sync(true);
                Utils.safeApply($scope);
            },
            manageEquipments: async () => {
                delete $scope.equipment;

                template.open('administrator-main', 'administrator/equipment/equipment-container');
                template.open('equipments-main', 'administrator/equipment/manage-equipments');
                await $scope.equipments.sync();
                await $scope.contracts.sync();
                await $scope.equipmentTypes.sync();
                $scope.taxes.sync();
                $scope.tags.sync();
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
            createCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            updateCampaigns: async () => {
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
                campaign.titles = new Titles();
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
            campaignCatalog: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.fromCatalog=true
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures() ;
                if(!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                template.close('right-side');
                $scope.display.equipment = false;
                Utils.safeApply($scope);
                $scope.current.structure ? await $scope.equipments.sync(idCampaign, $scope.current.structure.id) : null;
            },
            equipmentDetail: async (params) => {
                let idCampaign = params.idCampaign;
                let idEquipment = params.idEquipment;
                $scope.idIsInteger(idCampaign);
                $scope.idIsInteger(idEquipment);
                $scope.current.structure
                    ? await $scope.initBasketItem(parseInt(idEquipment), parseInt(idCampaign), $scope.current.structure.id)
                    : null;
                if(!$scope.fromCatalog){
                    $scope.redirectTo(`/campaign/${idCampaign}/catalog`);
                }
                template.open('right-side', 'customer/campaign/catalog/equipment-detail');
                window.scrollTo(0, 0);
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
                if(!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-order');
                $scope.initCampaignOrderView();
                Utils.safeApply($scope);
            },
            campaignBasket: async (params) => {
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/basket/manage-basket');
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures() ;
                if($scope.current.structure) {
                    await $scope.baskets.sync(idCampaign, $scope.current.structure.id);
                }
                if(!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }

                Utils.safeApply($scope);
            },
            orderWaiting: async () => {
                await $scope.syncCampaignInputSelected();
                $scope.preferences = await $scope.ub.getPreferences();
                if ($scope.preferences && $scope.preferences.preference){
                    if ($scope.fromWaiting)
                        $scope.fromWaiting = false;
                    let preferences = JSON.parse($scope.preferences.preference);
                    if (preferences.ordersWaitingCampaign) {
                        let campaignPref;
                        $scope.campaignsForSelectInput.forEach(c => {
                            if (c.id === preferences.ordersWaitingCampaign)
                                campaignPref = c;
                        });
                        if (campaignPref) {
                            await $scope.initOrders('WAITING');
                            $scope.selectCampaignShow(campaignPref);
                        } else
                            await $scope.openLightSelectCampaign();
                    } else
                        await $scope.openLightSelectCampaign();
                }else
                    await $scope.openLightSelectCampaign();

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
            instruction: async () =>{
                await $scope.initInstructions();
                template.open('administrator-main', 'administrator/instruction/instruction-container');
                template.open('instruction-main', 'administrator/instruction/manage-instruction');
                Utils.safeApply($scope);
            },
            operation: async () =>{
                $scope.loadingArray = true;
                await $scope.initOperation();
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/manage-operation');
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },

            operationOrders: async (params) =>{
                $scope.loadingArray = true;
                template.close('administrator-main');
                template.close('operation-main');
                $scope.operations = new Operations();
                $scope.structures = new Structures();
                await $scope.structures.sync();
                await $scope.operations.sync();
                $scope.operation = await $scope.operations.all.find(operationFound => operationFound.id.toString() === params.idOperation.toString());
                $scope.ordersClientByOperation = await $scope.operation.getOrders($scope.structures.all);
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/operation-orders-list');
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            createRegionOrder: async () => {
                $scope.loadingArray = true;
                await  $scope.campaigns.sync();
                await  $scope.operations.sync();
                let operations = [];
                $scope.operations.all.map((operation,index)=>{
                    if(operation.status == 'true' && !operation.instruction) {
                        operations.push(operation);
                    }
                });

                await $scope.contractTypes.sync();
                $scope.operations.all = operations;
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
        $scope.initInstructions = async ()=>{
            $scope.loadingArray = true;
            await $scope.instructions.sync();
            $scope.loadingArray = false;
        };
        $scope.initCampaignOrderView=()=>{
            if( $scope.campaign.priority_enabled == true && $scope.campaign.priority_field == PRIORITY_FIELD.ORDER){
                template.open('order-list', 'customer/campaign/order/orders-by-equipment');
            } else {
                template.open('order-list', 'customer/campaign/order/orders-by-project');
            }
        };

        $scope.initOperation = async (id_label? ) =>{
            $scope.labelOperation = new labels();
            await $scope.labelOperation.sync();
            await $scope.operations.sync();
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
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.access);
        };

        $scope.isManager = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.manager);
        };

        $scope.isAdministrator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator);
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $rootScope.$on('eventEmitedCampaign', function (event, data) {
            $scope.campaign = data;
        });

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };

        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
            let priceFloat = parseFloat(price);
            let taxFloat = parseFloat(tax_value);
            let price_TTC = ((priceFloat + ((priceFloat * taxFloat) / 100)));
            return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC) : '';
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
            if(!equipment.price_proposal){
                equipment.options.map((option) => {
                    (option.required === true || (selectedOptions ? option.selected === true : false))
                        ? price += parseFloat($scope.calculatePriceTTC(option.price, option.tax_amount))
                        : null;
                });
            }

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
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.articles');
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

        $scope.initOrders = async (status) => {
            await $scope.initOrderStructures();
            await $scope.syncOrders(status);
            Utils.safeApply($scope);
        };

        $scope.initOrderStructures = async () => {
            $scope.loadingArray = true;
            $scope.structures = new Structures();
            await $scope.structures.sync();
            await $scope.structures.getStructureType();
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.initOrdersForPreview = async (orders: OrderClient[]) => {
            $scope.orderToSend = new OrdersClient(Mix.castAs(Supplier, orders[0].supplier));
            $scope.orderToSend.all = Mix.castArrayAs(OrderClient, orders);
            $scope.orderToSend.preview = await $scope.orderToSend.getPreviewData();
            $scope.orderToSend.preview.index = 0;
        };
        $scope.syncCampaignInputSelected = async ():Promise<void> => {
            $scope.campaignsForSelectInput = [];
            $scope.allCampaignsSelect = new Campaign(lang.translate("lystore.campaign.order.all"), '');
            $scope.allCampaignsSelect.id = -1;
            await $scope.campaigns.sync();
            $scope.campaignsForSelectInput = [...$scope.campaigns.all];
            $scope.campaignsForSelectInput.unshift( $scope.allCampaignsSelect);

        };
        $scope.openLightSelectCampaign = async ():Promise<void> => {
            template.open('administrator-main');
            template.open('selectCampaign', 'administrator/order/select-campaign');
            $scope.display.lightbox.lightBoxIsOpen = true;
            $scope.initOrders('WAITING');
            Utils.safeApply($scope);
        };
        $scope.selectCampaignShow = (campaign?: Campaign): void => {
            $scope.ub.putPreferences("ordersWaitingCampaign", campaign.id);
            $scope.display.lightbox.lightBoxIsOpen = false;
            template.close('selectCampaign');
            if(campaign){
                $scope.campaign = campaign;
                $scope.displayedOrders.all = $scope.ordersClient.all
                    .filter( order => order.campaign.id === campaign.id || campaign.id === -1);
                $scope.cancelSelectCampaign(false);
            } else {
                $scope.campaign = $scope.allCampaignsSelect;
                $scope.cancelSelectCampaign(true);
            }
        };
        $scope.getOrderWaitingFiltered = async (campaign:Campaign):Promise<void> =>{
            await $scope.initOrders('WAITING');
            $scope.selectCampaignShow(campaign);
        };
        $scope.cancelSelectCampaign = (initOrder: boolean):void => {
            if(initOrder) {
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
            template.open('administrator-main', 'administrator/order/order-waiting');
            Utils.safeApply($scope);
        };

        if ($scope.isManager() || $scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        }
        else if ($scope.hasAccess() && !$scope.isManager() && !$scope.isAdministrator()) {
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
