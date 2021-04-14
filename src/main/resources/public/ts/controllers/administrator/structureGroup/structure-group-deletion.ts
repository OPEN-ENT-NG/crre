import {ng} from 'entcore';
import {
    Utils
} from '../../../model';

export const structureGroupDeletionController = ng.controller('structureGroupDeletionController',
    ['$scope', ($scope) => {

        $scope.deleteStructureGroup = async () => {
            await $scope.structureGroup.delete();
            await $scope.structureGroups.sync();
            $scope.display.lightbox.structureGroup = false;
            Utils.safeApply($scope);
        };
    }]);