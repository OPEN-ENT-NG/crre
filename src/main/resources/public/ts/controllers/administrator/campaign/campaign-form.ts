import {_, ng} from 'entcore';
import {Campaign, StructureGroup, Utils} from '../../../model';
import http from "axios";

export const campaignFormController = ng.controller('campaignFormController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.othersSelected = false;
            $scope.articleFormat = [
                {name: "Tous", value: null},
                {name: "Catalogue papier", value: "articlepapier"},
                {name: "Catalogue numérique", value: "articlenumerique"},
                {name: "Catalogue numérique Pro", value: "articlenumerique|pro"},
                {name: "Catalogue papier Pro", value: "articlepapier|pro"},
                {name: "Catalogue numérique LGT", value: "articlenumerique|lgt"},
                {name: "Catalogue papier LGT", value: "articlepapier|lgt"},
                {name: "Catalogue consommable Pro", value: "consommable|pro"}];
            $scope.creditFormat = [
                {name: "Aucun crédit", value: "none"},
                {name: "Licences manuels", value: "licences"},
                {name: "Licences consommables", value: "consumable_licences"},
                {name: "Crédits monétaires", value: "credits"},
                {name: "Crédits monétaires consommables", value: "consumable_credits"}
            ];
            $scope.structureFormat = [
                {name: "Papier"},
                {name: "Numériques"},
                {name: "Mixtes"},
                {name: "Établissements professionnels"},
                {name: "Établissements généraux"},
                {name: "Établissements polyvalents"}
            ]
            $scope.formatCheck = [];
            $scope.allSelected = $scope.allProAndGenSelected = false;
            await $scope.getTypesCampaign();
            if (new RegExp('update').test(window.location.hash)) {
                $scope.campaign.catalog = $scope.articleFormat.find(format => $scope.campaign.catalog == format.value);
                $scope.campaign.use_credit = $scope.creditFormat.find(format => $scope.campaign.use_credit == format.value);
                if (!!$scope.campaign.id_type) {
                    $scope.campaign.name_type = $scope.formatCheck.find(format => $scope.campaign.id_type == format.id_type).name_type;
                    $scope.campaign_type = $scope.formatCheck.find(format => $scope.campaign.id_type == format.id_type);
                }
                if ($scope.campaign.groups.length > 0) {
                    formatStructureGroups($scope.campaign.groups);
                }
            } else {
                $scope.campaign = new Campaign();
                $scope.campaign.use_credit = $scope.creditFormat[0];
            }
            Utils.safeApply($scope);
        }


        $scope.changeList = () => {
            $scope.campaign.use_credit = $scope.creditFormat.find(format => $scope.campaign_type.credit == format.value)
            $scope.campaign.reassort = $scope.campaign_type.reassort;
            $scope.campaign.automatic_close = $scope.campaign_type.automatic_close;
            $scope.campaign.id_type = $scope.campaign_type.id_type;
            $scope.campaign.name_type = $scope.campaign_type.name_type;
            $scope.campaign.catalog = $scope.campaign_type.catalog;
            $scope.structureFormat.forEach(structure => {
                structure.selected = !!$scope.campaign_type.structure.find(item => {
                    return item.libelle == structure.name
                });
            });
            $scope.allSelected = $scope.allProAndGenSelected = false;
            Utils.safeApply($scope);
        }

        $scope.getTypesCampaign = async () => {
            let {data} = await http.get(`/crre/campaigns/types`);
            data.forEach(function (type) {
                type.catalog = $scope.articleFormat.find(format => type.catalog == format.name);
                type.structure = JSON.parse(type.structure);
                $scope.formatCheck.push(type);
            });
            Utils.safeApply($scope);
        }

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.validCampaignForm = (campaign: Campaign) => {
            return campaign.name !== undefined
                && campaign.name.trim() !== ''
                && _.findWhere($scope.structureFormat, {selected: true}) !== undefined
                && $scope.campaign.catalog !== undefined;

        };

        $scope.managStructureGroups = () => {
            let proOrGenSelected = false;
            let papierNumeriqueSelected = false;
            let groups = [];
            $scope.structureFormat.slice(3, 6).forEach(group => {
                if (group.selected) {
                    proOrGenSelected = true;
                    $scope.structureFormat.slice(0, 3).forEach(groupPapNum => {
                        if (groupPapNum.selected) {
                            papierNumeriqueSelected = true;
                            const groupName = group.name + " " + groupPapNum.name.toLowerCase();
                            groups.push(_.findWhere($scope.structureGroups.all, {name: groupName}));
                        }
                    });
                    if (!papierNumeriqueSelected) {
                        groups.push(_.findWhere($scope.structureGroups.all, {name: group.name}));
                    }
                }
            });
            if (!proOrGenSelected && papierNumeriqueSelected) {
                $scope.structureFormat.slice(0, 3).forEach(groupPapNum => {
                    if (groupPapNum.selected) {
                        papierNumeriqueSelected = true;
                        groups.push(_.findWhere($scope.structureGroups.all, {name: groupPapNum.name}));
                    }
                });
            } else if (!proOrGenSelected && !papierNumeriqueSelected) {
                $scope.structureGroups.selected.forEach(group => {
                    groups.push(group);
                });
            }
            return groups;
        };

        $scope.selectStructures = () => {
            $scope.structureFormat.slice(0, 3).forEach(group => {
                group.selected = $scope.allSelected;
            });
            Utils.safeApply($scope);
        }

        $scope.changeInOthersSelected = (checking: boolean) => {
            if (checking) {
                $scope.structureFormat.slice(0, 6).forEach(group => {
                    group.selected = false;
                });
                $scope.allSelected = $scope.allProAndGenSelected = false;
                $scope.othersSelected = true;
            } else {
                let remainOthersSelected = false;
                $scope.structureFormat.slice(11).forEach(group => {
                    if (group.selected) {
                        remainOthersSelected = true;
                    }
                });
                $scope.othersSelected = remainOthersSelected;
            }
            Utils.safeApply($scope);
        }

        $scope.selectProAndGenStructures = () => {
            $scope.structureFormat.slice(3, 6).forEach(group => {
                group.selected = $scope.allProAndGenSelected;
            });
            Utils.safeApply($scope);
        }

        $scope.validCampaign = async (campaign: Campaign) => {
            $scope.campaign.groups = [];
            $scope.campaign.catalog = $scope.campaign.catalog.value;
            $scope.campaign.use_credit = $scope.campaign.use_credit.value;
            $scope.campaign.groups = $scope.managStructureGroups();
            await campaign.save();
            $scope.redirectTo('/campaigns');
            Utils.safeApply($scope);
        };
        this.init();

        function formatStructureGroups(groups: StructureGroup[]) {
            groups.forEach(group => {
                if (new RegExp('généraux').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Établissements généraux").selected = true;
                }
                if (new RegExp('professionnels').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Établissements professionnels").selected = true;
                }
                if (new RegExp('polyvalents').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Établissements polyvalents").selected = true;
                }
                if (new RegExp('mixtes').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Mixtes").selected = true;
                }
                if (new RegExp('papier').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Papier").selected = true;
                }
                if (new RegExp('numériques').test(group.name)) {
                    $scope.structureFormat.find(s => s.name === "Numériques").selected = true;
                }
            });

            // If Numérique, Papier and Mixte are selected, select the all group
            if ($scope.structureFormat.slice(0, 3).filter(s => s.selected === true).length === 3) {
                $scope.allSelected = true;
            }

            // If Pro and General are selected, select the all group
            if ($scope.structureFormat.slice(3, 6).filter(s => s.selected === true).length === 3) {
                $scope.allProAndGenSelected = true;
            }

        }
    }]);