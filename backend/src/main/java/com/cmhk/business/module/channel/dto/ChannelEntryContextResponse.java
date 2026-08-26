package com.cmhk.business.module.channel.dto;

public record ChannelEntryContextResponse(
        String entryToken,
        String entryName,
        String channelName,
        Integer elderlyMode
) {
}
