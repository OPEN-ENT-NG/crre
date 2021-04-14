import {_, ng, template} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {
    StructureGroup,
    StructureGroupImporter,
    Utils
} from '../../../model';

export const manageStructureGroupController = ng.controller('manageStructureGroupController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox: {
                structureGroup: false
            }
        };

        $scope.openStructureGroupForm = (structureGroup: StructureGroup = new StructureGroup()) => {
            $scope.redirectTo('/structureGroups/create');
            $scope.structureGroup = new StructureGroup();
            Mix.extend($scope.structureGroup, structureGroup);
            $scope.structureGroup.structureIdToObject(structureGroup.structures, $scope.structures);
            Utils.safeApply($scope);
        };

        $scope.openStructureGroupImporter = async () => {
            $scope.importer = new StructureGroupImporter();
            await template.open('structureGroup.lightbox', 'administrator/structureGroup/structureGroup-importer');
            $scope.display.lightbox.structureGroup = true;
        };

        $scope.openStructureGroupDeletion = (structureGroup: StructureGroup) => {
            $scope.structureGroup = structureGroup;
            template.open('structureGroup.lightbox', 'administrator/structureGroup/structureGroup-delete');
            $scope.display.lightbox.structureGroup = true;
        };
    }]);