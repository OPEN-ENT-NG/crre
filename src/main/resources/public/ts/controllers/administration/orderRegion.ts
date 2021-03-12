import {idiom as lang, moment, ng, template, toasts} from 'entcore';
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
            $scope.reassorts = [{reassort: true, isChecked: false}, {reassort: false, isChecked: false}];
            $scope.states = [{status: "WAITING", isChecked: true}, {status: "IN PROGRESS", isChecked: true},
                {status: "VALID", isChecked: true}, {status: "REJECTED", isChecked: false},
                {status: "DONE", isChecked: true}];
            $scope.filters = new Filters();
            $scope.filtersFront = new FiltersFront();
            let newFilterFront = new FilterFront();
            newFilterFront.name = "status";
            newFilterFront.value = ["WAITING", "IN PROGRESS", "VALID", "DONE"];
            $scope.filtersFront.all.push(newFilterFront);
            $scope.states.forEach(state => {
                if(state.status != "REJECTED") {
                    let newFilter = new Filter();
                    newFilter.name = "status";
                    newFilter.value = state.status;
                    $scope.filters.all.push(newFilter);
                }
            });
            await $scope.campaigns.sync();
            $scope.equipments.loading = true;
            $scope.equipments.all = [];
            Utils.safeApply($scope);
            await $scope.equipments.sync(true, undefined, undefined );
            $scope.initPopUpFilters();
            Utils.safeApply($scope);
        };

        $scope.getProjects = async() => {
            try {
                const page: string = $scope.filter.page ? `?page=${$scope.filter.page}` : '';
                let { data } = await http.get(`/crre/orderRegion/projects${page}`);
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

        $scope.yourCustomFilter = function(order) {
            // order.filter(form => form.collab === true && form.owner_id != model.me.userId);

            if(($scope.reassorts[0].isChecked && $scope.reassorts[1].isChecked) || (!$scope.reassorts[0].isChecked && !$scope.reassorts[1].isChecked)) {
                return order;
            } else {
                if($scope.reassorts[0].isChecked) {
                    return order && order.reassort == true;
                } else if ($scope.reassorts[1].isChecked) {
                    return order && order.reassort == false;
                }
            }
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

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            const data = await $scope.filter_order();
            if(data.length > 0){
                await synchroRegionOrders(true, data);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                Utils.safeApply($scope);
            }
            Utils.safeApply($scope);
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
            let selectedOrders = [];
            let allOrders = [];
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {
                        selectedOrders.push(order);
                    }
                    allOrders.push(order);
                });
            });
            if(selectedOrders.length == 0)
                selectedOrders = allOrders;
            let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
            let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
            let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
            window.location = `/crre/region/orders/exports?${params_id_order}&${params_id_equipment}&${params_id_structure}`;
            $scope.uncheckAll();
        }

        $scope.generateLibraryOrder = async () => {
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
            if (selectedOrders.length == 0) {
                selectedOrders = allOrders;
            }
            let ordersToSend = new OrdersRegion();
            ordersToSend.all = Mix.castArrayAs(OrderRegion, selectedOrders);
            let {status} = await ordersToSend.updateStatus('SENT');
            if (status == 200) {
                $scope.projects.forEach((project, i) => {
                    project.orders.forEach(async (order, j) => {
                        if (order.selected) {
                            project.orders.splice(j,1);
                            order.status = "SENT";
                            order.selected = false;
                        }
                    });
                    if(project.orders.length == 0) {
                        $scope.projects.splice(i, 1);
                    } else {
                        Utils.setStatus(project, project.orders[0]);
                    }
                });
                let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
                let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
                let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
                await http.post(`/crre/region/orders/library?${params_id_order}&${params_id_equipment}&${params_id_structure}`);
                $scope.uncheckAll();
                Utils.safeApply($scope);
            }
        }

            $scope.getProjectsSearchFilter = async (name: string, startDate: Date, endDate: Date) => {
                try {
                    startDate = moment(startDate).format('YYYY-MM-DD').toString();
                    endDate = moment(endDate).format('YYYY-MM-DD').toString();
                    let {data} = await http.get(`/crre/ordersRegion/projects/search_filter?q=${name}&startDate=${startDate}&endDate=${endDate}`);
                    $scope.projects = data;
                } catch (e) {
                    toasts.warning('crre.basket.sync.err');
                }
            }

            $scope.getFilter = async (word: string, filter: string) => {
                let newFilter = new Filter();
                let newFilterFront = new FilterFront();
                newFilter.name = filter;
                newFilter.value = word;
                newFilterFront.name = filter;
                newFilterFront.value = [];
                if ($scope.filters.all.some(f => f.value === word)) {
                    $scope.filters.all.splice($scope.filters.all.findIndex(a => a.value === word), 1);
                    if ($scope.filtersFront.all.some(f => f.name === filter)) {
                        $scope.filtersFront.all.forEach(function (filt) {
                            if (filt.name === filter) {
                                filt.value.splice(filt.value.findIndex(a => a.value === word), 1);
                            }
                        })
                    }
                } else {
                    if ($scope.filtersFront.all.some(f => f.name === filter)) {
                        $scope.filtersFront.all.forEach(function (filt) {
                            if (filt.name === filter) {
                                filt.value.push(word)
                            }
                        })
                        $scope.filters.all.push(newFilter);
                    } else {
                        $scope.filters.all.push(newFilter);
                        newFilterFront.value.push(word);
                        $scope.filtersFront.all.push(newFilterFront);
                    }

                }
                if ($scope.filters.all.length > 0) {
                    const data = await $scope.filter_order(true);
                    await synchroRegionOrders(true, data);
                    Utils.safeApply($scope);
                } else {
                    await $scope.searchByName($scope.query_name);
                    Utils.safeApply($scope);
                }
            };

            $scope.searchByName = async (name: string) => {
                if (!!name) {
                    $scope.query_name = name;
                    const data = await $scope.filter_order(true);
                    await synchroRegionOrders(true, data);
                } else {
                    if ($scope.filters.all.length == 0) {
                        $scope.filter.page = 0;
                        $scope.loading = true;
                        $scope.projects = [];
                        Utils.safeApply($scope);
                        await synchroRegionOrders();
                    } else {
                        const data = await $scope.filter_order(true);
                        await synchroRegionOrders(true, data);
                    }

                }
            }

            $scope.filterByDate = async () => {
                if (moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    const data = await $scope.filter_order(true);
                    await synchroRegionOrders(true, data);
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

        $scope.initPopUpFilters = (filter?:string) => {
            let value = $scope.$eval(filter);
            $scope.showPopUpColumnsReassort = $scope.showPopUpColumnsSchool = $scope.showPopUpColumnsState = $scope.showPopUpColumnsDocumentsTypes = $scope.showPopUpColumnsCampaign = $scope.showPopUpColumnsEditor = $scope.showPopUpColumnsDiffusor = false;
            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsReassort': $scope.showPopUpColumnsReassort = true; break;
                    case 'showPopUpColumnsState': $scope.showPopUpColumnsState = true; break;
                    case 'showPopUpColumnsCampaign': $scope.showPopUpColumnsCampaign = true; break;
                    case 'showPopUpColumnsEditor': $scope.showPopUpColumnsEditor = true; break;
                    case 'showPopUpColumnsDiffusor': $scope.showPopUpColumnsDiffusor = true; break;
                    case 'showPopUpColumnsDocumentsTypes': $scope.showPopUpColumnsDocumentsTypes = true; break;
                    case 'showPopUpColumnsSchool': $scope.showPopUpColumnsSchool = true; break;
                    default: break;
                }
            }
        };

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
                let promesses = [http.get(`/crre/ordersRegion/orders?${params}&isFiltering=${isSearching}`)];
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