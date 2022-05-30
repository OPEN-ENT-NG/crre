import {_, ng, template, toasts} from 'entcore';
import {
    OrdersRegion,
    Utils,
    Filter,
    FilterFront,
} from "../../../model";

export const waitingOrderRegionController = ng.controller('waitingOrderRegionController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.filterChoice = {
                states : [],
                distributeurs : [],
                editors : [],
                schoolType : [],
                campaigns : [],
                docType : [],
                reassort : [],
                licence : [],
                id_structure : [],
            };
            $scope.filterChoiceCorrelation = {
                keys : ["docType","licence","campaigns", "schoolType", "editors", "distributeurs", "states", "id_structure"],
                states : 'status',
                distributeurs : 'distributeur',
                editors : 'editeur',
                schoolType : 'type',
                campaigns : 'id_campaign',
                docType : '_index',
                licence : 'licence',
                id_structure : "id_structure"
            };

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

            $scope.schoolType = [{name: 'PU'},{name:'PR'}];
            $scope.schoolType.forEach((item) => item.toString = () => $scope.translate(item.name));

            await $scope.campaigns.sync();
            $scope.campaigns.all.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.equipments.loading = true;
            $scope.equipments.all = [];
            Utils.safeApply($scope);
            await $scope.equipments.sync(true, undefined, undefined);
            Utils.safeApply($scope);
        };

        $scope.openFiltersLightbox= () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/filters');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.closeWaitingAdminLightbox= () =>{
            $scope.display.lightbox.waitingAdmin = false;
            Utils.safeApply($scope);
        };

        $scope.validateOrders = async () =>{
            let selectedOrders = new OrdersRegion();
            $scope.projects.all.forEach(project => {
                project.orders.forEach( async order => {
                    if(order.selected) {selectedOrders.all.push(order);}
                });
            });
            let {status} = await selectedOrders.updateStatus('VALID');
            if(status == 200){
                $scope.projects.all.forEach(project => {
                    project.orders.forEach( async order => {
                        if(order.selected) {order.status="VALID";}
                        order.selected = false;
                    });
                    project.selected =false;
                    Utils.setStatus(project, project.orders[0]);
                });
                toasts.confirm('crre.order.validated');
                $scope.display.toggle = false;
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.order.validated.error');
            }
        };

        $scope.generateLibraryOrder = async () => {
            let selectedOrders;
            if($scope.display.allOrdersSelected || !$scope.projects.hasSelectedOrders()) {
                await $scope.searchProjectAndOrders(false, true)
                selectedOrders = $scope.projects.extractAllOrders();
            } else {
                selectedOrders = $scope.projects.extractSelectedOrders();
            }
            let promesses = [await selectedOrders.updateStatus('SENT'),await selectedOrders.generateLibraryOrder()];
            let responses = await Promise.all(promesses);
            let statusOK = true;
            if (responses[0].status) {
                for (let i = 0; i < responses.length; i++) {
                    if (!(responses[i].status === 200)) {
                        statusOK = false;
                    }
                }
                if (statusOK) {
                    let selectedOrders = $scope.projects.extractSelectedOrders(true);
                    if(selectedOrders.length == 0) {
                        $scope.projects.all = [];
                    } else {
                        for(let i = $scope.projects.all.length - 1; i >= 0; i--) {
                            let project = $scope.projects.all[i];
                            for(let j = project.orders.length -1; j >= 0; j--) {
                                if (project.orders[j].selected) {
                                    project.orders.splice(j,1);
                                }
                            }
                            if(project.orders.length == 0) {
                                $scope.projects.all.splice(i, 1);
                            } else {
                                Utils.setStatus(project, project.orders[0]);
                            }
                        }
                    }
                    toasts.confirm('crre.order.region.library.create.message');
                    $scope.display.toggle = false;
                    Utils.safeApply($scope);
                    $scope.display.allOrdersSelected = false;
                    $scope.onScroll(true);
                } else {
                    toasts.warning('crre.order.region.library.create.err');
                }}
        };

        $scope.dropElement = (item,key): void => {
            $scope.filterChoice[key] = _.without($scope.filterChoice[key], item);
            $scope.getFilter();
        };

        $scope.getFilter = async () => {
            $scope.filters.all = [];
            $scope.filtersFront.all = [];
            for (const key of Object.keys($scope.filterChoice)) {
                let newFilterFront = new FilterFront();
                newFilterFront.name = $scope.filterChoiceCorrelation[key];
                newFilterFront.value = [];
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    if(key === "campaigns" || key === "id_structure"){
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
                await $scope.searchProjectAndOrders(false, false);
            } else {
                await $scope.searchByName($scope.query_name);
            }
        };

        $scope.openRefusingOrderLightbox= () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/refuse-order');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        };

        $scope.closeWaitingAdminLightbox= () =>{
            $scope.display.lightbox.waitingAdmin = false;
            Utils.safeApply($scope);
        };

        $scope.switchAllOrders = () => {
            $scope.projects.all.forEach(project => {
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
            $scope.projects.all.forEach(project => {
                if (!project.selected) {
                    testAllTrue = false;
                } else {
                    project.orders.forEach(order => {
                        if(!order.selected) {
                            testAllTrue = false;
                        }
                    })
                }
            });
            $scope.display.allOrdersSelected = testAllTrue;
            Utils.safeApply($scope);
        };

        $scope.exportCSVRegion = async (old: boolean, all: boolean): Promise<void> => {
            if (all) {
                await $scope.searchProjectAndOrders(false, true)
                $scope.projects.exportCSV(old, true);
            } else {
                $scope.projects.exportCSV(old, false);
            }
            $scope.display.allOrdersSelected = $scope.display.toggle = false;
            Utils.safeApply($scope);
        }

        this.init();
    }
    ]);