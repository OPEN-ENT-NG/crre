import {ng, template, toasts} from 'entcore';
import {
    Campaign,
    OrderClient,
    OrderRegion,
    OrdersClient,
    OrderSearchAmountFilter,
    OrdersRegion,
    Utils
} from '../../model';
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";
import {UserModel} from "../../model/UserModel";
import {ValidatorOrderWaitingFilter} from "../../model/ValidatorOrderWaitingFilter";
import {CREDIT_TYPE_ENUM} from "../../enum/credit-type-enum";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";

export const waitingValidatorOrderController = ng.controller('waitingValidatorOrderController',
    ['$scope', async ($scope,) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.allOrdersSelected = false;
        $scope.filter = {
            page: 0
        };
        $scope.filterOrder = null as ValidatorOrderWaitingFilter;
        $scope.ordersDuplicated = [] as OrderClient[];

        const init = async () => {
            $scope.loading = true;
            $scope.users = [] as Array<UserModel>;
            await $scope.getAllFilters();

            await checkCampaignAccessibility();

            let distinctOrdersCampaign = $scope.allOrderCLient.all.reduce((acc, x) =>
                    acc.concat(acc.find(y => y.campaign.id === x.campaign.id) ? [] : [x])
                , []);

            $scope.type_campaign = [] as Array<Campaign>;
            distinctOrdersCampaign.forEach(order => {
                $scope.type_campaign.push(order.campaign);
            });
            $scope.type_campaign.forEach((item) => item.toString = () => item.name);

            await $scope.getAllAmount();
            $scope.allOrdersSelected = false;
            $scope.loading = false;
            Utils.safeApply($scope);
        };

        $scope.loadNextPage = async (): Promise<void> => {
            $scope.filter.page++;
            const newData = await $scope.ordersClient.searchOrder($scope.current.structure.id, $scope.filterOrder, false, $scope.filter.page);
            endLoading(newData);
            $scope.loading = false;

            $scope.syncSelected();
            Utils.safeApply($scope);
        };

        $scope.getAllFilters = async () => {
            $scope.users = await $scope.ordersClient.getUsers('WAITING', $scope.current.structure.id);
        };

        $scope.getAllAmount = async () => {
            let filter: OrderSearchAmountFilter = new OrderSearchAmountFilter();
            filter.status = [ORDER_STATUS_ENUM.WAITING];
            filter.idsCampaign = $scope.filterOrder.typeCampaignList.map((campaign: Campaign) => campaign.id);
            filter.idsUser = $scope.filterOrder.userList.map((user: UserModel) => user.id_user);
            filter.searchingText = $scope.filterOrder.queryName;
            let {startDate, endDate} = Utils.formatDate($scope.filterOrder.startDate, $scope.filterOrder.endDate);
            filter.startDate = startDate;
            filter.endDate = endDate;
            $scope.amountTotal = await $scope.ordersClient.calculateTotal($scope.current.structure.id, filter);
        }

        $scope.remainAvailable = () => {
            let ordersClient: OrdersClient = $scope.allOrdersSelected ? $scope.allOrderCLient : $scope.ordersClient;
            if ((ordersClient.selected.length <= 0 && !$scope.allOrdersSelected) || (ordersClient.all.length <= 0 && $scope.allOrdersSelected) || $scope.onlyCampaignInaccessible) return true

            let nbLicences = $scope.campaign.nb_licences_available ? $scope.campaign.nb_licences_available : 0;
            nbLicences += $scope.campaign.nb_licences_consumable_available ? $scope.campaign.nb_licences_consumable_available : 0;

            let purseAmount = $scope.campaign.purse_amount ? $scope.campaign.purse_amount : 0;
            let purseAmountConsumable = $scope.campaign.consumable_purse_amount ? $scope.campaign.consumable_purse_amount : 0;

            let isInavailable: boolean = false;

            if (ordersClient && ordersClient.selected && ordersClient.selected.length > 0) {
                isInavailable = ordersClient.all.length == 0 ||
                    nbLicences - ordersClient.calculTotalAmount() < 0 ||
                    purseAmount - ordersClient.calculTotalPriceTTC(false) < 0 ||
                    purseAmountConsumable - ordersClient.calculTotalPriceTTC(true) < 0;
            } else if ($scope.amountTotal && ordersClient) {
                isInavailable = ordersClient.all.length == 0 ||
                    nbLicences - ordersClient.calculTotalAmount() < 0 ||
                    purseAmount - $scope.amountTotal.priceCredit < 0 ||
                    purseAmountConsumable - $scope.amountTotal.priceConsumableCredit < 0;
            }

            return isInavailable;
        };

        $scope.checkValid = () => {
            let ordersClient: OrdersClient;
            if ($scope.allOrdersSelected) {
                $scope.allOrderCLient.all.forEach(order => {
                    for (let i = 0; i <= $scope.ordersClient.all.length; i++) {
                        if (order.id == $scope.ordersClient.all[i].id) {
                            order.amount = $scope.ordersClient.all[i].amount;
                            break
                        }
                    }
                });
                ordersClient = $scope.allOrderCLient;
            } else {
                ordersClient = $scope.ordersClient;
            }
            let isValid: boolean;
            if (ordersClient.selected.length > 0) {
                isValid = !ordersClient.selected.some(order => {
                    return !order.amount || order.amount <= 0;
                })
            } else {
                isValid = !ordersClient.all.some(order => {
                    return !order.amount || order.amount <= 0;
                })
            }
            return isValid;
        };

        const checkCampaignAccessibility = async (): Promise<void> => {
            $scope.allOrderCLient = new OrdersClient();
            await $scope.allOrderCLient.searchOrder($scope.current.structure.id, $scope.filterOrder, true);

            $scope.onlyCampaignInaccessible = $scope.allOrderCLient.all.find((order: OrderClient) => order.campaign.accessible) == null;
        }

        $scope.updateOrders = async (totalPrice: number, totalPriceConsumable: number, totalAmount: number, totalAmountConsumable: number,
                                     ordersToRemove: Array<OrderClient>, numberOrdered: number) => {
            $scope.campaign.nb_order_waiting -= numberOrdered;
            $scope.campaign.historic_etab_notification += numberOrdered;
            $scope.campaign.nb_licences_available -= totalAmount;
            $scope.campaign.nb_licences_consumable_available -= totalAmountConsumable;
            $scope.campaign.purse_amount -= totalPrice;
            $scope.campaign.consumable_purse_amount -= totalPriceConsumable;

            ordersToRemove.forEach(order => {
                $scope.ordersClient.all.forEach((orderClient, i) => {
                    if (orderClient.id == order.id) {
                        $scope.ordersClient.all.splice(i, 1);
                    }
                });
            });
            Utils.safeApply($scope);
        }

        function reformatOrders(ordersToReformat, ordersToCreate: OrdersRegion, ordersToRemove: OrdersClient,
                                totalPrice: number, totalPriceConsumable: number, totalAmount: number, totalAmountConsumable: number) {
            ordersToReformat.forEach(order => {
                if (order.campaign.accessible) {
                    let orderRegionTemp = new OrderRegion();
                    orderRegionTemp.createFromOrderClient(order);
                    ordersToCreate.all.push(orderRegionTemp);
                    ordersToRemove.all.push(order);
                    if (order.campaign.use_credit == CREDIT_TYPE_ENUM.CREDITS) {
                        totalPrice += order.price * order.amount;
                    } else if (order.campaign.use_credit == CREDIT_TYPE_ENUM.CONSUMABLE_CREDITS) {
                        totalPriceConsumable += order.price * order.amount;
                    } else if (order.campaign.use_credit == CREDIT_TYPE_ENUM.LICENCES) {
                        totalAmount += order.amount;
                    } else if (order.campaign.use_credit == CREDIT_TYPE_ENUM.CONSUMABLE_LICENCES) {
                        totalAmountConsumable += order.amount;
                    }
                }
            });
            return {totalPrice, totalPriceConsumable, totalAmount, totalAmountConsumable};
        }

        $scope.createOrder = async (comment: string): Promise<void> => {
            $scope.closeConfirmOrder();
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.RESUME);
            $scope.loading = true;
            let ordersToCreate = new OrdersRegion();
            let totalPrice = 0;
            let totalPriceConsumable = 0;
            let totalAmount = 0;
            let totalAmountConsumable = 0;
            let ordersToReformat;

            if ($scope.allOrdersSelected || $scope.ordersClient.selectedElements.length === 0) {
                await $scope.ordersClient.searchOrder($scope.current.structure.id, $scope.filterOrder, true);
                $scope.ordersClient.forEach(order => {
                    if (order.campaign.accessible) {
                        const orderRegion = new OrderRegion();
                        orderRegion.createFromOrderClient(order);
                        ordersToCreate.all.push(orderRegion);
                    }
                })
            } else {
                ordersToReformat = $scope.ordersClient.selectedElements;
                const __ret = reformatOrders(ordersToReformat, ordersToCreate, $scope.ordersClient,
                    totalPrice, totalPriceConsumable, totalAmount, totalAmountConsumable);
                totalPrice = __ret.totalPrice;
                totalPriceConsumable = __ret.totalPriceConsumable
                totalAmount = __ret.totalAmount;
                totalAmountConsumable = __ret.totalAmountConsumable;
            }
            ordersToCreate.create(comment).then(async data => {
                if (data.status === 201) {
                    toasts.confirm('crre.order.region.create.message');
                    $scope.$broadcast(INFINITE_SCROLL_EVENTER.RESUME);
                    if ($scope.allOrdersSelected || $scope.ordersClient.selectedElements.length === 0) {
                        $scope.ordersClient.all = [];
                        Utils.safeApply($scope);
                        if (!$scope.current.structure) await $scope.initStructures();
                        await $scope.getInfos();
                        await $scope.initOrders('WAITING');
                        await init();
                    } else {
                        await $scope.updateOrders(totalPrice, totalPriceConsumable, totalAmount, totalAmountConsumable,
                            $scope.ordersClient.selected, ordersToReformat.length);
                        $scope.allOrdersSelected = false;
                        await $scope.getAllAmount();
                        await checkCampaignAccessibility();
                        $scope.loading = false;
                        Utils.safeApply($scope);
                    }
                } else {
                    toasts.warning('crre.admin.order.create.err');
                }
            })
        };

        $scope.switchAllOrders = async () => {
            $scope.ordersClient.all.map((order) => order.selected = $scope.allOrdersSelected);
            await checkCampaignAccessibility();
            Utils.safeApply($scope);
        };

        $scope.checkSwitchAll = async (): Promise<void> => {
            let ifAllTrue: boolean = true;
            let ifAllFalse: boolean = true;
            let onlyCampaignInaccessible: boolean = true;
            $scope.ordersClient.all.forEach(function (order) {
                if (order.selected) {
                    if (order.campaign && order.campaign.accessible) {
                        onlyCampaignInaccessible = false;
                    }
                    ifAllFalse = false;
                } else {
                    ifAllTrue = false;
                }
            });
            $scope.allOrdersSelected = ifAllTrue;
            await checkCampaignAccessibility();
            Utils.safeApply($scope);
        };

        function endLoading(newData: boolean) {
            if (newData)
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            else {
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.PAUSE);
            }
        }

        $scope.syncSelected = (): void => {
            $scope.allOrdersSelected ? $scope.ordersClient.all.forEach(order => order.selected = $scope.allOrdersSelected) : null;
        };

        $scope.openLightboxRefuseOrder = () => {
            template.open('refuseOrder.lightbox', 'validator/order-refuse-confirmation');
            $scope.display.lightbox.refuseOrder = true;
        };

        $scope.closeRefuseOrder = () => {
            $scope.display.lightbox.refuseOrder = false;
            Utils.safeApply($scope);
        };

        $scope.openLightboxConfirmOrder = async () => {
            if($scope.allOrdersSelected) {
                await $scope.allOrderCLient.searchOrder($scope.current.structure.id, $scope.filterOrder, true);
                $scope.ordersDuplicated = $scope.allOrderCLient.all
            } else {
                $scope.ordersDuplicated = $scope.ordersClient.selected;
            }
            await template.open('confirmOrder.lightbox', 'validator/order-confirm');
            $scope.display.lightbox.confirmOrder = true;
        };

        $scope.closeConfirmOrder = () => {
            template.close('confirmOrder.lightbox');
            $scope.display.lightbox.confirmOrder = false;
            Utils.safeApply($scope);
        };

        $scope.exportCSV = () => {
            let selectedOrders = new OrdersClient();
            let all: boolean = $scope.ordersClient.selectedElements.length == 0 || $scope.allOrdersSelected;
            selectedOrders.all = all ? selectedOrders.all : $scope.ordersClient.selectedElements;
            selectedOrders.exportCSV($scope.filterOrder.typeCampaignList, $scope.filterOrder.userList, $scope.filterOrder.queryName, $scope.current.structure.id, $scope.filterOrder.startDate, $scope.filterOrder.endDate, all, [ORDER_STATUS_ENUM.WAITING]);
            $scope.ordersClient.forEach(function (order) {
                order.selected = false;
            });
            $scope.allOrdersSelected = false;
        }

        $scope.updateAmount = async (orderClient: OrderClient, amount: number) => {
            if (!!amount && amount > 0) {
                orderClient.amount = amount;
                await orderClient.updateAmount(amount);
                await $scope.getAllAmount();
                Utils.safeApply($scope);
            } else if (amount == 0) {
                toasts.warning('crre.empty.amount.error');
            }
        };

        $scope.updateReassort = async (orderClient: OrderClient) => {
            orderClient.reassort = !orderClient.reassort;
            await orderClient.updateReassort();
            Utils.safeApply($scope);
        };

        $scope.search = async () => {
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.RESUME);
            $scope.loading = true;
            $scope.filter.page = 0;
            $scope.ordersClient = new OrdersClient();

            const newData = await $scope.ordersClient.searchOrder($scope.current.structure.id, $scope.filterOrder, true, $scope.filter.page);
            endLoading(newData);
            $scope.loading = false;
            await $scope.getAllAmount();

            $scope.syncSelected();
            Utils.safeApply($scope);
        }

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

        await init();
    }]);