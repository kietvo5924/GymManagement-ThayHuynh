package com.gym.service.gymmanagementservice.models;

public enum CheckInStatus {
    SUCCESS,                // Thành công
    FAILED_MEMBER_NOT_FOUND, // Không tìm thấy hội viên
    FAILED_NO_ACTIVE_PACKAGE // Hội viên không có gói tập nào đang hoạt động
}
