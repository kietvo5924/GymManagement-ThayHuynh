package com.gym.service.gymmanagementservice.models;

public enum PackageAccessType {
    SINGLE_CLUB,    // Gói chỉ có giá trị tại 1 CLB (CLB được định nghĩa trong targetClub)
    ALL_CLUBS       // Gói có giá trị tại tất cả các CLB
}