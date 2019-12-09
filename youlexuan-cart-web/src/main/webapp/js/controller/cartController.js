app.controller('cartController', function ($scope, $controller, cartService) {
    $controller('addressController', {$scope: $scope});//继承
    //查看购物车列表
    $scope.findCartList = function () {
        cartService.findCartList().success(function (response) {
            $scope.cartList = response;
            $scope.totalValue = cartService.sum($scope.cartList);//求合计数
        })
    };

    //添加商品到购物车
    $scope.addGoodsToCartList = function (itemId, num) {
        cartService.addGoodsToCartList(itemId, num).success(function (response) {
            if (response.success) {
                $scope.findCartList();
            } else {
                alert(response.message);
            }
        })
    };

    //获取地址列表
    $scope.findAddressList = function () {
        cartService.findAddressList().success(function (response) {
            $scope.addressList = response;
            //设置默认地址
            for (var i = 0; i < $scope.addressList.length; i++) {
                if ($scope.addressList[i].isDefault == '1') {
                    $scope.address = $scope.addressList[i];
                    break;
                }
            }
        })
    };

    //选择地址
    $scope.selectAddress = function (address) {
        $scope.address = address;
    };

    //判断是否是当前选中的地址
    $scope.isSelectedAddress = function (address) {
        if (address == $scope.address) {
            return true;
        } else {
            return false;
        }
    };

    $scope.order = {paymentType: '1'};
    //选择支付方式
    $scope.selectPayType = function (type) {
        $scope.order.paymentType = type;
    };

    //保存订单
    $scope.submitOrder = function () {
        $scope.order.receiverAreaName = $scope.address.address;//地址
        $scope.order.receiverMobile = $scope.address.mobile;//手机
        $scope.order.receiver = $scope.address.contact;//联系人
        cartService.submitOrder($scope.order).success(function (response) {
            if (response.success) {
                if ($scope.order.paymentType == '1') {
                    location.href = 'pay.html';
                } else {
                    location.href = 'paysuccess.html';
                }
            } else {
                alert(response.message);
            }
        })
    };

    $scope.findAllProvinces = function () {
        cartService.findAllProvinces().success(function (response) {
            $scope.provincesList = response;
        });
    };

    $scope.$watch('entity.provinceId', function (provinceid) {
        if (provinceid) {
            cartService.byProvinceIdFindCities(provinceid).success(function (response) {
                $scope.citiesList = response;
            })
        }
    });

    $scope.$watch('entity.cityId', function (cityid) {
        if (cityid) {
            cartService.byCityIdFindAreas(cityid).success(function (response) {
                $scope.areasList = response;
            })
        }
    });
});