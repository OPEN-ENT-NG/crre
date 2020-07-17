import { ng, template, notify, moment, _ } from 'entcore';
import { PurseImporter, Utils, Purse, Purses } from '../../model';
import { Mix } from 'entcore-toolkit';

declare let window: any;

export const purseController = ng.controller('PurseController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.campaign = $scope.campaigns.get(parseInt($routeParams.idCampaign));
        $scope.campaign.purses = new Purses(parseInt($routeParams.idCampaign));
        $scope.campaign.purses.sync().then(() => Utils.safeApply($scope));

        $scope.lightbox = {
            open: false
        };

        $scope.sort = {
            purse: {
                type: 'name',
                reverse: false
            }
        };

        $scope.openEditPurseForm = (purse: Purse = new Purse()) => {
            $scope.purse = new Purse();
            Mix.extend($scope.purse, purse);
            template.open('purse.lightbox', 'administrator/campaign/purse/edit-purse-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.cancelPurseForm = () => {
            $scope.lightbox.open = false;
            delete $scope.purse;
        };

        $scope.validPurse = async (purse: Purse) => {
            let status = await purse.save();
            if(status === 202){
                $scope.isNegativePurse = true;
            }else{
                $scope.lightbox.open = false;
                await $scope.campaign.purses.sync($scope.campaign.id);
                delete $scope.purse;
            }
            $scope.allHolderSelected = false;
            Utils.safeApply($scope);
        };

        $scope.openPurseImporter = (): void => {
            $scope.importer = new PurseImporter($scope.campaign.id);
            template.open('purse.lightbox', 'administrator/campaign/purse/import-purses-form');
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
                    await $scope.campaign.purses.sync($scope.campaign.id);
                    $scope.lightbox.open = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                Utils.safeApply($scope);
            }
        };

        $scope.exportPurses = (id: number) => {
            window.location = `/lystore/campaign/${id}/purses/export`;
        };
        $scope.checkPurses = async ( id_Campaign : number ) =>{
            $scope.isChecked = true;
            await $scope.campaign.purses.check(id_Campaign);
            Utils.safeApply($scope)
        }

    }]);