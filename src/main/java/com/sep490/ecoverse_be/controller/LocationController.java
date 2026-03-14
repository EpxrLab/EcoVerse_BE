package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ILocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ResponseDto<?>> getProvinces() {
        return ResponseEntity.ok(
                ResponseDto.success(ILocationService.getProvinces(), "Get the list of provinces successes"));
    }

    @GetMapping("/wards/{provinceCode}")
    public ResponseEntity<ResponseDto<?>> getWards(@PathVariable String provinceCode) {
        return ResponseEntity.ok(
                ResponseDto.success(ILocationService.getWards(provinceCode), "Get the list of wards successes"));
    }
}
