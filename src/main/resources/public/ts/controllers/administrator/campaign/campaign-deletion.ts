import { ng } from 'entcore';
import {
    Utils
} from '../../../model';

export const campaignDeletionController = ng.controller('campaignDeletionController',
    ['$scope', ($scope) => {
        $scope.deleteCampaigns = async (campaigns) => {
            await $scope.campaigns.delete(campaigns);
            await $scope.campaigns.sync();
            $scope.allCampaignSelected = false;
            $scope.display.lightbox.campaign = false;
            Utils.safeApply($scope);
        };
    }]);