import {Behaviours, ng, template, toasts} from 'entcore';
import {
    Equipments,
    Offer,
    Offers,
    OrderClient,
    OrderRegion,
    OrdersRegion,
    Project,
    Projects,
    Utils,
} from "../../../model";
import {ORDER_STATUS_ENUM} from "../../../enum/order-status-enum";
import {ProjectFilter} from "../../../model/ProjectFilter";
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";
import {Mix} from "entcore-toolkit";
import {StatusFilter} from "../../../model/StatusFilter";
import {ORDER_BY_PROJECT_FIELD_ENUM} from "../../../enum/order-by-project-field-enum";

export const waitingOrderRegionController = ng.controller('waitingOrderRegionController',
    ['$scope', async ($scope) => {
        $scope.display = {
            toggle: false,
            loading: true,
            allOrdersSelected: false,
            lightbox: {
                waitingAdmin: false
            },
            projects: new Projects()
        };
        $scope.projectFilter = new ProjectFilter();

        function initProjects() {
            $scope.display.projects = new Projects();
            $scope.projectFilter.page = 0;
            $scope.display.loading = true;
            Utils.safeApply($scope);
        }

        const init = async () => {
            $scope.projectFilter.orderBy = ORDER_BY_PROJECT_FIELD_ENUM.DATE;
            $scope.projectFilter.orderDesc = true;
            $scope.statusFilterList = [new StatusFilter(ORDER_STATUS_ENUM.SENT), new StatusFilter(ORDER_STATUS_ENUM.IN_PROGRESS),
                new StatusFilter(ORDER_STATUS_ENUM.VALID), new StatusFilter(ORDER_STATUS_ENUM.DONE), new StatusFilter(ORDER_STATUS_ENUM.REJECTED)];

            $scope.schoolType = [{name: 'PU'}, {name: 'PR'}];
            $scope.schoolType.forEach((item) => {
                item.toString = () => $scope.translate(item.name);
                item.getValue = () => item.name;
            });

            await $scope.campaigns.sync();
            $scope.campaigns.all.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.equipments.loading = true;
            $scope.equipments.all = [];
            Utils.safeApply($scope);
            await $scope.equipments.sync(true, undefined, undefined);
            $scope.equipments.docsType.forEach((item) => item.getValue = () => item.name);
            await $scope.launchSearch($scope.display.projects, false, false);
            Utils.safeApply($scope);
        };

        $scope.openConfirmGenerateLibraryLightbox = async (): Promise<void> => {
            if ($scope.display.allOrdersSelected || !$scope.display.projects.hasSelectedOrders()) {
                $scope.display.projects.all = [];
                $scope.display.loading = true;
                await $scope.launchSearch(new Projects(), true, true);
            }
            template.open('lightbox.waitingAdmin', 'administrator/order/confirm-generate-library');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.validateOrders = async (): Promise<void> => {
            $scope.display.loading = true;
            let selectedOrders: OrdersRegion = new OrdersRegion();
            $scope.display.projects.forEach((project: Project) => {
                project.orders.forEach(async (order: OrderRegion) => {
                    if (order.selected &&
                        order.status != ORDER_STATUS_ENUM.SENT && order.status != ORDER_STATUS_ENUM.DONE &&
                        order.status != ORDER_STATUS_ENUM.VALID) {
                        selectedOrders.push(order);
                    }
                });
            });
            let projectsToShow: Projects = $scope.display.projects;
            $scope.display.projects = new Projects();
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus(ORDER_STATUS_ENUM.VALID);
            if (status == 200) {
                projectsToShow.forEach((project: Project) => {
                    project.orders.forEach(async (order: OrderRegion) => {
                        if (order.selected && order.status != ORDER_STATUS_ENUM.SENT && order.status != ORDER_STATUS_ENUM.DONE) {
                            order.status = ORDER_STATUS_ENUM.VALID;
                        }
                        order.selected = false;
                    });
                    project.selected = false;
                    Utils.setStatus(project, project.orders);
                });
                toasts.confirm('crre.order.validated');
                $scope.display.projects = projectsToShow;
                $scope.display.toggle = $scope.display.allOrdersSelected = $scope.display.loading = false;
                Utils.safeApply($scope);
            } else {
                $scope.display.projects = projectsToShow;
                $scope.display.loading = false;
                Utils.safeApply($scope);
                if (status == 401) {
                    toasts.warning('crre.order.error.purse');
                } else {
                    toasts.warning('crre.order.validated.error');
                }
            }
        };

        $scope.openRefusingOrderLightbox = () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/refuse-order');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        };

        $scope.switchAllOrders = () => {
            $scope.display.projects.all.forEach(project => {
                project.selected = $scope.display.allOrdersSelected;
                project.orders.forEach(async order => {
                    order.selected = $scope.display.allOrdersSelected;
                });
            });
            $scope.display.toggle = $scope.display.allOrdersSelected;
            Utils.safeApply($scope);
        };

        $scope.checkSwitchAll = (): void => {
            let testAllTrue = true;
            $scope.display.projects.all.forEach(project => {
                if (!project.selected) {
                    testAllTrue = false;
                } else {
                    project.orders.forEach(order => {
                        if (!order.selected) {
                            testAllTrue = false;
                        }
                    })
                }
            });
            $scope.display.allOrdersSelected = testAllTrue;
            Utils.safeApply($scope);
        };

        $scope.exportCSVRegion = async (): Promise<void> => {
            let projects: Projects = $scope.display.projects;
            if ($scope.display.allOrdersSelected) {
                projects = new Projects();
                await $scope.launchSearch(projects, false, true);
            }
            await projects.exportCSV($scope.display.allOrdersSelected);
            Utils.safeApply($scope);
        }

        $scope.loadNextPage = async (): Promise<void> => {
            $scope.projectFilter.page++;
            await $scope.launchSearch(new Projects(), true, false);
        };

        $scope.resetSearch = async (): Promise<void> => {
            initProjects();
            $scope.projectFilter.page = 0;
            await $scope.launchSearch($scope.display.projects, false, false);
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            Utils.safeApply($scope);
        }

        $scope.launchSearch = async (projectList: Projects, addNewData: boolean, onlyId: boolean): Promise<void> => {
            projectList.search($scope.projectFilter)
                .then(async (data: Project[]) => {
                    if (data && data.length > 0) {
                        if (projectList.all.length > 0) {
                            await $scope.synchroRegionOrders(addNewData, onlyId, projectList);
                            $scope.syncSelected();
                            Behaviours.applicationsBehaviours['crre'].InfiniteScrollService
                                .updateScroll();
                        }
                    }
                    $scope.display.loading = false;
                    Utils.safeApply($scope);
                });
        }

        $scope.syncSelected = (): void => {
            $scope.display.projects.all.forEach(project => {
                project.selected = $scope.display.allOrdersSelected;
                project.orders.forEach(order => {
                    order.selected = $scope.display.allOrdersSelected;
                });
            })
        };

        $scope.synchroRegionOrders = async (addNewData: boolean = false, onlyId: boolean = false, projects?: Projects): Promise<void> => {
            let {resultProject, responses} = await getOrdersOfProjects(projects);
            if (responses[0]) {
                let data = [];
                responses.forEach(response => {
                    data = data.concat(response.data);
                });
                await filterAndBeautifyOrders(data, resultProject, onlyId);
                let projectWithOrders = new Projects();
                beautifyProjectsFromOrders(resultProject, projectWithOrders);
                if (addNewData) {
                    $scope.display.projects.all = $scope.display.projects.all.concat(projectWithOrders.all);
                }
            } else {
                $scope.display.projects.all = [];
            }
            $scope.display.loading = false;
            Utils.safeApply($scope);
        };

        async function getOrdersOfProjects(projects: Projects) {
            let resultProject = projects;
            let promesses = [];
            let projetsSplit = new Projects();
            resultProject.forEach(projet => {
                if (projet.count > 500) {
                    if (projetsSplit.all.length > 0) {
                        promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit, $scope.projectFilter));
                        projetsSplit = new Projects();
                    }
                    projetsSplit.push(projet);
                    promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit, $scope.projectFilter));
                    projetsSplit = new Projects();
                } else if (projetsSplit.all.length > 100) {
                    promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit, $scope.projectFilter));
                    projetsSplit = new Projects();
                    projetsSplit.push(projet);
                } else {
                    projetsSplit.push(projet);
                }
            });
            if (projetsSplit.all.length > 0) {
                promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit, $scope.projectFilter));
            }
            if ($scope.structures.all.length == 0 && $scope.isAdministrator()) {
                $scope.structures.sync();
            }
            const responses = await Promise.all(promesses);
            return {resultProject, responses};
        }

        async function filterAndBeautifyOrders(projectsResult, projects: Projects, onlyId: boolean) {
            for (let orders of projectsResult) {
                if (orders.length > 0) {
                    let equipments = new Equipments();
                    if (!onlyId) {
                        const allProjects = projectsResult.reduce((acc, val) => acc.concat(val), []);
                        await equipments.getEquipments(allProjects);
                    }
                    const idProject = orders[0].id_project;
                    for (let order of orders) {
                        if (!onlyId) {
                            if (order.old) {
                                if (order.offers != null) {
                                    let offers = new Offers();
                                    let offersJson = JSON.parse(order.offers);
                                    for (let offerJson of offersJson) {
                                        let offer = new Offer();
                                        offer.name = offerJson.titre;
                                        offer.value = offerJson.amount;
                                        offers.all.push(offer);
                                    }
                                    order.offers = offers;
                                }
                            } else {
                                let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
                                if (equipment && equipment.type === "articlenumerique") {
                                    order.offers = Utils.computeOffer(order, equipment);
                                }
                                if (!$scope.isAdministrator()) {
                                    const orderClient: OrderClient = Mix.castAs(OrderClient, JSON.parse(order.order_parent));
                                    (orderClient.status == "RESUBMIT") ? order.status = orderClient.status : null;
                                }
                            }
                        }
                    }
                    projects.all.find(project => project.id == idProject).orders = orders;
                }
            }
        }

        function beautifyProjectsFromOrders(projets: Projects, projectWithOrders: Projects) {
            for (const project of projets.all) {
                if (project.orders && project.orders.length > 0) {
                    project.total = currencyFormatter.format(Number(calculateTotalRegion(project.orders, 2)));
                    project.amount = calculateAmountRegion(project.orders);
                    const firstOrder: OrderRegion = project.orders[0];
                    project.creation_date = firstOrder.creation_date.toString();
                    Utils.setStatus(project, project.orders);
                    project.campaign_name = firstOrder.campaign_name;
                    const structure = $scope.structures.all.find(structure => firstOrder.id_structure == structure.id);
                    if (structure) {
                        project.uai = structure.uai;
                        project.structure_name = structure.name;
                    }
                    project.expanded = project.orders.length <= 500;
                    projectWithOrders.all.push(project);
                }
            }
        }

        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});

        const calculateTotalRegion = (orders: OrdersRegion, roundNumber: number) => {
            let totalPrice = 0;
            orders.forEach(order => {
                let price;
                if (typeof order.price === 'string') {
                    price = parseFloat(order.price);
                } else {
                    price = order.price
                }
                totalPrice += price;
            });
            return totalPrice.toFixed(roundNumber);
        };

        const calculateAmountRegion = (orders: OrdersRegion) => {
            let totalAmount = 0;
            orders.forEach(order => {
                totalAmount += order.amount;
            });
            return totalAmount;
        };

        $scope.closeWaitingAdminLightbox = async () => {
            $scope.display.lightbox.waitingAdmin = false;
            if ($scope.display.allOrdersSelected || !$scope.display.projects.hasSelectedOrders()) {
                $scope.display.allOrdersSelected = false;
                await $scope.launchSearch($scope.display.projects, false, false);
            }
            Utils.safeApply($scope);
        };

        $scope.switchAllOrdersOfProject = (project) => {
            project.orders.forEach(order => {
                order.selected = project.selected;
            });
            switchDisplayToggle();
        };

        $scope.checkParentSwitch = (project) => {
            let all = true;
            project.orders.forEach(order => {
                if (!order.selected)
                    all = order.selected;
            });
            project.selected = all;
            switchDisplayToggle();
        };

        const switchDisplayToggle = () => {
            let orderSelected = false
            $scope.display.projects.all.forEach(project => {
                if (project.orders.some(order => order.selected)) {
                    orderSelected = true;
                }
            });
            $scope.display.toggle = $scope.display.projects.all.some(project => project.selected) || orderSelected;
            Utils.safeApply($scope);
        };

        $scope.orderByProjectField = ORDER_BY_PROJECT_FIELD_ENUM;

        $scope.isOrderBy = (orderByProjectField: ORDER_BY_PROJECT_FIELD_ENUM): boolean => {
            return $scope.projectFilter.orderBy === orderByProjectField;
        }

        $scope.switchOrderDesc = (): void => {
            $scope.projectFilter.orderDesc = !$scope.projectFilter.orderDesc;
            $scope.projectFilter.page = 0;
            $scope.launchSearch($scope.display.projects, false, false)
                .then(() => Utils.safeApply($scope))
                .catch(err => console.error(err));
        }

        $scope.switchOrderBy = (orderByProjectField: ORDER_BY_PROJECT_FIELD_ENUM, defaultOrderDesc: boolean): void => {
            $scope.projectFilter.orderBy = orderByProjectField;
            $scope.projectFilter.orderDesc = (defaultOrderDesc != null) ? defaultOrderDesc : false;
            $scope.projectFilter.page = 0;
            $scope.launchSearch($scope.display.projects, false, false)
                .then(() => Utils.safeApply($scope))
                .catch(err => console.error(err));
        }

        $scope.clickOnTableColumnHeader = (orderByProjectField: ORDER_BY_PROJECT_FIELD_ENUM, defaultOrderDesc: boolean): void => {
            initProjects();
            if ($scope.isOrderBy(orderByProjectField)) {
                $scope.switchOrderDesc();
            } else {
                $scope.switchOrderBy(orderByProjectField, defaultOrderDesc);
            }
        }

        const checkStatusChoice = (status : ORDER_STATUS_ENUM) : boolean => {
            return $scope.projectFilter.statusFilterList.filter((state:StatusFilter) => state.getValue() === status).length > 0;
        };

        $scope.containsSentOrDoneOrders = () : boolean => {
            return checkStatusChoice(ORDER_STATUS_ENUM.SENT) || checkStatusChoice(ORDER_STATUS_ENUM.DONE);
        };

        await init();
    }
    ]);