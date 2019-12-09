app.controller("searchController", function ($scope, $location, searchService) {

    //加载查询字符串
    $scope.loadkeywords = function () {
        $scope.searchMap.keywords = $location.search()['keywords'];
        $scope.search();
    };

    //搜索
    $scope.search = function () {
        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);
        searchService.search($scope.searchMap).success(function (response) {
            //搜索返回的结果
            $scope.resultMap = response;
            //调用分页
            buildPageLabel();
        });
    };

    //搜索对象
    $scope.searchMap = {
        'keywords': '',
        'category': '',
        'brand': '',
        'spec': {},
        'price': '',
        'pageNo': 1,
        'pageSize': 20,
        'sortField': '',
        'sort': ''
    };

    //添加搜索项
    $scope.addSearchItem = function (key, value) {
        //如果点击的是分类或者是品牌
        if (key == 'category' || key == 'brand' || key == 'price') {
            $scope.searchMap[key] = value;
        } else {
            $scope.searchMap.spec[key] = value;
        }
        //执行搜索
        $scope.search();
    };

    //移除复合搜索条件
    $scope.removeSearchItem = function (key) {
        //如果是分类或品牌
        if (key == 'category' || key == 'brand' || key == 'price') {
            $scope.searchMap[key] = "";
        } else {
            //否则是规格，移除此属性
            delete $scope.searchMap.spec[key];
        }
        //执行搜索
        $scope.search();
    };

    //构建分页标签
    buildPageLabel = function () {
        //新增分页栏属性
        $scope.pageLabel = [];
        //得到最后页码
        var maxPageNo = $scope.resultMap.totalPages;
        //开始页码
        var firstPage = 1;
        //截止页码
        var lastPage = maxPageNo;
        //前面有点
        $scope.firstDot = true;
        //后面有点
        $scope.lastDot = true;
        //如果总页数大于5页，显示部分页码
        if ($scope.resultMap.totalPages > 5) {
            //如果当前页小于等于3
            if ($scope.searchMap.pageNo <= 3) {
                //前5页
                lastPage = 5;
                //前面没点
                $scope.firstDot = false;
                //如果当前页大于等于最大页码-2
            } else if ($scope.searchMap.pageNo >= lastPage - 2) {
                firstPage = maxPageNo - 4;
                //后面没点
                $scope.lastDot = false;
            } else {
                //显示当前页为中心的5页
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        } else {
            $scope.firstDot = false;
            $scope.lastDot = false;
        }
        //循环产生页码标签
        for (var i = firstPage; i <= lastPage; i++) {
            $scope.pageLabel.push(i);
        }
    };

    $scope.queryByPage = function (pageNo) {
        //页码验证
        if (pageNo < 1 || pageNo > $scope.resultMap.totalPages) {
            return;
        }
        $scope.searchMap.pageNo = pageNo;
        $scope.search();
    };

    //判断当前页为第一页
    $scope.isToPage = function () {
        if ($scope.searchMap.pageNo == 1) {
            return true;
        } else {
            return false;
        }
    };

    //判断当前页是否为最后一页
    $scope.isEndPage = function () {
        if ($scope.searchMap.pageNo == $scope.resultMap.totalPages) {
            return true;
        } else {
            return false;
        }
    };

    //判断指定页码是否为当前页
    $scope.isPage = function (p) {
        if (parseInt(p) == parseInt($scope.searchMap.pageNo)) {
            return true;
        } else {
            return false;
        }
    };

    //设置排序规则
    $scope.sortSearch = function (sortField, sort) {
        $scope.searchMap.sortField = sortField;
        $scope.searchMap.sort = sort;
        $scope.search();
    };

    //判断关键字是不是品牌
    $scope.keywordsIsBrand = function () {
        for (var i = 0; i < $scope.resultMap.brandList.length; i++) {
            if ($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text) > -1) {
                return true;
            }
        }
        return false;
    }
});