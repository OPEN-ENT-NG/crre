import {_, ng} from 'entcore';
import http from 'axios';

export const uploadFiles = ng.directive("uploadFiles", function () {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=?',
            uploadUri: '=',
            end: '=?',
            ngChange: '&'
        },
        templateUrl: '/lystore/public/template/directives/upload-files/main.html',
        controller: ['$scope', '$element', '$attrs', function ($scope, element, attributes) {
            $scope.upload = {
                loading: false,
                documents: []
            };
            if (!($scope.upload.documents instanceof Array)) {
                $scope.upload.documents = [];
            }

            element.on('dragenter', (e) => {
                e.preventDefault();
            });

            element.on('dragover', (e) => {
                element.find('.drop-zone').addClass('dragover');
                e.preventDefault();
            });

            element.on('dragleave', () => {
                element.find('.drop-zone').removeClass('dragover');
            });

            element.on('drop', async (e) => {
                element.find('.drop-zone').removeClass('dragover');
                e.preventDefault();
                $scope.importFiles(e.originalEvent.dataTransfer.files);
            });

            $scope.$watch('ngModel', function () {
                $scope.upload.documents = [];
            });

            $scope.importFiles = (files = $scope.files) => {
                let file: File;
                if (files === undefined || files.length === 0) return;
                $scope.upload.loading = true;
                $scope.$apply();
                for (let i = 0; i < files.length; i++) {
                    file = files[i];
                    $scope.upload.documents.push(file);
                    $scope.uploadFile(file);
                }
            };

            $scope.uploadFile = async (file) => {
                file.status = 'loading';
                let formData = new FormData();
                formData.append("file", file, file.name);
                try {
                    const {data} = await http.post($scope.uploadUri, formData, {'headers': {'Content-Type': 'multipart/form-data'}});
                    file.id = data.id;
                    file.status = 'loaded';
                } catch (err) {
                    file.status = 'failed';
                }
                $scope.$apply();
            };

            $scope.endUpload = () => {
                $scope.$apply();
                setTimeout(function () {
                    $scope.$parent.$eval(attributes.ngModel);
                    $scope.end($scope.upload.documents);
                    $scope.upload.loading = false;
                    $scope.upload.documents = [];
                }, 0);
            };

            $scope.delete = async (doc) => {
                try {
                    const {id} = doc;
                    await http.delete(`${$scope.uploadUri}/${id}`);
                    $scope.upload.documents = _.reject($scope.upload.documents, (doc) => doc.id === id);
                    if ($scope.upload.documents.length === 0) $scope.upload.loading = false;
                    $scope.$apply();
                } catch (e) {
                    throw e;
                }
            };

        }]
    };
});