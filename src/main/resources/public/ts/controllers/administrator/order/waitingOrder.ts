import {_, angular, ng, template, toasts} from 'entcore';
import {
    OrdersRegion,
    Utils,
    Filter,
    FilterFront, Projects, Project, OrderRegion,
} from "../../../model";
import {ORDER_STATUS_ENUM} from "../../../enum/order-status-enum";

export const waitingOrderRegionController = ng.controller('waitingOrderRegionController',
    ['$scope', async ($scope) => {
        const init = async () => {
            $scope.filterChoice = {
                states: [],
                distributeurs: [],
                editors: [],
                schoolType: [],
                campaigns: [],
                docType: [],
                reassort: [],
                licence: [],
                id_structure: [],
            };
            $scope.filterChoiceCorrelation = {
                keys: ["docType", "licence", "campaigns", "schoolType", "editors", "distributeurs", "states", "id_structure"],
                states: 'status',
                distributeurs: 'distributeur',
                editors: 'editeur',
                schoolType: 'type',
                campaigns: 'id_campaign',
                docType: '_index',
                licence: 'licence',
                id_structure: "id_structure"
            };

            $scope.states = [{status: ORDER_STATUS_ENUM.SENT}, {status: ORDER_STATUS_ENUM.IN_PROGRESS}, {status: ORDER_STATUS_ENUM.VALID}, {status: ORDER_STATUS_ENUM.DONE}, {status: ORDER_STATUS_ENUM.REJECTED}];
            $scope.states.forEach((item) => item.toString = () => {
                if (item.status === ORDER_STATUS_ENUM.IN_PROGRESS) {
                    return $scope.translate("NEW")
                } else {
                    return $scope.translate(item.status)
                }
            });

            $scope.filterChoice.states = $scope.states.filter(state => state.status == ORDER_STATUS_ENUM.VALID || state.status == ORDER_STATUS_ENUM.IN_PROGRESS);

            $scope.filterChoice.states.forEach(state => {
                let newFilter = new Filter();
                newFilter.name = "status";
                newFilter.value = state.status;
                $scope.filters.all.push(newFilter);
            });
            let newFilterFront = new FilterFront();
            newFilterFront.name = "status";
            newFilterFront.value = ["WAITING", "IN_PROGRESS", "VALID", "DONE"];
            $scope.filtersFront.all.push(newFilterFront);

            $scope.schoolType = [{name: 'PU'}, {name: 'PR'}];
            $scope.schoolType.forEach((item) => item.toString = () => $scope.translate(item.name));

            await $scope.campaigns.sync();
            $scope.campaigns.all.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.equipments.loading = true;
            $scope.equipments.all = [];
            Utils.safeApply($scope);
            await $scope.equipments.sync(true, undefined, undefined);
            Utils.safeApply($scope);
        };

        $scope.openFiltersLightbox = () => {
            template.open('lightbox.waitingAdmin', 'administrator/order/filters');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.openConfirmGenerateLibraryLightbox = async (): Promise<void> => {
            if ($scope.display.allOrdersSelected || !$scope.display.projects.hasSelectedOrders()) {
                $scope.display.projects.all = [];
                $scope.display.loading = true;
                await $scope.searchProjectAndOrders(false, true, true)
            }
            template.open('lightbox.waitingAdmin', 'administrator/order/confirm-generate-library');
            $scope.display.lightbox.waitingAdmin = true;
            Utils.safeApply($scope);
        }

        $scope.closeWaitingAdminLightbox = () => {
            $scope.display.lightbox.waitingAdmin = false;
            if ($scope.display.allOrdersSelected || !$scope.display.projects.hasSelectedOrders()) {
                $scope.display.allOrdersSelected = false;
                $scope.onScroll(true);
            }
            Utils.safeApply($scope);
        };

        $scope.validateOrders = async (): Promise<void> => {
            $scope.display.loading = true;
            let selectedOrders: OrdersRegion = new OrdersRegion();
            $scope.display.projects.forEach((project: Project) => {
                project.orders.forEach(async (order: OrderRegion) => {
                    if (order.selected) {
                        selectedOrders.push(order);
                    }
                });
            });
            let projectsToShow: Projects = $scope.display.projects;
            $scope.display.projects = new Projects();
            Utils.safeApply($scope);
            let {status} = await selectedOrders.updateStatus('VALID');
            if (status == 200) {
                projectsToShow.forEach((project: Project) => {
                    project.orders.forEach(async (order: OrderRegion) => {
                        if (order.selected) {
                            order.status = "VALID";
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

        $scope.dropElement = (item, key): void => {
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
                    if (key === "campaigns" || key === "id_structure") {
                        value = item.id;
                    } else if (key === "states") {
                        value = item.status;
                    }
                    newFilter.value = value;
                    newFilterFront.value.push(value);
                    $scope.filters.all.push(newFilter);
                });
                $scope.filtersFront.all.push(newFilterFront);
            }
            if ($scope.filters.all.length > 0) {
                await $scope.searchProjectAndOrders(false, false, false);
            } else {
                await $scope.searchByName($scope.query_name);
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

        $scope.exportCSVRegion = async (old: boolean, all: boolean): Promise<void> => {
            $scope.display.allOrdersSelected = $scope.display.toggle = false;
            if (all) {
                await $scope.searchProjectAndOrders(old, true, true)
                $scope.display.projects.exportCSV(old, true);
            } else {
                $scope.display.projects.exportCSV(old, false);
            }
            Utils.safeApply($scope);
        }

        await init();
    }
    ]);