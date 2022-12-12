import {ng} from 'entcore';
import * as ApexCharts from 'apexcharts';
import {Utils} from '../../../model';

export const statsGlobalController = ng.controller('statsGlobalController', [
    '$scope', async ($scope) => {

        $scope.reassorts = [{name: 'true'}, {name: 'false'}];
        $scope.schoolOrientation = [{name: 'LG'}, {name: 'LP'}];

        $scope.reassorts.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.schoolOrientation.forEach((item) => item.toString = () => $scope.translate(item.name));

        const init = async () => {
            await $scope.initFilter();
            await $scope.initYear();
            await $scope.stats.get($scope.filters);
            Utils.safeApply($scope);
            // Init filter as last year
            $scope.filterChoice.years.push($scope.years[0]);
            initCharts();
            Utils.safeApply($scope);
        };

        $scope.getPublicTotal = (field, publics) => {
            let total = 0;
            if (field.find(r => r.public === publics)) {
                total = field.find(r => r.public === publics).total;
            }
            return total;
        }

        $scope.getPublicPercentage = (field, publics) => {
            let total = 0;
            if (field.find(r => r.public === publics)) {
                total = field.find(r => r.public === publics).percentage;
            }
            return total;
        }

        $scope.isPublic = (publics) => {
            return !!$scope.filterChoice.schoolType.find(r => r.name === publics) || $scope.filterChoice.schoolType.length == 0;
        }

        $scope.getFilter = async () => {
            $scope.getAllFilter();
            await $scope.stats.get($scope.filters);
            initCharts();
            Utils.safeApply($scope);
        }

        // Charts

        const initCharts = (): void => {
            initRessourcesChart();
            initLicenceChart();
            initStructureChart();
        }

        const initRessourcesChart = (): void => {
            let data = [];
            if ($scope.filterChoice.schoolType.length == 0 || $scope.filterChoice.schoolType.length == 2) {
                data.push($scope.getPublicTotal($scope.stats.allPaperRessources, 'Public') + $scope.getPublicTotal($scope.stats.allPaperRessources, 'Privé'));
                data.push($scope.getPublicTotal($scope.stats.allNumericRessources, 'Public') + $scope.getPublicTotal($scope.stats.allNumericRessources, 'Privé'))
            } else {
                data.push($scope.getPublicTotal($scope.stats.allPaperRessources, $scope.filterChoice.schoolType[0].name));
                data.push($scope.getPublicTotal($scope.stats.allNumericRessources, $scope.filterChoice.schoolType[0].name));
            }
            let xaxis = {
                categories: [
                    ["Papier"],
                    ["Numérique"]
                ],
                labels: {
                    style: {
                        colors: [
                            "#1794a5",
                            "#f71c35"
                        ],
                        fontSize: "18px"
                    }
                }
            };

            let series = [{data: data}];


            // Generate options with labels and colors
            let options = {
                chart: {
                    type: 'bar',
                    height: 350,
                    width: 400
                },
                responsive: [
                    {
                        breakpoint: 1500,
                        options: {
                            chart: {
                                width: 300
                            }
                        }
                    }
                ],
                colors: ["#1794a5", "#f71c35"],
                plotOptions: {
                    bar: {
                        columnWidth: "100%%",
                        distributed: true
                    }
                },
                dataLabels: {
                    enabled: true
                },
                legend: {
                    show: false
                },
                grid: {
                    show: false
                }
            };


            // Generate chart with options and data
            if ($scope.ressourceChart) {
                $scope.ressourceChart.destroy();
            }
            var newOptions = JSON.parse(JSON.stringify(options));
            newOptions.series = series;
            newOptions.xaxis = xaxis;
            $scope.ressourceChart = new ApexCharts(document.querySelector('#bar-chart'), newOptions);
            $scope.ressourceChart.render();


        }

        const initLicenceChart = (): void => {
            let series = [];
            let statLicence = 0;
            if ($scope.stats.licences.initial_amount > 0) {
                statLicence = parseFloat(($scope.stats.licences.amount / $scope.stats.licences.initial_amount * 100).toFixed(2));
            }
            series.push(statLicence);

            // Generate options with labels and colors
            let options = {
                chart: {
                    type: 'radialBar',
                    height: 350,
                    width: 400
                },
                responsive: [
                    {
                        breakpoint: 1500,
                        options: {
                            chart: {
                                width: 300
                            }
                        }
                    }
                ],
                plotOptions: {
                    radialBar: {
                        startAngle: -90,
                        endAngle: 90,
                        track: {
                            background: "#d0dfe6",
                            strokeWidth: "97%",
                            margin: 5, // margin is in pixels
                            dropShadow: {
                                enabled: true,
                                top: 2,
                                left: 0,
                                opacity: 0.31,
                                blur: 2
                            }
                        },
                        dataLabels: {
                            name: {
                                show: false
                            },
                            value: {
                                offsetY: -2,
                                fontSize: "22px"
                            }
                        }
                    }
                },
                fill: {
                    colors: ["#1794a5"]
                },
                labels: ["Licences consommées"]
            };


            // Generate chart with options and data
            if ($scope.licenceChart) {
                $scope.licenceChart.destroy();
            }
            var newOptions = JSON.parse(JSON.stringify(options));
            newOptions.series = series;
            $scope.licenceChart = new ApexCharts(document.querySelector('#radial-chart'), newOptions);
            $scope.licenceChart.render();
        }

        const initStructureChart = (): void => {
            let series = [];
            if ($scope.filterChoice.schoolType.length == 0 || $scope.filterChoice.schoolType.length == 2) {
                let structureOrder = (parseFloat($scope.getPublicPercentage($scope.stats.structuresMoreOneOrder, 'Public')) +
                    parseFloat($scope.getPublicPercentage($scope.stats.structuresMoreOneOrder, 'Privé'))) / 2;
                series.push(structureOrder);
                series.push(100 - structureOrder);
            } else {
                let structureOrder = parseFloat($scope.getPublicPercentage($scope.stats.structuresMoreOneOrder, $scope.filterChoice.schoolType[0].name));
                series.push(structureOrder);
                series.push(100 - structureOrder);
            }

            // Generate options with labels and colors
            let options = {
                colors: ["#f71c35", "#1794a5"],
                series: series,
                chart: {
                    type: "donut",
                    height: 450,
                    width: 500
                },
                labels: ["Établissements ayant commandé", "Établissements sans commande"],
                responsive: [
                    {
                        breakpoint: 1500,
                        options: {
                            chart: {
                                width: 300
                            },
                            legend: {
                                position: "bottom"
                            }
                        }
                    }
                ]
            };

            // Generate chart with options and data
            if ($scope.structureChart) {
                $scope.structureChart.destroy();
            }
            var newOptions = JSON.parse(JSON.stringify(options));
            newOptions.series = series;
            $scope.structureChart = new ApexCharts(document.querySelector('#donut-chart'), newOptions);
            $scope.structureChart.render();
        }

        await init();
        Utils.safeApply($scope);
    }
]);