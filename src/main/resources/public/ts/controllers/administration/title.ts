import {ng, template, toasts} from 'entcore';
import {Notification, TitleImporter, Utils} from '../../model';

declare let window: any;

export const titleController = ng.controller('TitleController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.campaign = $scope.campaigns.get(parseInt($routeParams.idCampaign));
        $scope.campaign.titles.sync($scope.campaign.id).then($scope.$apply);

        $scope.lightbox = {
            open: false
        };

        $scope.sort = {
            title: {
                type: 'name',
                reverse: false
            }
        };

        $scope.openTitleImporter = (): void => {
            $scope.importer = new TitleImporter($scope.campaign.id);
            template.open('title.lightbox', 'administrator/campaign/title/import-titles-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.importTitles = async (importer: TitleImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    await $scope.campaign.titles.sync($scope.campaign.id);
                    $scope.lightbox.open = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                Utils.safeApply($scope);
            }
        };

        $scope.deleteTitle = async ({id_campaign, id, structure_id}) => {
            try {
                await $scope.campaign.titles.delete(id_campaign, id, structure_id);
                await $scope.campaign.titles.sync($scope.campaign.id);
                $scope.lightbox.open = false;
                toasts.confirm('lystore.campaign.titles.delete.success');
                Utils.safeApply($scope);
            } catch (err) {
                toasts.warning('lystore.campaign.titles.delete.error');
            }
        };

        $scope.openDeleteConfirmation = (structure, title) => {
            $scope.title = {
                name: title.name,
                id: title.id,
                structure_id: structure.id_structure,
                structure_name: structure.name,
                id_campaign: $scope.campaign.id
            };
            template.open('title.lightbox', 'administrator/campaign/title/title-deletion-confirmation');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };
    }]);