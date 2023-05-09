import {_, ng} from 'entcore';
import {Equipment, Filter, Filters, Utils} from '../../../model';
import {FilterCatalogItem} from "../../../model/FiltersCatalogItem";

export const catalogController = ng.controller('catalogController',
    ['$scope', async ($scope) => {
        const init = async () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;

            $scope.catalog = {
                disciplines: [],
                targets: [],
                grades: [],
                classes: [],
                catalogs: [],
                editors: [],
                consumables: [],
                pros: [],
                booksellers: [],
            };

            $scope.preFilter = {
                disciplines: true,
                targets: true,
                grades: true,
                classes: true,
                catalogs: true,
                editors: true,
                consumables: true,
                pros: true,
                booksellers: true,
            }
            $scope.correlationFilterES = {
                keys: ["catalogs", "disciplines", "grades", "classes", "editors", "consumables", "pros", "targets", "booksellers"],
                disciplines: 'disciplines.libelle',
                targets: 'targetscible',
                grades: 'niveaux.libelle',
                classes: 'classes.libelle',
                catalogs: '_index',
                editors: 'editeur',
                consumables: 'conso',
                pros: 'pro',
                booksellers: 'booksellers',
            };
            initFilters();

            if (!$scope.current.structure) await $scope.initStructures();
        }

        function initFilters() {
            if (!!$scope.campaign.catalog && $scope.filters.all.length == 0) {
                $scope.filters = new Filters();
                // If catalog contain consommable filter
                let consommableFilter = new Filter();
                consommableFilter.name = "conso";
                if ($scope.campaign.catalog.split("|").includes("consommable")) {
                    consommableFilter.value = "Consommable";
                } else if ($scope.campaign.catalog.split("|").includes("nonconsommable")) {
                    consommableFilter.value = "Manuel";
                } else if ($scope.campaign.catalog.split("|").includes("ressource")) {
                    consommableFilter.value = "Ressource";
                }
                $scope.filters.all.push(consommableFilter);

                // If catalog contain pro/lgt filter
                if ($scope.campaign.catalog.split("|").includes("pro") || $scope.campaign.catalog.split("|").includes("lgt")) {
                    let proFilter = new Filter();
                    proFilter.name = "pro";
                    proFilter.value = $scope.campaign.catalog.split("|").includes("pro") ? "Lycée professionnel" : "Lycée général et technologique";
                    $scope.filters.all.push(proFilter);
                }

                // If catalog contain numeric/paper filter
                if ($scope.campaign.catalog.split("|").includes("articlenumerique")) {
                    let catalogFilter = new Filter();
                    catalogFilter.name = "_index";
                    catalogFilter.value = "articlenumerique";
                    $scope.filters.all.push(catalogFilter);
                } else if ($scope.campaign.catalog.split("|").includes("articlepapier")) {
                    let catalogFilter = new Filter();
                    if ($scope.correlationFilterES.keys.indexOf('classes') != -1) {
                        $scope.correlationFilterES.keys.splice($scope.correlationFilterES.keys.indexOf('classes'), 1);
                        delete $scope.equipments.filters.classes;
                        delete $scope.catalog.classes;
                        Utils.safeApply($scope);
                    }
                    catalogFilter.name = "_index";
                    catalogFilter.value = "articlepapier";
                    $scope.filters.all.push(catalogFilter);
                }
            } else {
                $scope.correlationFilterES.keys.forEach(key => {
                    let arrayFilter = [];
                    $scope.filters.all.filter(t => t.name == $scope.correlationFilterES[key]).forEach(filter => {
                        arrayFilter.push($scope.equipments.filters[key].find(c => c.name == filter.value));
                    });
                    $scope.catalog[key] = arrayFilter;
                });
            }
        }

        $scope.addFilter = async () => {
            if (!!$scope.queryWord || $scope.queryWord == '') {
                $scope.query.word = $scope.queryWord;
            }
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            // Add to prefilter to hide filters in front
            if (!!$scope.campaign.catalog) {
                const splitCatalog = $scope.campaign.catalog.split("|");
                // If catalog contain consommable filter, the catalog is already filtered
                if (splitCatalog.includes("consommable") || splitCatalog.includes("nonconsommable") || splitCatalog.includes("ressource")) {
                    $scope.preFilter["consumables"] = false;
                }
                if (splitCatalog.includes("articlepapier") || splitCatalog.includes("articlenumerique")) {
                    $scope.preFilter["catalogs"] = false;
                }
                if (splitCatalog.includes("pro") || splitCatalog.includes("lgt")) {
                    $scope.preFilter["pros"] = false;
                }
            }
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            if($scope.isAdministratorInStructure($scope.current.structure)) {
                $scope.equipments.filters.consumables = [{name: 'Consommable'}, {name: 'Manuel'}, {name: 'Ressource'}] as FilterCatalogItem[];
                $scope.equipments.filters.consumables.forEach((item) => item.toString = () => $scope.translate(item.name));
                $scope.equipments.filters.pros = [{name: 'Lycée général et technologique'}, {name: 'Lycée professionnel'}] as FilterCatalogItem[];
                $scope.equipments.filters.pros.forEach((item) => item.toString = () => $scope.translate(item.name));
            }

            Utils.safeApply($scope);
            if (!!$scope.campaign.catalog) {
                if (!$scope.campaign.catalog.split("|").includes("consommable")) {
                    let arrayDocs = [];
                    arrayDocs.push($scope.equipments.filters.catalogs.find(c => c.name = $scope.campaign.catalog.split("|")[0]));
                    $scope.catalog["catalogs"] = arrayDocs;
                }
                if ($scope.campaign.catalog.split("|").includes("consommable")) {
                    let arrayConso = [];
                    arrayConso.push($scope.equipments.filters.consumables.find(c => c.name = "Consommable"));
                    $scope.catalog["consumables"] = arrayConso;
                }
                if ($scope.campaign.catalog.split("|").includes("pro") || $scope.campaign.catalog.split("|").includes("lgt")) {
                    let type: string = $scope.campaign.catalog.split("|").includes("pro") ? "Lycée professionnel" : "Lycée général et technologique";
                    let proFilter: string = $scope.equipments.filters.pros.find(t => t.name = type);
                    if (proFilter) {
                        $scope.catalog["pros"] = [proFilter];
                    }
                }
            }
            Utils.safeApply($scope);
        };

        $scope.getFilter = async () => {
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            $scope.filters = new Filters();
            if (!!$scope.campaign.catalog && $scope.filters.all.length == 0) initFilters();
            for (const key of Object.keys($scope.catalog)) {
                $scope.catalog[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.correlationFilterES[key];
                    newFilter.value = item.name;
                    const alreadyInFilters = $scope.filters.all.filter(f => f.name == newFilter.name && f.value == newFilter.value);
                    if (alreadyInFilters.length == 0) {
                        $scope.filters.all.push(newFilter);
                    }
                })
            }
            $scope.$emit('eventEmitedQuery', $scope.query.word);
            $scope.$emit('eventEmitedFilters', $scope.filters);
            if ($scope.filters.all.length > 0) {
                    await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item, key): void => {
            $scope.filters = $scope.filters.all.filter(f => f.value != item.name);
            if (!!$scope.filters) {
                $scope.filters = new Filters();
            }
            $scope.$emit('eventEmitedFilters', $scope.filters);
            $scope.catalog[key] = _.without($scope.catalog[key], item);
            $scope.getFilter();
        };

        $scope.getSortName = (key): string => {
            let sortName = "name";
            if (key == "editors" || key == "disciplines") {
                sortName = "nameFormat"
            }
            return sortName;
        }

        await init();
        $scope.addFilter();
    }]);