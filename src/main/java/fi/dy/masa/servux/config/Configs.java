package fi.dy.masa.servux.config;

import com.google.common.collect.ImmutableList;

public class Configs
{
    public static class Generic
    {
        public static final BooleanConfig PLACEMENT_PROTOCOL_ENABLED = new BooleanConfig("placementProtocolEnabled", true);
        public static final BooleanConfig PLACEMENT_PROTOCOL_V3 = new BooleanConfig("placementProtocolV3", false);

        public static final ImmutableList<BaseConfig> OPTIONS = ImmutableList.of(
                PLACEMENT_PROTOCOL_ENABLED,
                PLACEMENT_PROTOCOL_V3
        );
    }
}
