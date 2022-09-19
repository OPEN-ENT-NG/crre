import {moment, ng, toasts} from 'entcore';
import {
    Equipments,
    Filters,
    FiltersFront,
    Offer,
    Offers, OrderRegion,
    OrdersRegion, Project,
    Projects,
    StructureGroups,
    Structures,
    Utils
} from "../../model";
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";

export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', ($scope) => {
        $scope.filters = new Filters();
        $scope.filtersFront = new FiltersFront();
        $scope.structures = new Structures();
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.display = {
            toggle: false,
            loading: true,
            allOrdersSelected: false,
            lightbox: {
                waitingAdmin: false
            }
        };
        $scope.filter = {
            page: 0,
            isDate: false,
        };
        if (!$scope.selectedType.split('/').includes('historic')) {
            $scope.current = {};
            $scope.current.structure = {};
            $scope.current.structure.id = null;
        }
        $scope.projects = new Projects();

        function initProjects() {
            $scope.projects = new Projects();
            $scope.filter.page = 0;
            $scope.display.loading = true;
            Utils.safeApply($scope);
        }

        $scope.onScroll = async (init?: boolean, old?: boolean): Promise<void> => {
            let projets = new Projects();
            if (init) {
                initProjects();
                await projets.filter_order(old, $scope.query_name, $scope.filters, $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
                Utils.safeApply($scope);
            } else {
                $scope.filter.page++;
                await projets.filter_order(old, $scope.query_name, $scope.filters, $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
                Utils.safeApply($scope);
            }
            if (projets.all.length > 0) {
                await $scope.synchroRegionOrders(true, false, projets, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            }
            $scope.display.loading = false;
            $scope.syncSelected();
            Utils.safeApply($scope);
        };

        $scope.searchProjectAndOrders = async (old = false, all: boolean, onlyId:boolean) => {
            let projets = new Projects();
            initProjects();
            all ? $scope.filter.page = null : 0;
            await projets.filter_order(old, $scope.query_name, $scope.filters,
                $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
            if (projets.all.length > 0) {
                await $scope.synchroRegionOrders(true, onlyId, projets, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                $scope.display.loading = false;
                Utils.safeApply($scope);
            }
        }

        $scope.searchByName = async (name: string, old = false) => {
            $scope.query_name = name;
            if ($scope.filters.all.length == 0 && !name) {
                initProjects();
                await $scope.synchroRegionOrders(false, false,null, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                await $scope.searchProjectAndOrders(old, false, false);
            }
        }

        $scope.filterByDate = async (old = false) => {
            if ($scope.filter.isDate) {
                if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchByName($scope.query_name, old);
                } else {
                    toasts.warning('crre.date.err');
                }
                $scope.filter.isDate = false;
            } else {
                $scope.filter.isDate = true;
            }
        };

        $scope.syncSelected = (): void => {
            $scope.projects.all.forEach(project => {
                project.selected = $scope.display.allOrdersSelected;
                project.orders.forEach(order => {
                    order.selected = $scope.display.allOrdersSelected;
                });
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

        const filterAll = function (order) {
            let test = true;
            let i = 0
            while (test && $scope.filtersFront.all.length > i) {
                if ($scope.filtersFront.all[i].name === "type")
                    i++;
                else {
                    let valuePropOrder = order[$scope.filtersFront.all[i].name];
                    for (let j = 0; j < $scope.filtersFront.all[i].value.length; j++) {
                        test = valuePropOrder === $scope.filtersFront.all[i].value[j];
                        if (test) {
                            break;
                        }
                    }
                    i++;
                }
            }
            return test;
        }

        async function getOrdersOfProjects(isSearching: boolean, old: boolean, projects: Projects) {
            let projets = new Projects();
            if (!isSearching) {
                await projets.get(old, $scope.filtersDate.startDate, $scope.filtersDate.endDate,
                    !$scope.selectedType.split('/').includes('historic'), $scope.filter.page, $scope.current.structure.id);
            } else {
                projets = (projects) ? projects : $scope.projects;
            }
            let promesses = [];
            let projetsSplit = new Projects();
            projets.forEach(projet => {
                if (projet.count > 500) {
                    if(projetsSplit.all.length > 0){
                        promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit,
                            !isSearching && !$scope.selectedType.split('/').includes('historic'), old));
                        projetsSplit = new Projects();
                    }
                    projetsSplit.push(projet);
                    promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit,
                        !isSearching && !$scope.selectedType.split('/').includes('historic'), old));
                    projetsSplit = new Projects();
                } else if (projetsSplit.all.length > 100) {
                    promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit,
                        !isSearching && !$scope.selectedType.split('/').includes('historic'), old));
                    projetsSplit = new Projects();
                    projetsSplit.push(projet);
                } else {
                    projetsSplit.push(projet);
                }
            });
            if(projetsSplit.all.length > 0){
                promesses.push(new OrdersRegion().getOrdersFromProjects(projetsSplit,
                    !isSearching && !$scope.selectedType.split('/').includes('historic'), old));
            }
            if ($scope.structures.all.length == 0 && $scope.isAdministrator()) {
                $scope.structures.sync($scope.structuresInRegroupement);
            }
            const responses = await Promise.all(promesses);
            return {projets, responses};
        }

        async function filterAndBeautifyOrders(data, old: boolean, projets: Projects, onlyId: boolean) {
            for (let orders of data) {
                if (orders.length > 0) {
                    const idProject = orders[0].id_project;
                    if (!old) {
                        orders = orders.filter(filterAll);
                        if (!onlyId) {
                            let equipments = new Equipments();
                            await equipments.getEquipments(orders);
                            for (let order of orders) {
                                let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
                                if (equipment && equipment.type === "articlenumerique") {
                                    order.offers = Utils.computeOffer(order, equipment);
                                }
                            }
                        }
                    } else {
                        for (let order of orders) {
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
                        }
                    }
                    projets.all.find(project => project.id == idProject).orders = orders;
                }
            }
        }

        function beautifyProjectsFromOrders(projets: Projects, projectWithOrders: Projects) {
            for (const project of projets.all) {
                if (project.orders && project.orders.length > 0) {
                    project.total = currencyFormatter.format(Number(calculateTotalRegion(project.orders, 2)));
                    project.amount = calculateAmountRegion(project.orders);
                    const firstOrder : OrderRegion = project.orders[0];
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

        $scope.synchroRegionOrders = async (isSearching: boolean = false, onlyId: boolean = false, projects?: Projects,
                                            old = false, page?: number): Promise<void> => {
            if (page == 0) {
                $scope.filter.page = page;
            }
            let {projets, responses} = await getOrdersOfProjects(isSearching, old, projects);
            if (responses[0]) {
                let data = [];
                responses.forEach(response => {
                    data = data.concat(response.data);
                });
                await filterAndBeautifyOrders(data, old, projets, onlyId);
                let projectWithOrders = new Projects();
                beautifyProjectsFromOrders(projets, projectWithOrders);
                if ((!isSearching || projects)) {
                    $scope.projects.all = $scope.projects.all.concat(projectWithOrders.all);
                } else {
                    $scope.projects = projectWithOrders;
                }
                $scope.display.loading = false;
                Utils.safeApply($scope);
            } else {
                $scope.projects.all = [];
                $scope.display.loading = false;
                Utils.safeApply($scope);
            }
        };

        $scope.synchroRegionOrders();

        const switchDisplayToggle = () => {
            let orderSelected = false
            $scope.projects.all.forEach(project => {
                if (project.orders.some(order => order.selected)) {
                    orderSelected = true;
                }
            });
            $scope.display.toggle = $scope.projects.all.some(project => project.selected) || orderSelected;
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
    }
    ]);