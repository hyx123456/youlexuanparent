app.controller("brandController", function ($scope, $http, $controller, brandService) {
    $controller('baseController', {$scope: $scope});
    $scope.findAll = function () {
        brandService.success(function (response) {
            $scope.list = response;
        });
    };

    $scope.searchEntity = {};
    //搜索查询
    $scope.search = function (page, rows) {

        brandService.search(page, rows, $scope.searchEntity).success(function (response) {
            $scope.list = response.rows;
            $scope.paginationConf.totalItems = response.total;
        });
    };

    //保存
    $scope.save = function () {
        if ($scope.entity.id != null) {
            brandService.add($scope.entity).success(function (response) {
                if (response.success) {
                    $scope.reloadList();
                } else {
                    alert(response.message);
                }
            });
        } else {
            brandService.update($scope.entity).success(function (response) {
                if (response.success) {
                    $scope.reloadList();
                } else {
                    alert(response.message);
                }
            });
        }
    };


    //根据ID回显数据
    $scope.findOne = function (id) {
        brandService.findOne(id).success(function (response) {
            $scope.entity = response;
        });
    };

    //删除
    $scope.delete = function () {
        brandService.delete($scope.selectIds).success(function (response) {
            if (response.success) {
                $scope.reloadList();
            } else {
                alert(response.message);
            }
        });
    };

});