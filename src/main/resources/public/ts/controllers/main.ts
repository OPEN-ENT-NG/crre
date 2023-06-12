import {_, Behaviours, idiom as lang, model, moment, ng, template, toasts} from 'entcore';
import {
    Basket,
    Baskets,
    BasketsOrders,
    Campaign,
    Campaigns,
    Equipment,
    Equipments,
    Filters,
    Logs,
    Offers,
    OrdersClient,
    Statistics,
    StatisticsStructures,
    Structure,
    StructureGroups,
    Structures,
    Student,
    Utils, WorkflowNeo4jModel
} from '../model';
import {INFINITE_SCROLL_EVENTER} from "../enum/infinite-scroll-eventer";
import {COMBO_LABELS} from "../enum/comboLabels";
import {ValidatorOrderWaitingFilter} from "../model/ValidatorOrderWaitingFilter";
import {TypeCatalogEnum} from "../enum/type-catalog-enum";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$rootScope',
    ($scope, route, $location, $rootScope) => {
        template.open('main', 'main');
        $scope.display = {
            equipment: false,
            lightbox: {lightBoxIsOpen: false,}
        };
        $scope.structures = new Structures();
        $scope.current = {};
        $scope.lang = lang;
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.displayedOrders = new OrdersClient();
        $scope.basketsOrders = new BasketsOrders();
        $scope.users = [];
        if (!!!$scope.filters) {
            $scope.filters = new Filters();
        }
        $scope.student = new Student();
        $scope.stats = new Statistics();
        $scope.statsStructure = new StatisticsStructures();
        $scope.total_licence = 0;
        $scope.loadingArray = false;
        $scope.query = {
            word: "",
        };
        $scope.labels = ["technologie", "dispositifDYS", "webAdaptatif", "exercicesInteractifs", "availableViaENT",
            "availableViaGAR", "canUseOffline", "needFlash", "corrigesPourEnseignants"];
        $scope.categories = ["Établissements professionnels papier", "Établissements généraux papier", "Établissements polyvalents papier",
            "Établissements professionnels numériques", "Établissements généraux numériques", "Établissements polyvalents numériques",
            "Établissements professionnels mixtes", "Établissements généraux mixtes", "Établissements polyvalents mixtes"];
        $scope.selectedType = $location.path();
        $scope.comboLabels = COMBO_LABELS;
        $scope.offerStudent = [];
        $scope.offerTeacher = [];
        $scope.offers = new Offers();

        route({
            main: async () => {
                $scope.equipments.filterFulfilled = false;
                if ($scope.isAdministrator()) {
                    $scope.redirectTo("/order/waitingAdmin");
                } else {
                    if ($scope.isValidator() || $scope.isPrescriptor()) {
                        $scope.selectedType = $location.path();
                        await $scope.initStructures((!!$scope.current.structure) ? $scope.current.structure.id : null);
                        await $scope.initCampaign($scope.current.structure);
                        await template.open('main-profile', 'prescriptor/campaign/campaign-list');
                    } else if ($scope.hasAccess()) {
                        $scope.redirectTo(`/equipments/catalog/0`);
                    }
                }
                Utils.safeApply($scope);
            },
            viewLogs: async () => {
                $scope.selectedType = $location.path();
                $scope.loadingArray = true;
                $scope.logs.reset();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/log/view-logs');
                await $scope.logs.loadPage($scope.current.page);
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            viewStats: async () => {
                $scope.selectedType = $location.path();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/stats/view-stats');
                Utils.safeApply($scope);
            },
            manageCampaigns: async () => {
                $scope.selectedType = $location.path();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/campaign/campaign_container');
                await template.open('campaigns-main', 'administrator/campaign/manage-campaign');
                await $scope.campaigns.sync();
                Utils.safeApply($scope);
            },
            createOrUpdateCampaigns: async () => {
                $scope.selectedType = $location.path();
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('main-profile', 'administrator/management-main');
                await template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: async () => {
                $scope.selectedType = $location.path();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageStructureGroups: async () => {
                $scope.selectedType = $location.path();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/structureGroup/structureGroup-container');
                await $scope.structureGroups.sync();
                await template.open('structureGroups-main', 'administrator/structureGroup/manage-structureGroup');
                await initOrderStructures();
                Utils.safeApply($scope);
            },
            createStructureGroup: async () => {
                $scope.selectedType = $location.path();
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/structureGroups');
                }
                template.open('main-profile', 'administrator/management-main');
                await template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            showCatalog: async (params) => {
                $scope.selectedType = $location.path();
                await setCampaign(params);
                await template.open('main-profile', 'prescriptor/campaign-main');
                await template.open('campaign-main', 'prescriptor/catalog/catalog-list');
                await selectCatalog();
                Utils.safeApply($scope);
            },
            showAdminCatalog: async () => {
                $scope.selectedType = $location.path();
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'prescriptor/catalog/catalog-list');
                await selectCatalog();
                Utils.safeApply($scope);
            },
            equipmentDetail: async (params) => {
                $scope.selectedType = $location.path();
                await setCampaign(params);
                await template.open('main-profile', 'prescriptor/campaign-main');
                await template.open('campaign-main', 'prescriptor/catalog/equipment-detail');
                await selectEquipment(params);
                Utils.safeApply($scope);
            },
            adminEquipmentDetail: async (params) => {
                $scope.selectedType = $location.path();
                await selectEquipment(params);
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'prescriptor/catalog/equipment-detail');
                Utils.safeApply($scope);
            },
            campaignOrder: async (params) => {
                $scope.selectedType = $location.path();
                let idCampaign = params.idCampaign;
                idIsInteger(idCampaign);
                if (!$scope.current.structure) {
                    let idStructure = params.idStructure;
                    await $scope.initStructures(idStructure);
                }
                if (!$scope.isPrescriptorInStructure($scope.current.structure)) {
                    $scope.redirectTo('/');
                    return;
                }
                await template.open('main-profile', 'prescriptor/campaign-main');
                await template.open('order-list', 'prescriptor/order/orders-list');
                await selectCampaign(idCampaign);
                $scope.campaign.order_notification = 0;
                await template.open('campaign-main', 'prescriptor/order/manage-order');
                Utils.safeApply($scope);
            },
            campaignBasket: async (params) => {
                $scope.selectedType = $location.path();
                let idCampaign = params.idCampaign;
                idIsInteger(idCampaign);
                if (!$scope.current.structure) {
                    await $scope.initStructures();
                }
                if (!$scope.isPrescriptorInStructure($scope.current.structure)) {
                    $scope.redirectTo('/');
                    return;
                }
                await $scope.baskets.sync(idCampaign, $scope.current.structure.id, $scope.campaign.reassort);
                await template.open('campaign-main', 'prescriptor/basket/manage-basket');
                await template.open('main-profile', 'prescriptor/campaign-main');

                await selectCampaign(idCampaign);
                Utils.safeApply($scope);
            },
            orderWaiting: async (params: any) => {
                $scope.selectedType = $location.path();
                $scope.loading = true;
                $scope.ordersClient.all = [];
                Utils.safeApply($scope);
                if (!$scope.current.structure) {
                    let idStructure = params.idStructure;
                    await $scope.initStructures(idStructure);
                }
                await $scope.getInfos();
                await $scope.initOrders('WAITING');
                selectCampaignShow($scope.campaign, "WAITING");
            },
            orderHistoric: async (params: any) => {
                $scope.selectedType = $location.path();
                if (!$scope.current.structure) {
                    let idStructure = params.idStructure;
                    await $scope.initStructures(idStructure);
                }
                await $scope.getInfos();
                selectCampaignShow($scope.campaign, "HISTORIC");
                $scope.campaign.historic_etab_notification = 0;
                Utils.safeApply($scope);
            },
            orderWaitingAdmin: async () => {
                $scope.selectedType = $location.path();
                $scope.displayedOrders.all = $scope.ordersClient.all;
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/order/order-waiting');
                Utils.safeApply($scope);
            },
            orderHistoricAdmin: async () => {
                $scope.selectedType = $location.path();
                $scope.displayedOrders.all = $scope.ordersClient.all;
                template.open('main-profile', 'administrator/management-main');
                await template.open('administrator-main', 'administrator/order/order-sent-library');
                Utils.safeApply($scope);
            }
        });

        $scope.translate = (key: string): string => lang.translate(key);

        $scope.checkParentSwitch = (basket, checker): void => {
            if (checker) {
                let testAllTrue = true;
                basket.orders.all.forEach(function (order) {
                    if (!order.selected) {
                        testAllTrue = false;
                    }
                });
                basket.selected = testAllTrue;
            } else {
                basket.selected = false;
            }
        };

        const setCampaign = async function (params) {
            let idCampaign = params.idCampaign;
            idIsInteger(idCampaign);
            if (idCampaign != "0") {
                if (!$scope.current.structure)
                    await $scope.initStructures();
                await selectCampaign(idCampaign);
            }
        };

        const selectCatalog = async function () {
            $scope.fromCatalog = true
            $scope.display.equipment = false;
            $scope.equipments.loading = true;
            $scope.equipments.all = [];
            Utils.safeApply($scope);
        }

        const selectEquipment = async function (params) {
            let idEquipment = params.idEquipment;
            idIsInteger(idEquipment);
            if (!$scope.current.structure) {
                await $scope.initStructures()
                await initBasketItem(parseInt(idEquipment), $scope.campaign.id,
                    ($scope.current.structure) ? $scope.current.structure.id : null);
            } else {
                await initBasketItem(parseInt(idEquipment), $scope.campaign.id, $scope.current.structure.id);
            }
            if ($scope.basket.equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
                $scope.offerStudent = [];
                $scope.offerTeacher = [];
                $scope.offers = await Utils.computeOffer($scope.basket, $scope.basket.equipment,
                    $scope.offerStudent, $scope.offerTeacher, true);
                await computeTechnos();
            }
            window.scrollTo(0, 0);
            $scope.display.equipment = true;
        }

        const selectCampaign = async function (idCampaign) {
            if (!$scope.campaign.id && idCampaign) {
                await $scope.campaigns.sync($scope.current.structure.id);
                $scope.campaigns.all.forEach(campaign => {
                    if (campaign.id == idCampaign) {
                        $scope.campaign = campaign;
                    }
                });
            }
        };

        $scope.getInfos = async function () {
            await $scope.campaigns.sync($scope.current.structure.id);
            $scope.campaign = $scope.campaigns.all[0];
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

        const computeTechnos = async () => {
            let technos = $scope.basket.equipment.technos;
            for (let i = 0; i < technos.length; i++) {
                if (i + 1 < technos.length) {
                    let technoTemp = JSON.parse(JSON.stringify(technos[i]));
                    removeUselessTechnos(technoTemp);
                    for (let j = i + 1; j < technos.length; j++) {
                        let technoTemp2 = JSON.parse(JSON.stringify(technos[j]));
                        removeUselessTechnos(technoTemp2);
                        if (_.isEqual(technoTemp, technoTemp2)) {
                            $scope.basket.equipment.technos[i].technologie += ", " + $scope.basket.equipment.technos[j].technologie;
                            $scope.basket.equipment.technos.splice(j, 1);
                        }
                        if (i + 1 >= technos.length && technos.length == 2) {
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

        const initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere($scope.equipments.all, {id: idEquipment});
            if ($scope.equipment == undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                $scope.equipment.loading = true;
                Utils.safeApply($scope);
                await $scope.equipment.sync(idEquipment, structure);
            }
            if (!idCampaign)
                idCampaign = null;
            $scope.basket = new Basket($scope.equipment, idCampaign, structure);
            Utils.safeApply($scope);
        };

        const idIsInteger = (id) => {
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

        /**
         * @deprecated Use {@link hasAccessInStructure}
         */
        $scope.hasAccess = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.access);
        };

        $scope.hasAccessInStructure = (structure: Structure): boolean => {
            return structure.workflow.find((workflowName: WorkflowNeo4jModel) => workflowName.name === Behaviours.applicationsBehaviours.crre.rights.workflow.access) != null;
        };

        /**
         * @deprecated Use {@link hasAccessInStructure}
         */
        $scope.isValidator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.validator);
        };

        $scope.isValidatorInStructure = (structure: Structure): boolean => {
            return structure.workflow.find((workflowName: WorkflowNeo4jModel) => workflowName.name === Behaviours.applicationsBehaviours.crre.rights.workflow.validator) != null;
        };

        $scope.isAdministrator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator);
        };

        /**
         * @deprecated Use {@link hasAccessInStructure}
         */
        $scope.isPrescriptor = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor);
        };

        $scope.isPrescriptorInStructure = (structure: Structure): boolean => {
            return structure.workflow.find((workflowName: WorkflowNeo4jModel) => workflowName.name === Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor) != null;
        };

        $scope.redirectTo = (path: string) => {
            $scope.selectedType = path;
            $location.path(path);
        };

        $rootScope.$on('eventEmitedCampaign', function (event, data) {
            $scope.campaign = data;
        });

        $rootScope.$on('eventEmitedFilters', function (event, data) {
            $scope.filters = data;
        });

        $rootScope.$on('eventEmitedQuery', function (event, data) {
            $scope.query.word = data;
        });

        $scope.openEquipmentId = (equipmentId: string) => {
            let url = `/equipments/catalog/equipment/${equipmentId}`;
            if ($scope.campaign && $scope.campaign.id)
                url += `/${$scope.campaign.id}`
            else
                url += `/0`
            $scope.redirectTo(url);
        };

        $scope.initStructures = async (idStructure?: string) => {
            await $scope.structures.syncUserStructures();
            if (idStructure) {
                $scope.current.structure = $scope.structures.all.find(structure => {
                    return structure.id == idStructure;
                });
            } else {
                $scope.current.structure = $scope.structures.all[0];
            }
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

        const syncOrders = async (status: string) => {
            $scope.ordersClient = new OrdersClient();
            let filter: ValidatorOrderWaitingFilter = new ValidatorOrderWaitingFilter();
            filter.startDate = moment().add(-1, 'years')._d;
            filter.endDate = moment()._d;
            const newData = await $scope.ordersClient.searchOrder($scope.current.structure.id, filter, true, 0);
            if (newData)
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.displayedOrders.all = $scope.ordersClient.all;
            $scope.loading = false;
        };

        $scope.initOrders = async (status: string) => {
            $scope.loadingArray = true;
            await initOrderStructures();
            await syncOrders(status);
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        const initOrderStructures = async () => {
            $scope.structures = new Structures();
            await $scope.structures.sync();
        };

        const selectCampaignShow = (campaign?: Campaign, type?: string): void => {
            if (campaign) {
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                Utils.safeApply($scope);
                cancelSelectCampaign(false, type);
            } else {
                cancelSelectCampaign(true, type);
            }
        };

        $scope.isInCampaignList = () => {
            let isInCampaign = false;
            if (location.hash == "#/") {
                isInCampaign = true;
            }
            return isInCampaign;
        };

        $scope.getColor = (id) => {
            let color = "";
            switch (id) {
                case 0:
                    color = "SENT";
                    break;
                case 1:
                    color = "NEW";
                    break;
                case 2:
                    color = "GRAY";
                    break;
                case 3:
                    color = "GRAY";
                    break;
                case 4:
                    color = "GRAY";
                    break;
                case 6:
                    color = "GRAY";
                    break;
                case 7:
                    color = "GRAY";
                    break;
                case 9:
                    color = "GRAY";
                    break;
                case 10:
                    color = "DONE";
                    break;
                case 14:
                    color = "WAITING_FOR_ACCEPTANCE";
                    break;
                case 15:
                    color = "REJECTED";
                    break;
                case 20:
                    color = "REJECTED";
                    break;
                case 35:
                    color = "WAITING_FOR_ACCEPTANCE";
                    break;
                case 52:
                    color = "WAITING_FOR_ACCEPTANCE";
                    break;
                case 55:
                    color = "SENT";
                    break;
                case 57:
                    color = "SENT";
                    break;
                case 58:
                    color = "BLUE";
                    break;
                case 59:
                    color = "REJECTED";
                    break;
                case 70:
                    color = "SENT";
                    break;
                case 71:
                    color = "SENT";
                    break;
                case 72:
                    color = "SENT";
                    break;
                case 1000:
                    color = "GRAY";
                    break;
            }
            return color;
        };

        const cancelSelectCampaign = (initOrder: boolean, type: string): void => {
            if (initOrder) {
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
            template.open('main-profile', 'validator/main');
            if (type == "WAITING") {
                template.open('campaign-main', 'validator/order-waiting');
            } else {
                template.open('campaign-main', 'validator/order-historic');
            }
            Utils.safeApply($scope);
        };

        $scope.statsByStructures = async () => {
            template.open('main-profile', 'administrator/management-main');
            await template.open('administrator-main', 'administrator/stats/view-stats-structures');
            Utils.safeApply($scope);
        }

        $scope.statsGlobal = async () => {
            template.open('main-profile', 'administrator/management-main');
            await template.open('administrator-main', 'administrator/stats/view-stats');
            Utils.safeApply($scope);
        }

    }]);
