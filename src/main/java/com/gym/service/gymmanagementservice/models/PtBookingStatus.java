package com.gym.service.gymmanagementservice.models;

public enum PtBookingStatus {
    REQUESTED_BY_MEMBER, // Hội viên yêu cầu
    CONFIRMED_BY_PT,     // PT đã xác nhận
    COMPLETED,           // Buổi tập đã hoàn thành (sẽ trừ buổi)
    CANCELED_BY_MEMBER,  // Hội viên hủy
    CANCELED_BY_PT,      // PT hủy
    NO_SHOW              // Hội viên không đến
}