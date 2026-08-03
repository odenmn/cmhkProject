package com.cmhk.business.module.auth.dto;

public record ChannelEntryContextResponse(
        String entryToken,
        String entryName,
        String channelName,
        Integer elderlyMode
) {
}
