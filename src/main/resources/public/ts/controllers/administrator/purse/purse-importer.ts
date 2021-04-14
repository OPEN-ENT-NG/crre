import {ng} from 'entcore';
import {PurseImporter, Utils} from '../../../model';

export const purseImporterController = ng.controller('purseImporterController',
    ['$scope', ($scope) => {
        $scope.importPurses = async (importer: PurseImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    $scope.filter.page = 0;
                    await $scope.purses.get().then((purses) => {
                        $scope.purses.all = purses;
                        $scope.loading = false;
                        Utils.safeApply($scope);
                    });
                    $scope.lightbox.open = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                Utils.safeApply($scope);
            }
        };
    }]);