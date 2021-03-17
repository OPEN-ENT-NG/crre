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
    Student, Offers, Offer
} from '../model';
import {INFINITE_SCROLL_EVENTER} from "../enum/infinite-scroll-eventer";

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
        $scope.query = {
            word: "",
        };
        $scope.selectedType = $location.path();

        route({
            main: async () => {
                if ($scope.isAdministrator()) {
                    $scope.redirectTo("/order/waiting");
                }
                else if ($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor() && !$scope.isAdministrator()) {
                    $scope.redirectTo(`/equipments/catalog/0`);
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
            showCatalog: async (params) => {
                await $scope.setCampaign(params);
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
                await $scope.setCampaign(params);
                template.close('administrator-main');
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/equipment-detail');
                await $scope.selectEquipment(params);
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
                template.close('campaign-main');
                $scope.ordersClient.all = [];
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('administrator-main', 'validator/order-waiting');
                $scope.loading = true;
                Utils.safeApply($scope);
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if(!$scope.current.structure)
                    await $scope.initStructures();
                await $scope.selectCampaign(idCampaign);
                let campaignPref;
                campaignPref = $scope.campaign;
                if (campaignPref) {
                    await $scope.initOrders('WAITING');
                    $scope.selectCampaignShow(campaignPref, "WAITING");
                } else {
                    await $scope.openLightSelectCampaign();
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
                } else {
                    await $scope.openLightSelectCampaign();
                    $scope.loading = false;
                    Utils.safeApply($scope);
                }
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

        $scope.setCampaign = async function (params) {
            let idCampaign = params.idCampaign;
            $scope.idIsInteger(idCampaign);
            if (idCampaign != "0") {
                if (!$scope.current.structure)
                    await $scope.initStructures();
                await $scope.selectCampaign(idCampaign);
            }
        }

        $scope.selectCatalog = async function (){
            $scope.fromCatalog=true
            $scope.display.equipment = false;
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            Utils.safeApply($scope);
        }

        $scope.selectEquipment = async function (params){
            let idEquipment = params.idEquipment;
            $scope.idIsInteger(idEquipment);
            if(!$scope.current.structure){
                await $scope.initStructures()
                if($scope.current.structure)
                    await $scope.initBasketItem(parseInt(idEquipment), $scope.campaign.id, $scope.current.structure.id);
                else
                    await $scope.initBasketItem(parseInt(idEquipment), $scope.campaign.id, null);
            } else {
                await $scope.initBasketItem(parseInt(idEquipment), $scope.campaign.id, $scope.current.structure.id);
            }
            if($scope.basket.equipment.type === 'articlenumerique') {
                await $scope.computeOffer();
                await $scope.computeTechnos();
            }
            window.scrollTo(0, 0);
            $scope.display.equipment = true;
        }

        $scope.selectCampaign = async function (idCampaign) {
            if (!$scope.campaign.id && idCampaign) {
                await $scope.campaigns.sync($scope.current.structure.id);
                $scope.campaigns.all.forEach(campaign => {
                    if (campaign.id == idCampaign) {
                        $scope.campaign = campaign;
                    }
                });
            }
        };

        const removeUselessTechnos = (techno) => {
            let removeTechnos = ["configurationMiniOS", "technologie", "$$hashKey", "versionReader", "annotations",
                "assignationTachesEleves", "availableHorsENT", "captureImage", "creationCours", "deploiementMasse",
                "exercicesAutoCorriges", "exportCleUSB", "exportDocument", "exportReponsesEleves", "exportSCORM",
                "fonctionsRecherche", "importDocument", "marquePage", "modifContenuEditorial", "oneClic", "partageContenuEleves",
                "personnalisationUserInterface", "zoom"];
            removeTechnos.forEach(t => {
                delete techno[t];
            });
            return techno;
        }

        $scope.computeTechnos = async () => {
            let technos = $scope.basket.equipment.technos;
            for (let i = 0; i < technos.length; i++) {
                if(i + 1 < technos.length) {
                    let technoTemp = JSON.parse(JSON.stringify(technos[i]));
                    removeUselessTechnos(technoTemp);
                    for(let j = i + 1; j < technos.length; j++) {
                        let technoTemp2 = JSON.parse(JSON.stringify(technos[j]));
                        removeUselessTechnos(technoTemp2);
                        if (_.isEqual(technoTemp, technoTemp2)) {
                            $scope.basket.equipment.technos[i].technologie += ", " + $scope.basket.equipment.technos[j].technologie;
                            $scope.basket.equipment.technos.splice(j, 1);
                        }
                        if(i + 1 >= technos.length && technos.length == 2) {
                            let technoTemp = JSON.parse(JSON.stringify(technos[0]));
                            removeUselessTechnos(technoTemp);
                            let technoTemp2 = JSON.parse(JSON.stringify(technos[1]));
                            removeUselessTechnos(technoTemp2);
                            if (_.isEqual(technoTemp, technoTemp2)) {
                                $scope.basket.equipment.technos[0].technologie += ", " + $scope.basket.equipment.technos[1].technologie;
                                $scope.basket.equipment.technos.splice(1, 1);
                            }
                        }
                    }
                }
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
                await $scope.equipment.sync(idEquipment, structure);
            }
            if(!idCampaign)
                idCampaign = null;
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
            let url = `/equipments/catalog/equipment/${equipment.id}`;
            if($scope.campaign && $scope.campaign.id)
                url += `/${$scope.campaign.id}`
            else
                url += `/0`
            $scope.redirectTo(url);
        };

        $scope.openEquipmentId = (equipmentId: string) => {
            let url = `/equipments/catalog/equipment/${equipmentId}`;
            if($scope.campaign && $scope.campaign.id)
                url += `/${$scope.campaign.id}`
            else
                url += `/0`
            $scope.redirectTo(url);
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
            const newData = await $scope.ordersClient.sync(status, $scope.structures.all, null, null, null, 0);
            if (newData)
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.displayedOrders.all = $scope.ordersClient.all;
        };

        $scope.initOrders = async (status: string) => {
            await $scope.initOrderStructures();
            await $scope.syncOrders(status);
        };

        $scope.initOrderStructures = async () => {
            $scope.loadingArray = true;
            $scope.structures = new Structures();
            await $scope.structures.sync();
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.openLightSelectCampaign = async ():Promise<void> => {
            await template.open('administrator-main');
            await template.open('selectCampaign', 'validator/select-campaign');
            $scope.display.lightbox.lightBoxIsOpen = true;
            await $scope.initOrders('WAITING');
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

        $scope.computeOffer = async () => {
            let amount = $scope.basket.amount;
            let gratuit = 0;
            let gratuite = 0;
            let offre = null;
            $scope.offerStudent = "";
            $scope.offerTeacher = "";
            $scope.offers = new Offers();
            $scope.basket.equipment.offres[0].leps.forEach(function (offer) {
                offre = new Offer();
                offre.name = offer.licence[0].valeur;
                if(offer.conditions.length > 1) {
                    offer.conditions.forEach(function (condition) {
                        if(offer.licence[0].valeur === "Elève") {
                            $scope.offerStudent += condition.gratuite + " licence élève gratuite pour " + condition.conditionGratuite + ", ";
                        } else {
                            $scope.offerTeacher += condition.gratuite + " licence enseignant gratuite pour " + condition.conditionGratuite + ", ";
                        }
                        if(amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                            gratuit = condition.conditionGratuite;
                            gratuite = condition.gratuite;
                        }
                    });
                    if(offer.licence[0].valeur === "Elève") {
                        $scope.offerStudent = $scope.offerStudent.slice(0, -2);
                    } else {
                        $scope.offerTeacher = $scope.offerTeacher.slice(0, -2);
                    }
                } else {
                    if(offer.licence[0].valeur === "Elève") {
                        $scope.offerStudent += offer.conditions[0].gratuite + " licence élève gratuite pour " + offer.conditions[0].conditionGratuite;
                    } else {
                        $scope.offerTeacher += offer.conditions[0].gratuite + " licence enseignant gratuite pour " + offer.conditions[0].conditionGratuite;
                    }
                    gratuit = offer.conditions[0].conditionGratuite;
                    gratuite = offer.conditions[0].gratuite * Math.floor(amount/gratuit);
                }
                offre.value = gratuite;
                $scope.offers.all.push(offre);
            });
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
