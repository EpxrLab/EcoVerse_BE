package com.sep490.ecoverse_be.service;

public interface ILocationService {
    Object getProvinces();

    Object getWards(String provinceCode);
}
