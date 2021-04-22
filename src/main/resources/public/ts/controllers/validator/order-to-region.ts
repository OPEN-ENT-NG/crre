import {moment, ng, toasts} from 'entcore';
import {
    Equipments, Filter,
    Filters,
    FiltersFront,
    Offer,
    Offers, OrdersClient,
    OrdersRegion,
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
        $scope.structure_groups = new StructureGroups();
        $scope.structures = new Structures();
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.filterChoice = {
            renew : []
        };
        $scope.filterChoiceCorrelation = {
            keys : ["renew"],
            renew : 'renew'
        };
        $scope.renews = [{name: 'true'}, {name: 'false'}];
        $scope.renews.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.display = {
            toggle : false,
            loading : true,
            allOrdersSelected : false,
            lightbox : {
                waitingAdmin : false
            }
        };
        $scope.filter = {
            page: 0,
            isDate: false,
        };
        if(!$scope.selectedType.split('/').includes('historic')) {
            $scope.current = {};
            $scope.current.structure = {};
            $scope.current.structure.id = null;
        }
        $scope.projects = new Projects();

        $scope.onScroll = async (init?:boolean, old?:boolean): Promise<void> => {
            let projets = new Projects();
            if(init){
                $scope.projects = new Projects();
                $scope.filter.page = 0;
                $scope.display.loading = true;
                Utils.safeApply($scope);
                await projets.filter_order(old,$scope.query_name, $scope.filters,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
                Utils.safeApply($scope);
            }else{
                $scope.filter.page++;
                await projets.filter_order(old,$scope.query_name, $scope.filters,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
                Utils.safeApply($scope);
            }
            if(projets.all.length > 0){
                await $scope.synchroRegionOrders(true, projets, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            }
            $scope.display.loading = false;
            Utils.safeApply($scope);
        };

        $scope.searchProjectAndOrders = async (old = false) => {
            $scope.projects = new Projects();
            let projets = new Projects();
            $scope.filter.page = 0;
            $scope.display.loading = true;
            Utils.safeApply($scope);
            await projets.filter_order(old,$scope.query_name, $scope.filters,
                $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id);
            if (projets.all.length > 0) {
                await $scope.synchroRegionOrders(true, projets, old);
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
                $scope.filter.page = 0;
                $scope.display.loading = true;
                $scope.projects = new Projects();
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(false, null, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                await $scope.searchProjectAndOrders(old);
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
        }

        $scope.getFilter = async () => {
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.projects = new Projects();
            let projets = new Projects();
            Utils.safeApply($scope);
            $scope.filters = new Filters();
            for (const key of Object.keys($scope.filterChoice)) {
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    newFilter.value = value;
                    $scope.filters.all.push(newFilter);
                });
            }
            if($scope.filters.all.length > 0) {
                await projets.filter_order(true,$scope.query_name, $scope.filters,
                    $scope.filtersDate.startDate, $scope.filtersDate.endDate, $scope.filter.page, $scope.current.structure.id)
                if (projets.all.length > 0) {
                    await $scope.synchroRegionOrders(true, projets, true);
                    $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                    Utils.safeApply($scope);
                } else {
                    $scope.display.loading = false;
                    Utils.safeApply($scope);
                }
            } else {
                if (!!$scope.query_name) {
                    $scope.searchByName(null, true)
                } else {
                    $scope.searchByName($scope.query_name, true)
                }
            }
        };

        const calculateTotalRegion = (orders: OrdersRegion, roundNumber: number) => {
            let totalPrice = 0;
            orders.forEach(order => {
                let price;
                if(typeof order.price === 'string') {
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
            while(test && $scope.filtersFront.all.length > i) {
                if($scope.filtersFront.all[i].name === "type")
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
                await projets.get(old,$scope.filtersDate.startDate, $scope.filtersDate.endDate,
                    !$scope.selectedType.split('/').includes('historic'), $scope.filter.page, $scope.current.structure.id);
            } else {
                projets = (projects) ? projects : $scope.projects;
            }
            let promesses = [await new OrdersRegion().getOrdersFromProjects(projets,
                !isSearching && !$scope.selectedType.split('/').includes('historic'), old)];
            if ($scope.structures.all.length == 0 && $scope.isAdministrator()) {
                promesses.push($scope.structures.sync());
            }
            const responses = await Promise.all(promesses);
            return {projets, responses};
        }

        async function filterAndBeautifyOrders(data, old: boolean, projets : Projects) {
            for (let orders of data) {
                if (orders.length > 0) {
                    const idProject = orders[0].id_project;
                    orders = orders.filter(filterAll);
                    if (!old) {
                        let equipments = new Equipments();
                        await equipments.getEquipments(orders);
                        for (let order of orders) {
                            let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
                            if (equipment.type === "articlenumerique") {
                                order.offers = Utils.computeOffer(order, equipment);
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
                    const firstOrder = project.orders[0];
                    project.creation_date = firstOrder.creation_date;
                    Utils.setStatus(project, firstOrder);
                    project.campaign_name = firstOrder.campaign_name;
                    const structure = $scope.structures.all.find(structure => firstOrder.id_structure == structure.id);
                    if (structure) {
                        project.uai = structure.uai;
                        project.structure_name = structure.name;
                    }
                    projectWithOrders.all.push(project);
                }
            }
        }

        $scope.synchroRegionOrders = async (isSearching: boolean = false, projects?: Projects, old = false, page?: number): Promise<void> => {
            if(page == 0) {
                $scope.filter.page = page;
            }
            let {projets, responses} = await getOrdersOfProjects(isSearching, old, projects);
            if (responses[0]) {
                const data = responses[0].data;
                await filterAndBeautifyOrders(data, old, projets);
                let projectWithOrders = new Projects();
                beautifyProjectsFromOrders(projets, projectWithOrders);
                if (!isSearching || projects) {
                    $scope.projects.all = $scope.projects.all.concat(projectWithOrders.all);
                } else {
                    $scope.projects = projectWithOrders;
                }
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