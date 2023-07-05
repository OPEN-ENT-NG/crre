import {ng, template} from 'entcore';
import {Purse, PurseImporter, Purses, Structure, Student, StudentInfo, Utils} from '../../../model';
import {Mix} from 'entcore-toolkit';
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";

export const purseController = ng.controller('PurseController',
    ['$scope', '$routeParams', ($scope) => {
        $scope.studentInformations = new Map<string, Student>();
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
        $scope.init = true;

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.search($scope.query_name, $scope.init)
            $scope.init = false
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
            $scope.syncSelected();
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
        };

        $scope.checkSwitchAll = (): void => {
            let testAllTrue = true;
            $scope.purses.forEach(purse => {
                if (!purse.selected) {
                    testAllTrue = false;
                }
            });
            $scope.allPurseSelected = testAllTrue;
            Utils.safeApply($scope);
        };

        $scope.syncSelected = () : void => {
            $scope.purses.forEach(purse => purse.selected = $scope.allPurseSelected)
        };

        $scope.getStudentInformation = (idStructure): Array<StudentInfo> => {
            let studentInfo: Student;
            if ($scope.studentInformations.has(idStructure)) {
                studentInfo = $scope.studentInformations.get(idStructure);
            } else {
                studentInfo = new Student();
                $scope.studentInformations.set(idStructure, studentInfo);
                studentInfo.getAmount(idStructure).then(() => Utils.safeApply($scope));
            }

            return studentInfo.studentInfo;
        }

    }]);