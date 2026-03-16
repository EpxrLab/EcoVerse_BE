package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;

import java.util.List;

public interface ParentService {
    List<StudentAccountInfo> getChildren();
}
