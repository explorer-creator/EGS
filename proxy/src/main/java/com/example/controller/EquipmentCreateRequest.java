package com.example.controller;

import java.time.LocalDate;

/**
 * 新增设备时前端提交的字段模型。
 * 对应设备管理页面中的各个输入项。
 */
public class EquipmentCreateRequest {

    /** 设备编码 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 生产厂家 */
    private String manufacturer;

    /** 品牌 */
    private String brand;

    /** 规格型号 */
    private String model;

    /** 供应商 */
    private String supplier;

    /** 生产日期，格式：yyyy-MM-dd */
    private LocalDate productionDate;

    /** 使用年限（单位：年） */
    private Integer serviceLife;

    /** 折旧方式 */
    private String depreciationMethod;

    /** 设备所在位置 */
    private String location;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public Integer getServiceLife() {
        return serviceLife;
    }

    public void setServiceLife(Integer serviceLife) {
        this.serviceLife = serviceLife;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

