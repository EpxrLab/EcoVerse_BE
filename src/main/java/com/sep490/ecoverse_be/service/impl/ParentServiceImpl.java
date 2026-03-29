package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParentServiceImpl implements ParentService {
    @Autowired
    private ParentRepository parentRepository;
    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Override
    public List<StudentAccountInfo> getChildren() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = principal.getUser();
        Parent parent = parentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));
        List<StudentParentLink> studentParentLinks = studentParentLinkRepository.findByParentId(parent.getId());
        List<StudentAccountInfo> studentAccountInfos = new ArrayList<>();
        for (StudentParentLink studentParentLink : studentParentLinks) {
            StudentAccountInfo studentAccountInfo = new StudentAccountInfo();
            studentAccountInfo.setStudentId(studentParentLink.getStudent().getId());
            studentAccountInfo.setStudentCode(studentParentLink.getStudent().getStudentCode());
            studentAccountInfo.setStudentFullName(studentParentLink.getStudent().getFullName());
            studentAccountInfo.setClassName(studentParentLink.getStudent().getClassName());
            studentAccountInfo.setGradeLevel(studentParentLink.getStudent().getGradeLevel());
            studentAccountInfo.setGender(studentParentLink.getStudent().getGender());
            studentAccountInfo.setAddress(studentParentLink.getStudent().getAddress());
            studentAccountInfo.setDob(studentParentLink.getStudent().getDateOfBirth());
            studentAccountInfo.setTotalCoin(studentParentLink.getStudent().getTotalCoins());
            studentAccountInfo.setAccountStatus(studentParentLink.getStudent().getUser().getStatus());
            studentAccountInfo.setActive(studentParentLink.getStudent().getUser().getIsActive());
            studentAccountInfos.add(studentAccountInfo);
        }
        return studentAccountInfos;
    }
}
