app.service("brandService", function ($http) {
    this.findAll = function () {
        return $http.get("../brand/findAll.do");
    };
    this.search = function (page, rows, searchEntity) {
        return $http.post("../brand/search.do?pageNum=" + page + "&pageSize=" + rows, searchEntity);
    };
    this.add = function (entity) {
        return $http.post("../brand/add", entity);
    };
    this.update = function (entity) {
        return $http.post("../brand/update", entity);
    };
    this.findOne = function (id) {
        return $http.get("../brand/findOne.do?id=" + id);
    };
    this.delete = function (ids) {
        return $http.get("../brand/delete.do?ids=" + ids);
    };
    this.selectOptionList = function () {
        return $http.get("/brand/selectOptionList.do");
    }
});