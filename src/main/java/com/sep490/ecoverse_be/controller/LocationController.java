package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ILocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/locations/")
@RequiredArgsConstructor
public class LocationController {
    private final ILocationService ILocationService;

    @GetMapping("/provinces")
    public ResponseDto<?> getProvinces() {
        return new ResponseDto<>(HttpStatus.OK.value(), "Get the list of provinces successes", ILocationService.getProvinces());
    }

    @GetMapping("/districts/{provinceCode}")
    public ResponseDto<?> getDistricts(@PathVariable String provinceCode) {
        return new ResponseDto<>(HttpStatus.OK.value(), "Get the list of districts successes", ILocationService.getDistricts(provinceCode));
    }
}
