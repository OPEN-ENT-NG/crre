import {_, idiom as lang, moment, ng, notify, template, toasts} from 'entcore';
import {
    OrderRegion,
    OrdersRegion,
    Structure,
    StructureGroup,
    StructureGroups,
    Structures,
    Utils,
    Equipments,
    Contracts, Equipment
} from "../../model";
import http from "axios";
import {Mix} from "entcore-toolkit";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {

        $scope.orderToCreate = new OrderRegion();
        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.display = {
            lightbox: {
                validOrder: false,
            },
        };
        $scope.translate = (key: string):string => lang.translate(key);

        $scope.getOrdersByProject = async(id: number) => {
            try {
                let { data } = await http.get(`/crre/orderRegion/orders/${id}`);
                let orders = Mix.castArrayAs(OrderRegion, data)
                for (let order of orders) {
                    let equipment = new Equipment();
                    await equipment.sync(order.equipment_key);
                    order.price = (equipment.price * (1 + equipment.tax_amount / 100)) * order.amount;
                    order.name = equipment.name;
                    order.image = equipment.image;
                }
                return orders;
            } catch (e) {
                notify.error('crre.basket.sync.err');
            }
        }

        $scope.getProjects = async() => {
            try {
                let { data } = await http.get(`/crre/orderRegion/projects`);
                $scope.projects = data;
            } catch (e) {
                notify.error('crre.basket.sync.err');
            }
        }

        $scope.getProjectsSearch = async(name: string) => {
            try {
                let { data } = await http.get(`/crre/ordersRegion/search?q=${name}`);
                $scope.projects = data;
            } catch (e) {
                notify.error('crre.basket.sync.err');
            }
        }

        $scope.searchByName =  async (name: string) => {
            if(name != "") {
                await $scope.getProjectsSearch(name);
                await synchroRegionOrders(true);
            } else {
                await synchroRegionOrders();
            }
            Utils.safeApply($scope);
        }

        $scope.calculateTotalRegion = (orders: OrderRegion[], roundNumber: number) => {
            let totalPrice = 0;
            orders.forEach(order => {
                totalPrice += order.amount * order.price_single_ttc;
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
        const synchroRegionOrders = async (isSearching: boolean = false) : Promise<void> => {
            //await $scope.basketsOrders.sync($scope.campaign.id);
            //await $scope.displayedOrdersRegion.sync();
            if(!isSearching) {
                await $scope.getProjects();
            }
            for (const project of $scope.projects) {
                project.orders = await $scope.getOrdersByProject(project.id);
                project.orders.map(order => {
                    order.creation_date =  moment(order.creation_date.created).format('DD-MM-YYYY').toString();
                });
                project.total = currencyFormatter.format($scope.calculateTotalRegion(project.orders, 2));
                project.amount = $scope.calculateAmountRegion(project.orders);
                project.creation_date = moment(project.orders[0].creation_date).format('DD-MM-YYYY').toString();
                project.status = project.orders[0].status;
            }

            Utils.safeApply($scope);
        };
        synchroRegionOrders();

        $scope.updateCampaign = async ():Promise<void> => {
            $scope.orderToCreate.rows = undefined;
            $scope.orderToCreate.project = undefined;
            $scope.orderToCreate.operation = undefined;
            await $scope.structure_groups.syncByCampaign($scope.orderToCreate.campaign.id);
            let structures = new Structures();
            $scope.structure_groups.all.map(structureGR => {
                structureGR.structures.map(structureId => {
                    let newStructure = new Structure();
                    newStructure.id = structureId;
                    newStructure = $scope.structures.all.find(s => s.id === newStructure.id);
                    if (structures.all.indexOf(newStructure) === -1)
                        structures.push(newStructure);
                })
            });
            $scope.structuresToDisplay = structures;
            $scope.structuresToDisplay.all.sort((firstStructure, secondStructure) => {
                if (firstStructure.uai > secondStructure.uai) return 1;
                if (firstStructure.uai < secondStructure.uai) return -1;
                return 0;
            });
            Utils.safeApply($scope);
        };

        $scope.isOperationsIsEmpty = false;
        $scope.selectOperationForOrder = async ():Promise<void> => {
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.cancelUpdate = ():void => {
            if($scope.operationId) {
                $scope.redirectTo(`/operation/order/${$scope.operationId}`)
                $scope.operationId = undefined;
            }
            else if ($scope.fromWaiting)
                $scope.redirectTo('/order/waiting');
            else
                window.history.back();
        };
        $scope.updateOrderConfirm = async ():Promise<void> => {
            await $scope.selectOperationForOrder();
        };

        $scope.updateLinkedOrderConfirm = async ():Promise<void> => {
            let orderRegion = new OrderRegion();
            orderRegion.createFromOrderClient($scope.orderToUpdate);
            orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
            orderRegion.id_contract = orderRegion.equipment.id_contract;
            $scope.cancelUpdate();
            if($scope.orderToUpdate.typeOrder === "region"){
                await orderRegion.update($scope.orderToUpdate.id);
            } else {
                await orderRegion.create();
            }
            toasts.confirm('crre.order.region.update');
        };
        $scope.isValidFormUpdate = ():boolean => {
            return $scope.orderToUpdate &&  $scope.orderToUpdate.equipment_key
                &&  $scope.orderToUpdate.equipment
                && $scope.orderToUpdate.price_single_ttc
                && $scope.orderToUpdate.amount
                && ((($scope.orderToUpdate.rank>0 &&
                    $scope.orderToUpdate.rank > 0  ||
                    $scope.orderToUpdate.rank === null)) ||
                    !$scope.orderToUpdate.campaign.orderPriorityEnable())
        };

        function checkRow(row):boolean {
            return row.equipment && row.price && row.structure && row.amount
        }

        $scope.oneRow = ():boolean => {
            let oneValidRow = false;
            if ($scope.orderToCreate.rows)
                $scope.orderToCreate.rows.map(row => {
                    if (checkRow(row))
                        oneValidRow = true;
                });
            return oneValidRow;
        };

        $scope.validForm = ():boolean => {
            return $scope.orderToCreate.campaign
                && $scope.orderToCreate.project
                && $scope.orderToCreate.operation
                && $scope.oneRow()
                && ($scope.orderToCreate.rows.every( row => (row.rank>0 &&
                    row.rank<11  ||
                    row.rank === null))
                    || !$scope.orderToCreate.campaign.orderPriorityEnable());
        };

        $scope.addRow = ():void => {
            let row = {
                equipment: undefined,
                equipments: new Equipments(),
                allEquipments : [],
                contracts : new Contracts(),
                structure: undefined,
                price: undefined,
                amount: undefined,
                comment: "",
                display: {
                    struct: false
                }
            };
            if (!$scope.orderToCreate.rows)
                $scope.orderToCreate.rows = [];
            $scope.orderToCreate.rows.push(row);
            Utils.safeApply($scope)

        };

        $scope.dropRow = (index:number):void => {
            $scope.orderToCreate.rows.splice(index, 1);
        };

        $scope.duplicateRow = (index:number):void => {
            let row = JSON.parse(JSON.stringify($scope.orderToCreate.rows[index]));
            row.equipments = new Equipments();
            row.contracts = new Contracts();
            if (row.structure){
                if (row.structure.structures) {
                    row.structure = $scope.structure_groups.all.find(struct => row.structure.id === struct.id);
                } else {
                    row.structure = $scope.structures.all.find(struct => row.structure.id === struct.id);
                }
            }
            //duplicate contracttypes
            row.ct_enabled =  $scope.orderToCreate.rows[index].ct_enabled;

            $scope.orderToCreate.rows[index].contracts.all.forEach(ct=>{
                row.contracts.all.push(ct);
            })
            if($scope.orderToCreate.rows[index].contract_type)
                row.contract_type = $scope.orderToCreate.rows[index].contract_type ;

            $scope.orderToCreate.rows[index].equipments.forEach(equipment => {
                row.equipments.push(equipment);
                if (row.equipment && row.equipment.id === equipment.id)
                    row.equipment = equipment;
            });
            $scope.orderToCreate.rows.splice(index + 1, 0, row)
        };
        $scope.cancelBasketDelete = ():void => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.switchStructure = async (row:any, structure:Structure):Promise<void> => {
            await row.equipments.syncAll($scope.orderToCreate.campaign.id, (structure) ? structure.id : undefined);
            await row.contracts.sync();
            row.contract_type = undefined;
            row.ct_enabled = undefined;
            let contracts = [];
            row.equipments.all.forEach(e => {
                row.allEquipments.push(e);
                row.contracts.all.map(contract =>{
                    if(contract.id === e.id_contract  && !contract.isPresent ){
                        contract.isPresent = true;
                        contracts.push(contract);
                    }
                })
            });
            row.contracts.all = contracts;
            row.equipment = undefined;
            row.price = undefined;
            row.amount = undefined;
            Utils.safeApply($scope);
        };
        $scope.initEquipmentData = (row:OrderRegion):void => {
            let roundedString = row.equipment.priceTTC.toFixed(2);
            let rounded = Number(roundedString);
            row.price = Number(rounded);
            row.amount = 1;
        };
        $scope.initContractType = async (row) => {
            if (row.contract) {
                row.ct_enabled = true;
                row.equipment = undefined;
                row.equipments.all = row.allEquipments.filter(equipment => row.contract.id === equipment.id_contract);
                Utils.safeApply($scope);

            }
        };

        $scope.swapTypeStruct = (row):void => {
            row.display.struct = !row.display.struct;
            row.equipment = undefined;
            row.price = undefined;
            row.amount = undefined;
            row.comment ="";
            row.structure = undefined;
            Utils.safeApply($scope);
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
                notify.error('crre.admin.order.create.err');
            }
            Utils.safeApply($scope);
        }
    }
    ]);