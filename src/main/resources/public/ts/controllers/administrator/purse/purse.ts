import {ng, template} from 'entcore';
import {Purse, PurseImporter, Purses, Utils} from '../../../model';
import {Mix} from 'entcore-toolkit';
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";

export const purseController = ng.controller('PurseController',
    ['$scope', '$routeParams', ($scope) => {
        $scope.purses = new Purses();
        $scope.purses.get().then((purses) => {
            insertInPurses(purses);
        });
        $scope.lightbox = {
            open: false
        };
        $scope.filter = {
            page: 0
        };
        $scope.loading = true;
        $scope.allPurseSelected = false;

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.purses.get($scope.filter.page).then((purses) => {
                insertInPurses(purses);
            });
        };

        $scope.openEditPurseForm = (purse: Purse = new Purse()) => {
            $scope.purse = new Purse();
            Mix.extend($scope.purse, purse);
            template.open('purse.lightbox', 'administrator/purse/edit-purse-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        function insertInPurses(purses) {
            if (purses.length > 0) {
                $scope.purses.all = $scope.purses.all.concat(purses);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        $scope.search = async (name: string, init: boolean = false) => {
            $scope.loading = true;
            if(init) {
                $scope.purses.all = [];
                $scope.filter.page = 0;
            }
            if (!!name) {
                await $scope.purses.search(name, $scope.filter.page).then((purses) => {
                    insertInPurses(purses);
                });
            } else {
                await $scope.purses.get($scope.filter.page).then((purses) => {
                    insertInPurses(purses);
                });
            }
        }

        $scope.openPurseImporter = (): void => {
            $scope.importer = new PurseImporter();
            template.open('purse.lightbox', 'administrator/purse/import-purses-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.switchAll = () => {
            $scope.purses.forEach(purse => {purse.selected = $scope.allPurseSelected;});
            Utils.safeApply($scope);
        }

    }]);