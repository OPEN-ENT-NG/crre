import {Behaviours, moment, ng} from 'entcore';
import {
    Equipments,
    Filters,
    FiltersFront,
    Offer,
    Offers,
    OrderClient,
    OrderRegion,
    OrdersRegion,
    Project,
    Projects,
    Structure,
    Structures,
    Utils
} from "../../model";
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";
import {Mix} from "entcore-toolkit";
import {ProjectFilter} from "../../model/ProjectFilter";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";
import {StatusFilter} from "../../model/StatusFilter";
import {TypeCatalogEnum} from "../../enum/type-catalog-enum";

export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', async ($scope) => {
        $scope.filters = new Filters();
        $scope.structures = new Structures();
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-6, 'months')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.display = {
            toggle: false,
            loading: true,
            allOrdersSelected: false,
            lightbox: {
                waitingAdmin: false
            },
            projects: new Projects()
        };
        $scope.filter = {
            page: 0,
            isDate: false,
        };

        $scope.projectFilter = new ProjectFilter();

        if (!$scope.selectedType.split('/').includes('historic')) {
            $scope.current = {};
            $scope.current.structure = {};
            $scope.current.structure.id = null;
        }


        function initProjects() {
            $scope.display.projects = new Projects();
            $scope.display.loading = true;
            Utils.safeApply($scope);
        }

        $scope.onScroll = async (old?: boolean): Promise<void> => {
            $scope.display.loading = true;
            Utils.safeApply($scope);
            let projects = new Projects();
            $scope.projectFilter.structureList.push($scope.current.structure as Structure);
            if($scope.display.projects.all.length > 0) {
                $scope.projectFilter.page++;
            }
            projects.search($scope.projectFilter)
                .then(async (data: Project[]) => {
                    if (data && data.length > 0) {
                        await $scope.synchroRegionOrders(true, false, projects, old);
                        Behaviours.applicationsBehaviours['crre'].InfiniteScrollService
                            .updateScroll();
                        $scope.syncSelected();
                    }
                    $scope.display.loading = false;
                    Utils.safeApply($scope);
                });
         };

        $scope.searchProjectAndOrders = async (old: boolean, all: boolean, onlyId: boolean) => {
            let projects = new Projects();
            initProjects();
            all ? $scope.projectFilter.page = null : 0;
            $scope.projectFilter.structureList.push($scope.current.structure as Structure);
            projects.search($scope.projectFilter)
                .then(async (data: Project[]) => {
                    if (data && data.length > 0) {
                        await $scope.synchroRegionOrders(true, onlyId, projects, old);
                    }
                    $scope.display.loading = false;
                    Utils.safeApply($scope);
                });

        }

        $scope.search = async (old = false) => {
            $scope.projectFilter.page = 0;
            if ($scope.filters.all.length == 0 && !$scope.projectFilter.queryName) {
                initProjects();
                await $scope.synchroRegionOrders(false, false, null, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                if ($scope.filtersDate.startDate && $scope.filtersDate.endDate &&
                    moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchProjectAndOrders(old, false, false);
                }
            }
        }

        $scope.syncSelected = (): void => {
            $scope.display.projects.all.flatMap((project: Project) => {
                if ($scope.display.allOrdersSelected) {
                    project.selected = $scope.display.allOrdersSelected;
                } else if (project.selected == undefined) {
                    project.selected = false;
                }
                return project.orders;
            }).forEach((order: OrderRegion) => {
                if ($scope.display.allOrdersSelected) {
                    order.selected = $scope.display.allOrdersSelected;
                } else if (order.selected == undefined) {
                    order.selected = false;
                }
            })
        };

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

        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});

        async function getOrdersOfProjects(isSearching: boolean, old: boolean, projectsResult: Projects) {
            let projects = new Projects();
            if (!isSearching) {
                $scope.projectFilter.structureList.push($scope.current.structure as Structure);
                await projects.search($scope.projectFilter);
            } else {
                projects = (projectsResult) ? projectsResult : $scope.display.projects;
            }
            let promises = [];
            let projectsSplit = new Projects();
            projects.forEach(projet => {
                if (projet.count > 500) {
                    if (projectsSplit.all.length > 0) {
                        promises.push(new OrdersRegion().getOrdersFromProjects(projectsSplit,
                            $scope.projectFilter));
                        projectsSplit = new Projects();
                    }
                    projectsSplit.push(projet);
                    promises.push(new OrdersRegion().getOrdersFromProjects(projectsSplit,
                        $scope.projectFilter));
                    projectsSplit = new Projects();
                } else if (projectsSplit.all.length > 100) {
                    promises.push(new OrdersRegion().getOrdersFromProjects(projectsSplit,
                        $scope.projectFilter));
                    projectsSplit = new Projects();
                    projectsSplit.push(projet);
                } else {
                    projectsSplit.push(projet);
                }
            });
            if (projectsSplit.all.length > 0) {
                promises.push(new OrdersRegion().getOrdersFromProjects(projectsSplit,
                    $scope.projectFilter));
            }
            if ($scope.structures.all.length == 0 && $scope.isAdministrator()) {
                $scope.structures.sync();
            }
            const responses = await Promise.all(promises);
            return {projects, responses};
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
                            let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
                            order.equipment = equipment;
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
                                if (equipment && equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
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
                    project.expanded = project.orders.length <= 500;
                    projectWithOrders.all.push(project);
                }
            }
        }

        $scope.synchroRegionOrders = async (isSearching: boolean = false, onlyId: boolean = false, projectsList: Projects,
                                            old: boolean): Promise<void> => {
            let {projects, responses} = await getOrdersOfProjects(isSearching, old, projectsList);
            if (responses[0]) {
                let projectsResult = [];
                responses.forEach(response => {
                    projectsResult = projectsResult.concat(response.data);
                });
                await filterAndBeautifyOrders(projectsResult, projects, onlyId);
                let projectWithOrders = new Projects();
                beautifyProjectsFromOrders(projects, projectWithOrders);
                if ((!isSearching || projects)) {
                    $scope.display.projects.all = $scope.display.projects.all.concat(projectWithOrders.all);
                } else {
                    $scope.display.projects = projectWithOrders;
                }
                $scope.display.loading = false;
                Utils.safeApply($scope);
            } else {
                $scope.display.projects.all = [];
                $scope.display.loading = false;
                Utils.safeApply($scope);
            }
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

        initProjects();

    }
    ]);