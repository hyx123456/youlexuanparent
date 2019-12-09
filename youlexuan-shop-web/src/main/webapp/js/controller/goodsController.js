//控制层
app.controller('goodsController', function ($scope, $location, $controller, goodsService, uploadService, itemCatService, typeTemplateService) {

    $controller('baseController', {$scope: $scope});//继承

    //读取列表数据绑定到表单中  
    $scope.findAll = function () {
        goodsService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    };

    //分页
    $scope.findPage = function (page, rows) {
        goodsService.findPage(page, rows).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    };

    //查询实体
    $scope.findOne = function (id) {
        var id = $location.search()['id'];
        if (id == null) {
            return;
        }
        goodsService.findOne(id).success(
            function (response) {
                $scope.entity = response;
                editor.html(response.goodsDesc.introduction);
                $scope.entity.goodsDesc.itemImages = JSON.parse(response.goodsDesc.itemImages);
                $scope.entity.goodsDesc.customAttributeItems = JSON.parse(response.goodsDesc.customAttributeItems);
                $scope.entity.goodsDesc.specificationItems = JSON.parse(response.goodsDesc.specificationItems);
                for (var i = 0; i < response.itemList.length; i++) {
                    $scope.entity.itemList[i].spec = JSON.parse(response.itemList[i].spec);
                }
            }
        );
    };

    //保存
    $scope.save = function () {
        $scope.entity.goodsDesc.introduction = editor.html();
        var serviceObject;//服务层对象
        if ($scope.entity.goods.id != null) {//如果有ID
            serviceObject = goodsService.update($scope.entity); //修改
        } else {
            serviceObject = goodsService.add($scope.entity);//增加
        }
        serviceObject.success(
            function (response) {
                if (response.success) {
                    alert("保存成功");
                    $scope.entity = {};
                    editor.html("");
                    location.href = "goods.html";
                } else {
                    alert(response.message);
                }
            }
        );
    };


    //批量删除
    $scope.dele = function () {
        //获取选中的复选框
        goodsService.dele($scope.selectIds).success(
            function (response) {
                if (response.success) {
                    $scope.reloadList();//刷新列表
                    $scope.selectIds = [];
                }
            }
        );
    };

    $scope.searchEntity = {};//定义搜索对象

    //搜索
    $scope.search = function (page, rows) {
        goodsService.search(page, rows, $scope.searchEntity).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    };

    //添加
    $scope.add = function () {
        $scope.entity.goodsDesc.introduction = editor.html();
        goodsService.add($scope.entity).success(function (response) {
            if (response.success) {
                alert(response.message);
                $scope.entity = {};
                editor.html('');
            } else {
                alert(response.message);
            }
        });
    };

    //$scope.image_entity = {};
    //上传图片
    $scope.uploadFile = function () {
        uploadService.upload().success(function (response) {
            if (response.success) {
                $scope.image_entity.url = response.message;
            } else {
                alert(response.message);
            }
        }).error(function () {
            alert("上传发生错误");
        });
    };

    $scope.entity = {goods: {}, goodsDesc: {itemImages: []}};
    //添加图片列表
    $scope.add_image_entity = function () {
        $scope.entity.goodsDesc.itemImages.push($scope.image_entity);
    };
    //移除图片列表
    $scope.remove_image_entity = function (index) {
        $scope.entity.goodsDesc.itemImages.splice(index, 1);
    };

    //读取一级分类
    $scope.selectItemCat1List = function () {
        itemCatService.findByParentId(0).success(function (response) {
            $scope.itemCat1List = response;
        });
    };

    //读取二级分类
    $scope.$watch("entity.goods.category1Id", function (parentId) {
        itemCatService.findByParentId(parentId).success(function (response) {
            $scope.itemCat2List = response;
        });
    });
    //读取三级分类
    $scope.$watch("entity.goods.category2Id", function (parentId) {
        itemCatService.findByParentId(parentId).success(function (response) {
            $scope.itemCat3List = response;
        });
    });
    //读取模板ID
    $scope.$watch("entity.goods.category3Id", function (parentId) {
        itemCatService.findOne(parentId).success(function (response) {
            $scope.entity.goods.typeTemplateId = response.typeId;
        });
    });
    //读取品牌列表
    $scope.$watch("entity.goods.typeTemplateId", function (typeId) {
        typeTemplateService.findOne(typeId).success(function (response) {
            $scope.typeTemplate = response;
            $scope.typeTemplate.brandIds = JSON.parse(response.brandIds);
            if ($location.search()['id'] == null) {
                $scope.entity.goodsDesc.customAttributeItems = JSON.parse(response.customAttributeItems);
            }
        });
        typeTemplateService.findSpecList(typeId).success(function (response) {
            $scope.specList = response;
        });
    });

    $scope.entity = {goodsDesc: {itemImages: [], specificationItems: []}};

    $scope.updateSpecAttribute = function ($event, name, value) {
        var object = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems, "attributeName", name);
        if (object != null) {
            if ($event.target.checked) {
                object.attributeValue.push(value);
            } else {
                object.attributeValue.splice(object.attributeValue.indexOf(value), 1);
                if (object.attributeValue.length == 0) {
                    $scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(object), 1);
                }
            }
        } else {
            $scope.entity.goodsDesc.specificationItems.push({"attributeName": name, "attributeValue": [value]});
        }
    };

    $scope.createItemList = function () {
        $scope.entity.itemList = [{spec: {}, price: 0, num: 9999, status: '0', isDefault: '0'}];
        var items = $scope.entity.goodsDesc.specificationItems;
        for (var i = 0; i < items.length; i++) {
            $scope.entity.itemList = addColumn($scope.entity.itemList, items[i].attributeName, items[i].attributeValue);
        }
    };
    addColumn = function (list, columnName, columnValues) {
        var newList = [];
        for (var i = 0; i < list.length; i++) {
            var oldRow = list[i];
            for (var j = 0; j < columnValues.length; j++) {
                var newRow = JSON.parse(JSON.stringify(oldRow));
                newRow.spec[columnName] = columnValues[j];
                newList.push(newRow);
            }
        }
        return newList;
    };

    $scope.state = ['未审核', '已审核', '审核未通过', '关闭'];

    $scope.itemCatList = [];
    $scope.findItemCatList = function () {
        itemCatService.findAll().success(function (response) {
            for (var i = 0; i < response.length; i++) {
                $scope.itemCatList[response[i].id] = response[i].name;
            }
        });
    };

    $scope.checkAttributeValue = function (specName, optionName) {
        var item = $scope.entity.goodsDesc.specificationItems;
        var object = $scope.searchObjectByKey(item, "attributeName", specName);
        if (object == null) {
            return false;
        } else {
            if (object.attributeValue.indexOf(optionName) >= 0) {
                return true;
            } else {
                return false;
            }
        }
    };


});	