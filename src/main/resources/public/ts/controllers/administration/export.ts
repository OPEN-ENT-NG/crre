import {moment, ng,_,idiom as lang, template, toasts} from "entcore";
import {Export, Notification, Utils, STATUS, Userbook, OrderClient} from "../../model";

declare let window: any;

export const exportCtrl = ng.controller('exportCtrl', [
    '$scope','$window', async ($scope,$window) => {
        $scope.display = {
            delete: false
        };
        $scope.search = {
            filterWord : '',
            filterWords : []
        };
        $scope.sort = {
            export : {
                type: 'created',
                reverse: true
            }
        };
        $scope.displayExports = [];
        $scope.displayExports = $scope.exports.all;
        $scope.STATUS = STATUS;

        $scope.getExport = (exportTemp: Export) => {
            if(exportTemp.status === STATUS.SUCCESS){
                 $window.open(`lystore/export/${exportTemp.fileId}`, '_blank');
            }
        };

        $scope.confirmDelete = () => {
            $scope.display.delete = true;
            $scope.exportsToDelete = $scope.exports.all.filter(exportFiltered => exportFiltered.selected && exportFiltered.status !== STATUS.WAITING);
            template.open('export.delete.lightbox', 'administrator/exports/export-lightbox-delete');
        };

        $scope.cancelExportLightbox = () => {
            $scope.exportToDelete = new Export();
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            Utils.safeApply($scope);

        };

        $scope.deleteExport = async ():Promise<void> => {
            await $scope.exports.delete( $scope.exportsToDelete
                    .map(exportMap => exportMap._id),
                $scope.exportsToDelete
                    .map(exportMap => exportMap.fileId));
            $scope.isAllExportSelected = false;
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            toasts.confirm('lystore.delete.notif');
            $scope.exportToDelete = [];
            await $scope.exports.getExports();
            $scope.filterDisplayedExports();
            Utils.safeApply($scope);

        };

        $scope.isAllExportSelected = false;
        $scope.switchAllExports = ():void => {
            $scope.isAllExportSelected  =  !$scope.isAllExportSelected;
            if ( $scope.isAllExportSelected) {
                $scope.displayExports.map(exportSelected => exportSelected.selected = true)
            } else {
                $scope.displayExports.map(exportSelected => exportSelected.selected = false)
            }
            Utils.safeApply($scope);
        };



        function generateRegexp (words: string[]): RegExp {
            function escapeRegExp(str: string) {
                return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&');
            }
            let reg;
            if (words.length > 0) {
                reg = '.*(';
                words.map((word: string) => reg += `${escapeRegExp(word.toLowerCase())}|`);
                reg = reg.slice(0, -1);
                reg += ').*';
            } else {
                reg = '.*';
            }
            return new RegExp(reg);
        }


        $scope.filterDisplayedExports = () => {
            let searchResult = [];
            let regex;
            $scope.displayExports = $scope.exports.all;
            if( $scope.search.filterWords.length > 0) {
                $scope.search.filterWords.map((searchTerm: string, index: number): void => {
                    let searchItems: Export[] = index === 0 ? $scope.displayExports : searchResult;
                    regex = generateRegexp([searchTerm]);

                    searchResult = _.filter(searchItems, (exportToHandle: Export) => {
                        return ('object_name' in exportToHandle ? regex.test(exportToHandle.object_name.toLowerCase()) : false)
                            || ('typeObject' in exportToHandle ? regex.test(lang.translate(exportToHandle.typeObject).toLowerCase()) : false)
                            || ('created' in exportToHandle ? regex.test(exportToHandle.created.toLowerCase()) : false)
                            || ('filename' in exportToHandle ? regex.test(exportToHandle.filename.toLowerCase()) : false)
                            || ('extension' in exportToHandle ? regex.test(lang.translate(exportToHandle.extension.toLowerCase())) : false)
                    });
                });
                $scope.displayExports = searchResult;
            }
            Utils.safeApply($scope);
        };

        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWords = _.without( $scope.search.filterWords , filterWord);
            $scope.isAllExportSelected = false;
            $scope.allExportsSelected = false;
            $scope.filterDisplayedExports();
        };

        $scope.controlDeleteExport = ():Boolean => {
            return $scope.exports.selected.some(exportSome => exportSome.status === STATUS.WAITING)
        };

        $scope.addFilterWords = (filterWord) => {
            if (filterWord !== '') {
                $scope.search.filterWords = _.union($scope.search.filterWords, [filterWord]);
                $scope.search.filterWord = '';
                Utils.safeApply($scope);
            }
        };

        $scope.addFilter = (filterWord: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addFilterWords(filterWord);
                $scope.filterDisplayedExports();
            }
        };
    }
]);