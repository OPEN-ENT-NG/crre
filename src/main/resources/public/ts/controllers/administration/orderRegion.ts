import {idiom as lang, moment, ng, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion,
    Structure,
    StructureGroups,
    Structures,
    Utils,
    Equipments,
    Basket, Equipment, Filter, Filters, OrderClient
} from "../../model";
import http from "axios";
import {Mix} from "entcore-toolkit";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', ($scope) => {

        $scope.orderToCreate = new OrderRegion();
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

        this.init = () => {
            $scope.reassorts = [{reassort: true}, {reassort: false}];
            $scope.filters = new Filters();
            $scope.initPopUpFilters();
        };

        $scope.getProjects = async() => {
            try {
                let { data } = await http.get(`/crre/orderRegion/projects`);
                $scope.projects = data;
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
            ordersToRefuse.all = Mix.castArrayAs(OrderClient, selectedOrders);
            let {status} = await ordersToRefuse.updateStatus('REJECTED', justification);
            if(status == 200){
                this.init();
                await synchroRegionOrders();
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
                this.init();
                await synchroRegionOrders();
                toasts.confirm('crre.order.validated');
                $scope.displayToggle = false;
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.validated.error');
            }
        };

        $scope.exportCSV = () => {
            let selectedOrders = [];
            $scope.projects.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {
                        selectedOrders.push(order);
                    }
                });
            });
            let params_id_order = Utils.formatKeyToParameter(selectedOrders, 'id');
            let params_id_equipment = Utils.formatKeyToParameter(selectedOrders, "equipment_key");
            let params_id_structure = Utils.formatKeyToParameter(selectedOrders, "id_structure");
            window.location = `/crre/region/orders/exports?${params_id_order}&${params_id_equipment}&${params_id_structure}`;
            $scope.uncheckAll();
        }

        $scope.getProjectsSearchFilter = async(name: string, startDate: Date, endDate: Date) => {
            try {
                startDate = moment(startDate).format('YYYY-MM-DD').toString();
                endDate = moment(endDate).format('YYYY-MM-DD').toString();
                let { data } = await http.get(`/crre/ordersRegion/projects/search_filter?q=${name}&startDate=${startDate}&endDate=${endDate}`);
                $scope.projects = data;
            } catch (e) {
                toasts.warning('crre.basket.sync.err');
            }
        }

        $scope.getFilter = async (word: string, filter: string) => {
            let newFilter = new Filter();
            newFilter.name = filter;
            newFilter.value = word;
            if ($scope.filters.all.some(f => f.value === word)) {
                $scope.filters.all.splice($scope.filters.all.findIndex(a => a.value === word) , 1)
            } else {
                $scope.filters.all.push(newFilter);
            }
            if($scope.filters.all.length > 0) {
                if (!!$scope.query_name) {
                    await $scope.filter_order();
                    await synchroRegionOrders(true);
                    Utils.safeApply($scope);
                } else {
                    await $scope.filter_order();
                    await synchroRegionOrders(true);
                    Utils.safeApply($scope);
                }
            } else {
                await $scope.searchByName($scope.query_name);
                Utils.safeApply($scope);
            }
        };

        $scope.searchByName =  async (name: string) => {
            if(!!name) {
                if($scope.filters.all.length == 0) {
                    await $scope.getProjectsSearchFilter(name, $scope.filtersDate.startDate, $scope.filtersDate.endDate);
                    await synchroRegionOrders(true);
                    Utils.safeApply($scope);
                } else {
                    await $scope.filter_order();
                    await synchroRegionOrders(true);
                    Utils.safeApply($scope);
                }
            } else {
                if($scope.filters.all.length == 0) {
                    await synchroRegionOrders();
                    Utils.safeApply($scope);
                } else {
                    await $scope.filter_order();
                    await synchroRegionOrders(true);
                    Utils.safeApply($scope);
                }

            }
            Utils.safeApply($scope);
        }

        $scope.filterByDate = async () => {
                if(moment($scope.filtersDate.startDate).isSameOrBefore(moment($scope.filtersDate.endDate))) {
                    await $scope.filter_order();
                    await synchroRegionOrders(true);
                } else {
                    toasts.warning('crre.date.err');
                }
        }

        $scope.filter_order = async() => {
            try {
                let params = "";
                let format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
                let url;
                let word = $scope.query_name;
                let filters = $scope.filters.all;
                filters.forEach(function (f, index) {
                    params += f.name + "=" + f.value;
                    if(index != filters.length - 1) {
                        params += "&";
                    }});
                let startDate = moment($scope.filtersDate.startDate).format('YYYY-MM-DD').toString();
                let endDate = moment($scope.filtersDate.endDate).format('YYYY-MM-DD').toString();
                if(!format.test(word)) {
                    if (!!word) {
                        url = `/crre/ordersRegion/projects/search_filter?startDate=${startDate}&endDate=${endDate}&q=${word}&${params}`;
                    } else {
                        url = `/crre/ordersRegion/projects/search_filter?startDate=${startDate}&endDate=${endDate}&${params}`;
                    }
                } else {
                    toasts.warning('crre.equipment.special');
                }
                let { data } = await http.get(url);
                $scope.projects = data;
            } catch (e) {
                toasts.warning('crre.equipment.sync.err');
                throw e;
            }
        }

        $scope.initPopUpFilters = (filter?:string) => {
            let value = $scope.$eval(filter);
            $scope.showPopUpColumnsReassort = false;
            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsReassort': $scope.showPopUpColumnsReassort = true; break;
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

        const currencyFormatter = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

        function setStatus(project, firstOrder) {
            project.status = firstOrder.status;
            let partiallyRefused = false;
            let partiallyValided = false;
            if(project.orders.length > 1){
                for (const order of project.orders) {
                    if (project.status != order.status)
                        if (order.status == 'VALID' || project.status == 'VALID')
                            partiallyValided = true;
                        else if (order.status == 'REJECTED' || project.status == 'REJECTED')
                            partiallyRefused = true;
                }
                if (partiallyRefused || partiallyValided) {
                    for (const order of project.orders) {
                        order.displayStatus = true;
                    }
                    if (partiallyRefused && !partiallyValided)
                        project.status = "PARTIALLYREJECTED"
                    else
                        project.status = "PARTIALLYVALIDED"
                }
            }
        }

        const synchroRegionOrders = async (isSearching: boolean = false) : Promise<void> => {
            if(!isSearching) {
                await $scope.getProjects();
            }
            let params = '';
            $scope.projects.map((project) => {
                params += `project_id=${project.id}&`;
            });
            params = params.slice(0, -1);
            let promesses = [http.get(`/crre/ordersRegion/orders?${params}`)];
            if($scope.structuresToDisplay.all.length == 0 && $scope.isAdministrator()){
                promesses.push($scope.structuresToDisplay.sync());
            }
            const responses = await Promise.all(promesses);
            if(responses[0]){
                const data = responses[0].data;
                for (const orders of data) {
                    if (orders.length > 0) {
                        const idProject = orders[0].id_project
                        $scope.projects.find(project => project.id == idProject).orders = orders;
                    }
                }
                let projectWithOrders = [];
                for (const project of $scope.projects) {
                    if (project.orders && project.orders.length > 0) {
                        project.total = currencyFormatter.format($scope.calculateTotalRegion(project.orders, 2));
                        project.amount = $scope.calculateAmountRegion(project.orders);
                        const firstOrder = project.orders[0];
                        project.creation_date = firstOrder.creation_date;
                        setStatus(project, firstOrder);
                        project.campaign_name = firstOrder.campaign_name;
                        const structure = $scope.structuresToDisplay.all.find(structure => firstOrder.id_structure == structure.id);
                        if(structure) {
                            project.uai = structure.uai;
                            project.structure_name = structure.name;
                        }
                        projectWithOrders.push(project);
                    }
                }
                $scope.projects = projectWithOrders;
                Utils.safeApply($scope);
            }
        };
        synchroRegionOrders();

        $scope.cancelBasketDelete = ():void => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            $scope.ordersClient.all.forEach(order => {
                let orderRegionTemp = new OrderRegion();
                orderRegionTemp.createFromOrderClient(order);
                ordersToCreate.all.push(orderRegionTemp);
            });

            let {status} = await ordersToCreate.create();
            if (status === 201) {
                toasts.confirm('crre.order.region.create.message');
                $scope.orderToCreate = new OrderRegion();
            }
            else {
                toasts.warning('crre.admin.order.create.err');
            }
            Utils.safeApply($scope);
        }

        $scope.getTotalHistoric = () => {
            let total = 0;
            if($scope.projects) {
                $scope.projects.forEach(project => {
                    total += parseFloat(project.total.replace(/[^0-9.,-]+/g, "").replace(",","."));
                });
            }
            return total;
        }

        $scope.getTotalAmountHistoric = () => {
            let total = 0;
            if($scope.projects) {
                $scope.projects.forEach(project => {
                    total += parseFloat(project.amount);
                });
            }
            return total;
        }

        $scope.switchDisplayToggle = () => {
            let orderSelected = false
            $scope.projects.forEach(project => {
                if(project.orders.some(order => order.selected)){
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
                if(!order.selected)
                    all = order.selected;
            });
            project.selected = all;
            $scope.switchDisplayToggle();
        }

        $scope.uncheckAll = () => {
            $scope.projects.forEach(project => {
                project.selected = false;
                project.orders.forEach( async order => {
                    order.selected = false;
                });
            });
            $scope.displayToggle = false;
            Utils.safeApply($scope);
        }

        $scope.switchAllOrders = () => {
            $scope.projects.forEach(project => {
                project.selected = $scope.allOrdersSelected;
                project.orders.forEach( async order => {
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
                project.orders.forEach( async order => {
                    if(order.selected) {
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
            if(responses[0].status) {
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