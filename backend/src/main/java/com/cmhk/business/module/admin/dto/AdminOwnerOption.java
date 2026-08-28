package com.cmhk.business.module.admin.dto;

/** 客户负责人下拉选项，不暴露管理员联系方式等无关信息。 */
public record AdminOwnerOption(Long id, String username, String displayName) {
}
