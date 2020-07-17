import {_, ng, template, toasts} from 'entcore';
import {Instruction, Notification, Operation, Utils} from "../../model";


declare let window: any;

export const instructionController = ng.controller('instructionController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.sort = {
            instruction : {
                type: 'object',
                reverse: false
            }
        };
        $scope.search = {
            filterWord : '',
            filterWords : []
        };
        $scope.display = {
            lightbox : {
                instruction:false,
                exportEquipment: false,
            }
        };
        $scope.formatDate = (date) => {
            return Utils.formatDate(date)
        };
        $scope.getYearFromStr = (str) =>{
            return str.substr(0,4);
        }
        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.isOperationEdit = false;
        $scope.openInstructionForm = async (action: string) => {
                $scope.instruction = new Instruction();
            $scope.loadingArray = true;
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            await $scope.initOperation();
            $scope.operationEditRemoveInstructionIds = [];
            $scope.operations.all = $scope.operations.all
                .filter(operation => operation.instruction === null && operation.status === 'false');
            $scope.instructions.all.map(instruction => {
                instruction.operations.map(idOperation => {
                    $scope.operations.all.filter(operation => operation.id !== idOperation )
                });
            });
            $scope.operations.all.sort(function (a, b) {
                return  a.label.label.localeCompare(b.label.label);
            });

            $scope.allOperations =  [] ;
            $scope.operations.all.forEach(op =>{
                $scope.allOperations.push(op);
            });

            if(action === 'create'){
                $scope.instruction.operations = [];
            } else if (action === 'edit'){
                $scope.instruction = $scope.instructions.selected[0];
                await $scope.instruction.getOperations($scope.instruction.id);
                $scope.isOperationEdit = true;
                $scope.operations.all = $scope.operations.all
                    .filter(operation => operation.id_instruction !== $scope.instruction.id);
                $scope.operations.all.sort(function (a, b) {
                    return  a.label.label.localeCompare(b.label.label);
                });
            }
            $scope.instruction.operations.forEach( op =>{
                $scope.operations.all =   $scope.operations.all.filter(operation => operation.id_label !== op.id_label )
            });
            $scope.knowOperationIsEmpty();
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.isNewOperation = false;
        $scope.addOperationRow = () => {
            if ( $scope.operations.all.length !== 0)  $scope.isNewOperation = true;
            Utils.safeApply($scope);
        };
        $scope.operationIsSelect = () => {
            $scope.operation = new Operation();
            $scope.operationAdd = JSON.parse($scope.instruction.operation);
            $scope.operation.id = $scope.operationAdd.id;
            $scope.operation.label = $scope.operationAdd.label;
            $scope.operation.nbOrberSub = $scope.operationAdd.nbOrberSub;
            $scope.operation.nb_orders = $scope.operationAdd.nb_orders;
            $scope.operation.id_label = $scope.operationAdd.id_label;
            $scope.operation.amount = $scope.operationAdd.amount;
            $scope.operation.status = $scope.operationAdd.status;
            $scope.operations.all = $scope.operations.all.filter( operation => operation.id !== $scope.operation.id );
            $scope.operations.all = $scope.operations.all.filter( operation => operation.id_label !== $scope.operation.id_label)
            $scope.instruction.operations.push($scope.operation);
            $scope.isNewOperation = false;
            if($scope.isOperationEdit) {
                $scope.operationEditRemoveInstructionIds = $scope.operationEditRemoveInstructionIds
                    .filter( id => id !== $scope.operation.id);
            }
            $scope.instruction.operation = undefined;
            $scope.knowOperationIsEmpty();
        };
        $scope.knowOperationIsEmpty =() => {
            $scope.isOperationsIsEmpty =  $scope.operations.all.length === 0 ?  true : false;
        };
        $scope.dropOperation = (indexSelect: Number, operation: Operation) => {
            if($scope.isOperationEdit) $scope.operationEditRemoveInstructionIds.push(operation.id);
            $scope.instruction.operations = $scope.instruction.operations
                .filter((operation, index) => index !== indexSelect);
            $scope.operations.all.push(operation);
            $scope.operations.all.sort(function (a, b) {
                return  a.label.label.localeCompare(b.label.label);
            });
            $scope.allOperations.forEach(op =>{
                if(op.id_label== operation.id_label){
                    $scope.operations.all.push(op);
                }
            });

            $scope.knowOperationIsEmpty();
            Utils.safeApply($scope);
        };
        $scope.cancelFormAddOperation = () => {
            $scope.isNewOperation = false;
        };
        $scope.cancelInstructionForm = async () =>{
            $scope.cancelFormAddOperation();
            $scope.isOperationEdit = false;
            await $scope.initOperation();
            await $scope.initInstructions();
            template.open('instruction-main', 'administrator/instruction/manage-instruction');
            Utils.safeApply($scope);
        };
        $scope.isAllInstructionSelected = false;
        $scope.switchAllInstruction = () => {
            $scope.isAllInstructionSelected  =  !$scope.isAllInstructionSelected;
            if ( $scope.isAllInstructionSelected) {
                $scope.instructions.all.map(instructionSelected => instructionSelected.selected = true)
            } else {
                $scope.instructions.all.map(instructionSelected => instructionSelected.selected = false)
            }
            Utils.safeApply($scope);
        };
        $scope.addInstructionFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if(!_.contains($scope.instructions.filters, event.target.value)){
                    $scope.instructions.filters = [...$scope.instructions.filters, event.target.value];
                }
                event.target.value = '';
                await $scope.initInstructions();
                Utils.safeApply($scope);
            }
        };
        $scope.dropInstructionFilter = async (filter: string) => {
            $scope.instructions.filters = $scope.instructions.filters.filter( filterWord => filterWord !== filter);
            await $scope.initInstructions();
            Utils.safeApply($scope);
        };
        $scope.cancelInstructionLightbox = () =>{
            $scope.display.lightbox.instruction = false;
            template.close('instruction.lightbox');
        };
        $scope.openLightboxDeleteInstruction = () => {
            $scope.display.lightbox.instruction = true;
            template.open('instruction.lightbox', 'administrator/instruction/instruction-delete-lightbox');
            Utils.safeApply($scope);
        };
        $scope.deleteInstructions = async () => {
            if($scope.instructions.selected.some(instruction => instruction.operations.length !== 0)){
                template.open('instruction.lightbox', 'administrator/instruction/instruction-delete-reject-lightbox');
            } else {
                await $scope.instructions.delete();
                await $scope.initInstructions();
                template.close('instruction.lightbox');
                $scope.display.lightbox.instruction = false;
                Utils.safeApply($scope);
            }
        };
        $scope.sendInstruction = async () => {
            await $scope.instruction.save();
            if($scope.instruction.operations.length !== 0){
                let operationIds = $scope.instruction.operations.map( operation => operation.id );
                if($scope.instruction.id){
                    await $scope.operations.updateOperations($scope.instruction.id, operationIds);
                }
            }
            if($scope.operationEditRemoveInstructionIds.length !== 0){
                await $scope.operations.updateRemoveOperations($scope.operationEditRemoveInstructionIds);
            }
            $scope.isOperationEdit = false;
            await $scope.initInstructions();
            $scope.cancelInstructionForm();
            Utils.safeApply($scope);
        };

        $scope.exportRME = async (instruction) => {
            $scope.instructions.selected[0].selected = false;
            toasts.info('lystore.export.notif');
            await  instruction.getExportRME();

            Utils.safeApply($scope);

        };
        $scope.openExportEquipmentRapp = (instruction) => {
            $scope.display.lightbox.exportEquipment = true;
            $scope.instructionToExport = instruction;
            template.open('export.equipment.lightbox', 'administrator/instruction/export-equipment-rapport-lightbox');

        };

        $scope.exportNotification = async (instruction) => {
            toasts.info('lystore.export.notif');
            await instruction.exportNotificationCP();
            $scope.instructions.selected[0].selected = false;
            Utils.safeApply($scope);
        };

        $scope.exportSubvention = async (instruction) => {
            toasts.info('lystore.export.notif');
            await instruction.exportRapportSubvention();
            $scope.instructions.selected[0].selected = false;
            Utils.safeApply($scope);
        };

        $scope.exportPublipostage = async (instruction) => {
            toasts.info('lystore.export.notif');
            await instruction.exportPublipostage();
            $scope.instructions.selected[0].selected = false;
            Utils.safeApply($scope);
        };

        $scope.exportIris = async (instruction) => {
            toasts.info('lystore.export.notif');
            await instruction.exportIris();
            $scope.instructions.selected[0].selected = false;
            Utils.safeApply($scope);
        };


        $scope.selectTypeForExport = async (type, instruction: Instruction) => {
            $scope.display.lightbox.exportEquipment = false;

            template.close('export.equipment.lightbox');
            toasts.info('lystore.export.notif');
            await instruction.getExportEquipment(type);
            $scope.instructions.selected[0].selected = false;
            Utils.safeApply($scope);
        }
    }]);