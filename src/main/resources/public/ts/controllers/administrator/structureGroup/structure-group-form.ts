import {_, ng} from 'entcore';
import {
    StructureGroup,
    Utils
} from '../../../model';

export const structureGroupFormController = ng.controller('structureGroupFormController',
    ['$scope', ($scope) => {
        $scope.search = {};

        $scope.structuresFilter = (structureRight) => {
            return _.findWhere($scope.structureGroup.structures, {id: structureRight.id}) === undefined;
        };

        $scope.getStructureNumber = () => {
            return _.without($scope.structures.all, ...$scope.structureGroup.structures).length;
        };

        $scope.selectAllStructures = (structures: any) => {
            structures.selectAll();
            Utils.safeApply($scope);
        };

        $scope.deselectAllStructures = (structures: any) => {
            structures.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.updateSelection = (structures: any, value: boolean) => {
            structures.map((structure) => structure.selected = value);
            Utils.safeApply($scope);
        };

        $scope.addStructuresInGroup = () => {
            $scope.structureGroup.structures.push.apply($scope.structureGroup.structures, $scope.structures.selected);
            $scope.structureGroup.structures = _.uniq($scope.structureGroup.structures);
            $scope.structures.deselectAll();
            $scope.search.structure = '';
            Utils.safeApply($scope);
        };

        $scope.deleteStructuresofGroup = () => {
            $scope.structureGroup.structures = _.difference($scope.structureGroup.structures,
                $scope.structureGroup.structures.filter(structureRight => structureRight.selected));
            $scope.structures.deselectAll();
            $scope.search.structureRight = '';
            Utils.safeApply($scope);
        };

        $scope.validStructureGroupForm = (structureGroup: StructureGroup) => {
            return structureGroup.name !== undefined
                && structureGroup.name.trim() !== ''
                && !$scope.categories.includes(structureGroup.name)
                && structureGroup.structures.length > 0;
        };

        $scope.validStructureGroup = async (structureGroup: StructureGroup) => {
            await structureGroup.save();
            $scope.redirectTo('/structureGroups');
            Utils.safeApply($scope);
        };
    }]);