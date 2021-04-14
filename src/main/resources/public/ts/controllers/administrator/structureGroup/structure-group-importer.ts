import {ng} from 'entcore';
import {
    StructureGroupImporter,
    Utils
} from '../../../model';

export const structureGroupImporter = ng.controller('structureGroupImporter',
    ['$scope', ($scope) => {

        $scope.importStructureGroups = async (importer: StructureGroupImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    $scope.display.lightbox.structureGroup = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                await $scope.structureGroups.sync();
                Utils.safeApply($scope);
            }
        };

    }]);