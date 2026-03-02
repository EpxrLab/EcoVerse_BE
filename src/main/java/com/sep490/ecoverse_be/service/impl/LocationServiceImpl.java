package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.service.ILocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements ILocationService {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://provinces.open-api.vn/api/v2";

    @Override
    public Object getProvinces() {
        return restTemplate.getForObject(BASE_URL + "/p/", Object.class);
    }

    @Override
    public Object getDistricts(String provinceCode) {
        return restTemplate.getForObject(BASE_URL + "/p/" + provinceCode + "?depth=2", Object.class);
    }

}
