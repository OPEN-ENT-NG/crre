import {_, idiom as lang, moment, ng, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion,
    StructureGroups,
    Structures,
    Utils,
    Basket, Equipment, Filter, Filters, OrderClient, FilterFront, FiltersFront
} from "../../model";
import http from "axios";
import {Mix} from "entcore-toolkit";
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', ($scope) => {

        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.display = {
            lightbox: {
                validOrder: false,
            },
        };
        $scope.filtersDate = [];
        $scope.filtersDate.startDate = moment().add(-1, 'years')._d;
        $scope.filtersDate.endDate = moment()._d;
        $scope.translate = (key: string):string => lang.translate(key);
        $scope.displayToggle=false;
        $scope.filter = {
            page: 0
        };
        $scope.projects = [];
        $scope.loading = true;

        this.init = async () => {
            $scope.filters = new Filters();
            $scope.filtersFront = new FiltersFront();
            $scope.filterChoice = {
                states : [],
                distributeurs : [],
                editors : [],
                schools : [],
                campaigns : [],
                docType : [],
                reassort : [],
                licence : [],
            }
            $scope.filterChoiceCorrelation = {
                keys : ["docType","reassort","licence","campaigns", "schools", "editors", "distributeurs", "states"],
                states : 'status',
                distributeurs : 'distributeur',
                editors : 'editeur',
                schools : 'type',
                campaigns : 'id_campaign',
                docType : '_index',
                reassort : 'reassort',
                licence : 'licence'
            }
            if(!$scope.selectedType.split('/').includes('historic')){
                $scope.states = [{status: "WAITING"},{status: "IN PROGRESS"},{status: "VALID"},{status: "DONE"},{status: "REJECTED"}];
                $scope.states.forEach((item) => item.toString = () => {
                    if(item.status === "IN PROGRESS"){
                        return $scope.translate("NEW")
                    }else{
                        return $scope.translate(item.status)
                    }
                });
                $scope.states.forEach(state => {
                    if(state.status != "REJECTED")
                        $scope.filterChoice.states.push(state);
                })
                $scope.filterChoice.states.forEach(state => {
                    let newFilter = new Filter();
                    newFilter.name = "status";
                    newFilter.value = state.status;
                    $scope.filters.all.push(newFilter);
                });
                let newFilterFront = new FilterFront();
                newFilterFront.name = "status";
                newFilterFront.value = ["WAITING", "IN PROGRESS", "VALID", "DONE"];
                $scope.filtersFront.all.push(newFilterFront);

                $scope.reassorts = [{name: 'true'}, {name: 'false'}];
                $scope.reassorts.forEach((item) => item.toString = () => $scope.translate(item.name));

                $scope.schools = [{name: 'PU'},{name:'PR'}];
                $scope.schools.forEach((item) => item.toString = () => $scope.translate(item.name));

                await $scope.campaigns.sync();
                $scope.campaigns.all.forEach((item) => item.toString = () => $scope.translate(item.name));
                $scope.equipments.loading = true;
                $scope.equipments.all = [];
                Utils.safeApply($scope);
                await $scope.equipments.sync(true, undefined, undefined);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item,key): void => {
            $scope.filterChoice[key] = _.without($scope.filterChoice[key], item);
            $scope.getFilter();
        };

        $scope.getProjects = async() => {
            try {
                const page: string = $scope.filter.page ? `page=${$scope.filter.page}&` : '';
                const filterRejectedSentOrders = !$scope.selectedType.split('/').includes('historic');
                let { data } = await http.get(`/crre/orderRegion/projects?${page}filterRejectedSentOrders=${filterRejectedSentOrders}`);
                return data;
            } catch (e) {
                toasts.warning('crre.basket.sync.err');
            }
        }

        $scope.openFiltersLightbox= () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/filters');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.openRefusingOrderLightbox= () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/refuse-order');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.closeWaitingAdminLightbox= () =>{
            $scope.display.lightbox.waitingAdmin = false;
            Utils.safeApply($scope);
        };

        $scope.filterAll = function (order) {
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

        $scope.onScroll = async (init?:boolean): Promise<void> => {
            let data;
            if(init){
                data = await $scope.filter_order(true);
            }else{
                $scope.filter.page++;
                data = await $scope.filter_order(false);
            }
            if(data.length > 0){
                await synchroRegionOrders(true, data);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            }
        };

        $scope.confirmRefuseOrder= async (justification:string) =>{
            $scope.display.lightbox.waitingAdmin = false;
            template.close('lightbox.waitingAdmin');
            let selectedOrders = [];
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {
                        selectedOrders.push(order);
                    }
                });
            });
            let ordersToRefuse  = new OrdersRegion();
            ordersToRefuse.all = Mix.castArrayAs(OrderRegion, selectedOrders);
            let {status} = await ordersToRefuse.updateStatus('REJECTED', justification);
            if(status == 200){
                $scope.projects.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {
                            order.status="REJECTED";
                            order.selected = false;
                        }
                        project.selected =false;
                    });
                    Utils.setStatus(project, project.orders[0]);
                });
                toasts.confirm('crre.order.refused.succes');
                $scope.displayToggle = false;
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.refused.error');
            }
        };

        $scope.validateOrders = async () =>{
            let selectedOrders = [];
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {
                        selectedOrders.push(order);
                    }
                });
            });
            let ordersToValidate  = new OrdersRegion();
            ordersToValidate.all = Mix.castArrayAs(OrderClient, selectedOrders);
            let {status} = await ordersToValidate.updateStatus('VALID');
            if(status == 200){
                $scope.projects.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {
                            order.status="VALID";
                            order.selected = false;
                        }
                        project.selected =false;
                    });
                    Utils.setStatus(project, project.orders[0]);
                });
                toasts.confirm('crre.order.validated');
                $scope.displayToggle = false;
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.validated.error');
            }
        };

        $scope.exportCSV = () => {
            let selectedOrders = extractSelectedOrders();
            let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
            let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
            let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
            window.location = `/crre/region/orders/exports?${params_id_order}&${params_id_equipment}&${params_id_structure}`;
            $scope.uncheckAll();
        }

        function extractSelectedOrders(select: boolean = false) {
            let selectedOrders = [];
            let allOrders = [];
            $scope.projects.forEach(project => {
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

        $scope.generateLibraryOrder = async () => {
            let selectedOrders = extractSelectedOrders();
            let ordersToSend = new OrdersRegion();
            ordersToSend.all = Mix.castArrayAs(OrderRegion, selectedOrders);
            let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
            let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
            let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
            let promesses = []
            promesses.push(ordersToSend.updateStatus('SENT'));
            promesses.push(http.post(`/crre/region/orders/library?${params_id_order}&${params_id_equipment}&${params_id_structure}`));
            let responses = await Promise.all(promesses);
            let statusOK = true;
            if (responses[0].status) {
                for (let i = 0; i < responses.length; i++) {
                    if (!(responses[i].status === 200)) {
                        statusOK = false;
                    }
                }
                if (statusOK) {
                    let selectedOrders = extractSelectedOrders(true);
                    if(selectedOrders.length == 0) {
                        $scope.projects = [];
                    } else {
                        for(let i = $scope.projects.length - 1; i >= 0; i--) {
                            let project = $scope.projects[i];
                            for(let j = project.orders.length -1; j >= 0; j--) {
                                if (project.orders[j].selected) {
                                    project.orders.splice(j,1);
                                }
                            }
                            if(project.orders.length == 0) {
                                $scope.projects.splice(i, 1);
                            } else {
                                Utils.setStatus(project, project.orders[0]);
                            }
                        }
                    }
                    toasts.confirm('crre.order.region.library.create.message');
                    $scope.displayToggle = false;
                    Utils.safeApply($scope);
                    $scope.allOrdersSelected = false;
                    $scope.onScroll(true);
                } else {
                    toasts.warning('crre.order.region.library.create.err');
                }}
        }

        $scope.getFilter = async () => {
            $scope.filters = new Filters();
            $scope.filtersFront = new FiltersFront()
            for (const key of Object.keys($scope.filterChoice)) {
                let newFilterFront = new FilterFront();
                newFilterFront.name = $scope.filterChoiceCorrelation[key];
                newFilterFront.value = [];
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    if(key === "campaigns"){
                        value = item.id;
                    } else if(key === "states"){
                        value = item.status;
                    }
                    newFilter.value = value;
                    newFilterFront.value.push(value);
                    $scope.filters.all.push(newFilter);
                });
                $scope.filtersFront.all.push(newFilterFront);
            }
            if ($scope.filters.all.length > 0) {
                await searchProjectAndOrders();
            } else {
                await $scope.searchByName($scope.query_name);
            }
        };

        async function searchProjectAndOrders() {
            const data = await $scope.filter_order(true);
            if (data.length > 0) {
                await synchroRegionOrders(true, data);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                $scope.loading = false;
                Utils.safeApply($scope);
            }
        }

        $scope.searchByName = async (name: string) => {
            $scope.query_name = name;
            if ($scope.filters.all.length == 0 && !name) {
                $scope.filter.page = 0;
                $scope.loading = true;
                $scope.projects = [];
                Utils.safeApply($scope);
                await synchroRegionOrders();
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            } else {
                await searchProjectAndOrders();
            }
        }

        $scope.filterByDate = async () => {
            if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                await searchProjectAndOrders();
            } else {
                toasts.warning('crre.date.err');
            }
        }

        $scope.filter_order = async (initProject?: boolean) => {
            try {
                let params = "";
                const format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
                let url;
                const word = $scope.query_name;
                const filters = $scope.filters.all;
                filters.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
                const startDate = moment($scope.filtersDate.startDate).format('YYYY-MM-DD').toString();
                const endDate = moment($scope.filtersDate.endDate).format('YYYY-MM-DD').toString();
                if (initProject) {
                    $scope.projects = [];
                    $scope.filter.page = 0;
                    $scope.loading = true;
                    Utils.safeApply($scope);
                }
                const page: string = `page=${$scope.filter.page}`;
                params += page;
                if (!format.test(word)) {
                    if (!!word) {
                        url = `/crre/ordersRegion/projects/search_filter?startDate=${startDate}&endDate=${endDate}&q=${word}&${params}`;
                    } else {
                        url = `/crre/ordersRegion/projects/search_filter?startDate=${startDate}&endDate=${endDate}&${params}`;
                    }
                } else {
                    toasts.warning('crre.equipment.special');
                }
                const {data} = await http.get(url);
                return data;
            } catch (e) {
                toasts.warning('crre.equipment.sync.err');
                throw e;
            }
        }

        $scope.calculateTotalRegion = (orders: OrderRegion[], roundNumber: number) => {
            let totalPrice = 0;
            orders.forEach(order => {
                totalPrice += order.price;
            });
            return totalPrice.toFixed(roundNumber);
        };

        $scope.calculateAmountRegion = (orders: OrderRegion[]) => {
            let totalAmount = 0;
            orders.forEach(order => {
                totalAmount += order.amount;
            });
            return totalAmount;
        };

        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});

        const synchroRegionOrders = async (isSearching: boolean = false, projects?): Promise<void> => {
            let projets;
            if (!isSearching) {
                projets = await $scope.getProjects();
            } else if (projects) {
                projets = projects;
            } else {
                projets = $scope.projects;
            }
            let params = '';
            projets.map((project) => {
                params += `project_id=${project.id}&`;
            });
            params = params.slice(0, -1);
            const filterRejectedSentOrders = !isSearching && !$scope.selectedType.split('/').includes('historic');
            let promesses = [http.get(`/crre/ordersRegion/orders?${params}&filterRejectedSentOrders=${filterRejectedSentOrders}`)];
            if ($scope.structuresToDisplay.all.length == 0 && $scope.isAdministrator()) {
                promesses.push($scope.structuresToDisplay.sync());
            }
            const responses = await Promise.all(promesses);
            if (responses[0]) {
                const data = responses[0].data;
                for (let orders of data) {
                    if (orders.length > 0) {
                        const idProject = orders[0].id_project;
                        orders = orders.filter($scope.filterAll);
                        projets.find(project => project.id == idProject).orders = orders;
                    }
                }
                let projectWithOrders = [];
                for (const project of projets) {
                    if (project.orders && project.orders.length > 0) {
                        project.total = currencyFormatter.format($scope.calculateTotalRegion(project.orders, 2));
                        project.amount = $scope.calculateAmountRegion(project.orders);
                        const firstOrder = project.orders[0];
                        project.creation_date = firstOrder.creation_date;
                        Utils.setStatus(project, firstOrder);
                        project.campaign_name = firstOrder.campaign_name;
                        const structure = $scope.structuresToDisplay.all.find(structure => firstOrder.id_structure == structure.id);
                        if (structure) {
                            project.uai = structure.uai;
                            project.structure_name = structure.name;
                        }
                        projectWithOrders.push(project);
                    }
                }
                if (!isSearching || projects) {
                    $scope.projects = $scope.projects.concat(projectWithOrders);
                } else {
                    $scope.projects = projectWithOrders;
                }
                $scope.loading = false;
                Utils.safeApply($scope);
            }
        };
        synchroRegionOrders();

        $scope.cancelBasketDelete = (): void => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.createOrder = async (): Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            $scope.ordersClient.all.forEach(order => {
                let orderRegionTemp = new OrderRegion();
                orderRegionTemp.createFromOrderClient(order);
                ordersToCreate.all.push(orderRegionTemp);
            });

            let {status} = await ordersToCreate.create();
            if (status === 201) {
                toasts.confirm('crre.order.region.create.message');
            } else {
                toasts.warning('crre.admin.order.create.err');
            }
            Utils.safeApply($scope);
        }

        $scope.switchDisplayToggle = () => {
            let orderSelected = false
            $scope.projects.forEach(project => {
                if (project.orders.some(order => order.selected)) {
                    orderSelected = true;
                }
            });
            $scope.displayToggle = $scope.projects.some(project => project.selected) || orderSelected;
            Utils.safeApply($scope);
        }

        $scope.switchAllOrdersOfProject = (project) => {
            project.orders.forEach(order => {
                order.selected = project.selected;
            });
            $scope.switchDisplayToggle();
        }

        $scope.checkParentSwitch = (project) => {
            let all = true;
            project.orders.forEach(order => {
                if (!order.selected)
                    all = order.selected;
            });
            project.selected = all;
            $scope.switchDisplayToggle();
        }

        $scope.uncheckAll = () => {
            $scope.projects.forEach(project => {
                project.selected = false;
                project.orders.forEach(async order => {
                    order.selected = false;
                });
            });
            $scope.displayToggle = false;
            Utils.safeApply($scope);
        }

        $scope.switchAllOrders = () => {
            $scope.projects.forEach(project => {
                project.selected = $scope.allOrdersSelected;
                project.orders.forEach(async order => {
                    order.selected = $scope.allOrdersSelected;
                });
            });
            $scope.displayToggle = $scope.allOrdersSelected;
            Utils.safeApply($scope);
        }

        $scope.reSubmit = async () => {
            let statusOK = true;
            let totalAmount = 0;
            let promesses = []
            $scope.projects.forEach(project => {
                project.orders.forEach(async order => {
                    if (order.selected) {
                        let equipment = new Equipment();
                        equipment.ean = order.equipment_key;
                        let basket = new Basket(equipment, $scope.campaign.id, $scope.current.structure.id);
                        basket.amount = order.amount;
                        totalAmount += order.amount;
                        promesses.push(basket.create());
                    }
                });
            });
            let responses = await Promise.all(promesses);
            if (responses[0].status) {
                for (let i = 0; i < responses.length; i++) {
                    if (responses[i].status === 200) {
                        if ($scope.campaign.nb_panier)
                            $scope.campaign.nb_panier += 1;
                        else
                            $scope.campaign.nb_panier = 1;
                    } else {
                        statusOK = false;
                    }
                }
                if (statusOK) {
                    let messageForMany = totalAmount + ' ' + lang.translate('articles') + ' ' +
                        lang.translate('crre.basket.added.articles');
                    toasts.confirm(messageForMany);
                } else {
                    toasts.warning('crre.basket.added.articles.error')
                }
                $scope.uncheckAll();
                Utils.safeApply($scope);
            }
        };
        this.init();
    }
    ]);