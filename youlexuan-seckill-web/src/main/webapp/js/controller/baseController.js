app.controller("baseController", function ($scope) {

    //切换页码
    $scope.reloadList = function () {
        $scope.search($scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
    };

    //分页控件配置
    $scope.paginationConf = {
        currentPage: 1,//当前页码
        totalItems: 10,//总条数
        itemsPerPage: 10,
        perPageOptions: [10, 20, 30, 40, 50],//页码选项
        onChange: function () {//触发事件
            $scope.reloadList();
        }
    };

    //分页
    $scope.findPage = function (page, rows) {
        $http.get("../brand/findPage.do?pageNum=" + page + "&pageSize=" + rows).success(function (response) {
            $scope.list = response.rows;
            $scope.paginationConf.totalItems = response.total;
        });
    };

    //保存选中的目标到数组中
    $scope.selectIds = [];
    $scope.updateSelection = function ($event, id) {
        if ($event.target.checked) {
            $scope.selectIds.push(id);
        } else {
            var idx = $scope.selectIds.indexOf(id);
            $scope.selectIds.splice(idx, 1);
        }
    };

    //全选
    $scope.selectAll = function ($event) {
        if ($event.target.checked) {
            for (var i = 0; i < $scope.list.length; i++) {
                $scope.selectIds.push($scope.list[i].id);
            }
        } else {
            $scope.selectIds = [];
        }
    };
    $scope.ischeck = function (id) {
        for (var i = 0; i < $scope.selectIds.length; i++) {
            if ($scope.selectIds[i] == id) {
                return true;
            }
        }
        return false;
    };

    //提取json字符串数据中某个属性，返回拼接字符串以逗号分隔
    $scope.jsonToString = function (jsonString, key) {
        var value = "";
        var json = JSON.parse(jsonString);
        for (var i = 0; i < json.length; i++) {
            if (i > 0) {
                value += ",";
            }
            value += json[i][key];
        }
        return value;
    }
});