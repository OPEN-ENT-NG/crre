import {ng, template} from 'entcore';
import {Purse, PurseImporter, Purses, Utils} from '../../model';
import {Mix} from 'entcore-toolkit';
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";
import http from "axios";

declare let window: any;

export const purseController = ng.controller('PurseController',
    ['$scope', '$routeParams', ($scope) => {
        $scope.purses = new Purses();
        $scope.purses.get().then((purses) => {
            $scope.purses.all = purses;
            $scope.loading = false;
            Utils.safeApply($scope);
        });
        $scope.lightbox = {
            open: false
        };
        $scope.filter = {
            page: 0
        };
        $scope.loading = true;

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.purses.get($scope.filter.page).then((purses) => {
                if(purses.length > 0) {
                    $scope.purses.all = $scope.purses.all.concat(purses);
                    $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                    Utils.safeApply($scope);
                }
                $scope.loading = false;
                Utils.safeApply($scope);
            });
        };

        $scope.openEditPurseForm = (purse: Purse = new Purse()) => {
            $scope.purse = new Purse();
            Mix.extend($scope.purse, purse);
            template.open('purse.lightbox', 'administrator/purse/edit-purse-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };


        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.search($scope.query_name);
        };

        $scope.search = async (name: string, init: boolean = false) => {
            if(init) {
                $scope.purses.all = [];
                $scope.filter.page = 0;
            }
            if (!!name) {
                let {data} = await http.get(`/crre/purse/search?q=${name}&page=${$scope.filter.page}`);
                if(data.length > 0 ) {
                    $scope.purses.all = $scope.purses.all.concat(data);
                    $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                }
                Utils.safeApply($scope);
            } else {
                await $scope.purses.get($scope.filter.page);
                Utils.safeApply($scope);
            }
        }

        $scope.cancelPurseForm = () => {
            $scope.lightbox.open = false;
            delete $scope.purse;
        };

        $scope.validPurse = async (purse: Purse) => {
            await purse.save();
            $scope.lightbox.open = false;
            purse.amount = $scope.purses.selected[0].amount + (purse.initial_amount - $scope.purses.selected[0].initial_amount);
            purse.licence_amount = $scope.purses.selected[0].licence_amount + (purse.licence_initial_amount - $scope.purses.selected[0].licence_initial_amount);
            purse.selected = false;
            $scope.purses.all = $scope.purses.all.filter(purse => { return purse.id != $scope.purses.selected[0].id });
            $scope.purses.push(purse);
            Utils.safeApply($scope);
        };

        $scope.checkPurse = () => {
            return ($scope.purses.selected[0].licence_initial_amount && !$scope.purses.selected[0].initial_amount && !$scope.purse.licence_initial_amount) ||
            ($scope.purses.selected[0].initial_amount && !$scope.purses.selected[0].licence_initial_amount && !$scope.purse.initial_amount) ||
            ($scope.purses.selected[0].initial_amount && $scope.purses.selected[0].licence_initial_amount && !$scope.purse.initial_amount && !$scope.purse.licence_initial_amount);
        }

        $scope.openPurseImporter = (): void => {
            $scope.importer = new PurseImporter();
            template.open('purse.lightbox', 'administrator/purse/import-purses-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

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

        $scope.exportPurses = () => {
            let selectedPurses = [];
            $scope.purses.forEach(purse => {
                if(purse.selected) {
                    selectedPurses.push(purse);
                }
            });
            let params_id_purses = Utils.formatKeyToParameter(selectedPurses, 'id');
            window.location = `/crre/purses/export?${params_id_purses}`;
        };

    }]);