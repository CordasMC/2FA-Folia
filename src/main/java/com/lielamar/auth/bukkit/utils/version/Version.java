package com.lielamar.auth.bukkit.utils.version;

import org.bukkit.Bukkit;

public class Version {

    private static final Version instance = new Version();
    public static Version getInstance() { return instance; }

    private Version() {
        try {
            this.serverVersion = ServerVersion.valueOf("v" + Bukkit.getServer().getBukkitVersion().split("-")[0].replaceAll("\\.", "_"));
        } catch (Exception ignored) {
            // Fallback to the latest known version if parsing fails
            this.serverVersion = ServerVersion.v1_21_4;
        }
    }


    private ServerVersion serverVersion;

    public ServerVersion getServerVersion() { return this.serverVersion; }


    public enum ServerVersion {
        v1_7_2("v1_7_2", 10702),
        v1_7_5("v1_7_5", 10705),
        v1_7_8("v1_7_8", 10708),
        v1_7_9("v1_7_9", 10709),
        v1_7_10("v1_7_10", 10710),

        v1_8_0("v1_8_0", 10800),
        v1_8_3("v1_8_3", 10803),
        v1_8_4("v1_8_4", 10804),
        v1_8_5("v1_8_5", 10805),
        v1_8_6("v1_8_6", 10806),
        v1_8_7("v1_8_7", 10807),
        v1_8_8("v1_8_8", 10808),

        v1_9_0("v1_9_0", 10900),
        v1_9_2("v1_9_2", 10902),
        v1_9_4("v1_9_4", 10904),

        v1_10_0("v1_10_0", 11000),
        v1_10_2("v1_10_2", 11002),

        v1_11_0("v1_11_0", 11100),
        v1_11_1("v1_11_1", 11101),
        v1_11_2("v1_11_2", 11102),

        v1_12_0("v1_12_0", 11200),
        v1_12_1("v1_12_1", 11201),
        v1_12_2("v1_12_2", 11202),

        v1_13_0("v1_13_0", 11300),
        v1_13_1("v1_13_1", 11301),
        v1_13_2("v1_13_2", 11302),

        v1_14_0("v1_14_0", 11400),
        v1_14_1("v1_14_1", 11401),
        v1_14_2("v1_14_2", 11402),
        v1_14_3("v1_14_3", 11403),
        v1_14_4("v1_14_4", 11404),

        v1_15_0("v1_15_0", 11500),
        v1_15_1("v1_15_1", 11501),
        v1_15_2("v1_15_2", 11502),

        v1_16_1("v1_16_1", 11601),
        v1_16_2("v1_16_2", 11602),
        v1_16_3("v1_16_3", 11603),
        v1_16_4("v1_16_4", 11604),
        v1_16_5("v1_16_5", 11605),

        v1_17_0("v1_17_0", 11700),
        v1_17_1("v1_17_1", 11701),

        v1_18_0("v1_18_0", 11800),
        v1_18_1("v1_18_1", 11801),
        v1_18_2("v1_18_2", 11802),

        v1_19_0("v1_19_0", 11900),
        v1_19_1("v1_19_1", 11901),
        v1_19_2("v1_19_2", 11902),
        v1_19_3("v1_19_3", 11903),
        v1_19_4("v1_19_4", 11904),

        v1_20_0("v1_20_0", 12000),
        v1_20_1("v1_20_1", 12001),
        v1_20_2("v1_20_2", 12002),
        v1_20_3("v1_20_3", 12003),
        v1_20_4("v1_20_4", 12004),
        v1_20_5("v1_20_5", 12005),
        v1_20_6("v1_20_6", 12006),

        v1_21_0("v1_21_0", 12100),
        v1_21_1("v1_21_1", 12101),
        v1_21_2("v1_21_2", 12102),
        v1_21_3("v1_21_3", 12103),
        v1_21_4("v1_21_4", 12104);

        private final String versionName;
        private final int versionId;

        ServerVersion(String versionName, int versionId) {
            this.versionName = versionName;
            this.versionId = versionId;
        }

        public String getVersionName() { return this.versionName; }
        public String getStrippedName() { return this.versionName.replaceFirst("v", "").replaceAll("_", "."); }

        int getVersionId() { return this.versionId; }

        public boolean above(ServerVersion otherVersion) { return this.versionId >= otherVersion.getVersionId(); }
    }
}