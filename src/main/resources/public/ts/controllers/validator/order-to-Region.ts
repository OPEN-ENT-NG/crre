import {moment, ng, toasts} from 'entcore';
import {
    OrderRegion,
    Utils,
    Offers,
    Offer, StructureGroups, Structures, Filters, FiltersFront
} from "../../model";
import http from "axios";
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', ($scope) => {
        $scope.filters = new Filters();
        $scope.filtersFront = new FiltersFront();
        $scope.structure_groups = new StructureGroups();
        $scope.structures = new Structures();
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.display = {
            toggle : false,
            loading : true
        };
        $scope.filter = {
            page: 0
        };
        $scope.isDate = false;
        $scope.projects = {
            all : []
        };

        $scope.closeWaitingAdminLightbox= () =>{
            $scope.display.lightbox.waitingAdmin = false;
            Utils.safeApply($scope);
        };

        $scope.onScroll = async (init?:boolean, old = false): Promise<void> => {
            let data;
            if(init){
                data = await filter_order(true, old);
                Utils.safeApply($scope);
            }else{
                $scope.filter.page++;
                data = await filter_order(false, old);
                Utils.safeApply($scope);
            }
            if(data.length > 0){
                await $scope.synchroRegionOrders(true, data, old);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            }
        };

        $scope.exportCSV = (old = false) => {
            let selectedOrders = $scope.extractSelectedOrders();
            let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
            let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
            let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
            let url = `/crre/region/orders/`;
            if(old) {
                url += `old/`;
            }
            url += `exports?${params_id_order}&${params_id_equipment}&${params_id_structure}`;
            window.location = url;
            $scope.uncheckAll();
        }

        $scope.extractSelectedOrders = (select: boolean = false) =>{
            let selectedOrders = [];
            let allOrders = [];
            $scope.projects.all.forEach(project => {
                project.orders.forEach(async order => {
                    if (order.selected) {
                        selectedOrders.push(order);
                    }
                    allOrders.push(order);
                });
            });
            if (selectedOrders.length == 0 && !select) {
                selectedOrders = allOrders;
            }
            return selectedOrders;
        }

        $scope.searchProjectAndOrders = async (old = false) => {
            const data = await filter_order(true, old);
            if (data.length > 0) {
                await $scope.synchroRegionOrders(true, data, old);
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
                $scope.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(false, null, true);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                await $scope.searchProjectAndOrders(old);
            }
        }

        $scope.filterByDate = async (old = false) => {
            if($scope.isDate) {
                if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.searchByName($scope.query_name, old);
                } else {
                    toasts.warning('crre.date.err');
                }
                $scope.isDate = false;
            } else {
                $scope.isDate = true;
            }
        }

        const filter_order = async (initProject?: boolean, old = false) => {
            function prepareParams() {
                let params = "";
                const format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
                const word = $scope.query_name;
                const filters = $scope.filters.all;
                filters.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
                const startDate = moment($scope.filtersDate.startDate).format('YYYY-MM-DD').toString();
                const endDate = moment($scope.filtersDate.endDate).format('YYYY-MM-DD').toString();
                if (initProject) {
                    $scope.projects.all = [];
                    $scope.filter.page = 0;
                    $scope.display.loading = true;
                    Utils.safeApply($scope);
                }
                const page: string = `page=${$scope.filter.page}`;
                params += page;
                return {params, format, word, startDate, endDate};
            }

            function prepareUrl(startDate, endDate, word, params: string) {
                let url = `/crre/ordersRegion/projects/`;
                if (old) {
                    url += `old/`
                }
                url += `search_filter?startDate=${startDate}&endDate=${endDate}&${params}`;
                if (!!word) {
                    url += `&q=${word}`;
                }
                return url;
            }

            try {
                let {params, format, word, startDate, endDate} = prepareParams();
                if (!format.test(word)) {
                    let url = prepareUrl(startDate, endDate, word, params);
                    const {data} = await http.get(url);
                    return data;
                } else {
                    toasts.warning('crre.equipment.special');
                }
            } catch (e) {
                toasts.warning('crre.equipment.sync.err');
                throw e;
            }
        }

        const calculateTotalRegion = (orders: OrderRegion[], roundNumber: number) => {
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

        const calculateAmountRegion = (orders: OrderRegion[]) => {
            let totalAmount = 0;
            orders.forEach(order => {
                totalAmount += order.amount;
            });
            return totalAmount;
        };

        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});

        const getProjects = async(old = false) => {
            try {
                const startDate = moment($scope.filtersDate.startDate).format('YYYY-MM-DD').toString();
                const endDate = moment($scope.filtersDate.endDate).format('YYYY-MM-DD').toString();
                const page: string = $scope.filter.page ? `page=${$scope.filter.page}&` : '';
                const filterRejectedSentOrders = !$scope.selectedType.split('/').includes('historic');
                let url = `/crre/orderRegion/projects`;
                if(old) {
                    url += `/old`;
                }
                url += `?${page}startDate=${startDate}&endDate=${endDate}&filterRejectedSentOrders=${filterRejectedSentOrders}`;
                let { data } = await http.get(url);
                return data;
            } catch (e) {
                toasts.warning('crre.basket.sync.err');
            }
        }

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

        async function getOrdersOfProjects(isSearching: boolean, old: boolean, projects) {
            let projets;
            if (!isSearching) {
                if (old) {
                    projets = await getProjects(true);
                } else {
                    projets = await getProjects();
                }
            } else if (projects) {
                projets = projects;
            } else {
                projets = $scope.projects.all;
            }
            let params = '';
            projets.map((project) => {
                params += `project_id=${project.id}&`;
            });
            params = params.slice(0, -1);
            const filterRejectedSentOrders = !isSearching && !$scope.selectedType.split('/').includes('historic');
            let url = `/crre/ordersRegion/orders`;
            if (old) {
                url += `/old`;
            }
            url += `?${params}&filterRejectedSentOrders=${filterRejectedSentOrders}`;
            let promesses = [http.get(url)];
            if ($scope.structures.all.length == 0 && $scope.isAdministrator()) {
                promesses.push($scope.structures.sync());
            }
            const responses = await Promise.all(promesses);
            return {projets, responses};
        }

        async function filterAndBeautifyOrders(data, old: boolean, projets) {
            for (let orders of data) {
                if (orders.length > 0) {
                    const idProject = orders[0].id_project;
                    orders = orders.filter(filterAll);
                    if (!old) {
                        await getEquipments(orders).then(equipments => {
                            for (let order of orders) {
                                let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                                if (equipment.type === "articlenumerique") {
                                    order.offers = Utils.computeOffer(order, equipment);
                                }
                            }
                        });
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
                    projets.find(project => project.id == idProject).orders = orders;
                }
            }
        }

        function beautifyProjectsFromOrders(projets, projectWithOrders: any[]) {
            for (const project of projets) {
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
                    projectWithOrders.push(project);
                }
            }
        }

        $scope.synchroRegionOrders = async (isSearching: boolean = false, projects?, old = false): Promise<void> => {
            let {projets, responses} = await getOrdersOfProjects(isSearching, old, projects);
            if (responses[0]) {
                const data = responses[0].data;
                await filterAndBeautifyOrders(data, old, projets);
                let projectWithOrders = [];
                beautifyProjectsFromOrders(projets, projectWithOrders);

                if (!isSearching || projects) {
                    $scope.projects.all = $scope.projects.all.concat(projectWithOrders);
                } else {
                    $scope.projects.all = projectWithOrders;
                }

                $scope.display.loading = false;
                Utils.safeApply($scope);
            }
        };

        $scope.synchroRegionOrders();

        const getEquipments = (orders) :Promise <any> => {
            let params = '';
            orders.map((order) => {
                params += `order_id=${order.equipment_key}&`;
            });
            params = params.slice(0, -1);
            return http.get(`/crre/equipments?${params}`);
        }

        const switchDisplayToggle = () => {
            let orderSelected = false
            $scope.projects.all.forEach(project => {
                if (project.orders.some(order => order.selected)) {
                    orderSelected = true;
                }
            });
            $scope.display.toggle = $scope.projects.all.some(project => project.selected) || orderSelected;
            Utils.safeApply($scope);
        }

        $scope.switchAllOrdersOfProject = (project) => {
            project.orders.forEach(order => {
                order.selected = project.selected;
            });
            switchDisplayToggle();
        }

        $scope.checkParentSwitch = (project) => {
            let all = true;
            project.orders.forEach(order => {
                if (!order.selected)
                    all = order.selected;
            });
            project.selected = all;
            switchDisplayToggle();
        }

        $scope.uncheckAll = () => {
            $scope.projects.all.forEach(project => {
                project.selected = false;
                project.orders.forEach(async order => {
                    order.selected = false;
                });
            });
            $scope.display.toggle = false;
            Utils.safeApply($scope);
        }

        $scope.switchAllOrders = () => {
            $scope.projects.all.forEach(project => {
                project.selected = $scope.allOrdersSelected;
                project.orders.forEach(async order => {
                    order.selected = $scope.allOrdersSelected;
                });
            });
            $scope.display.toggle = $scope.allOrdersSelected;
            Utils.safeApply($scope);
        }
    }
    ]);