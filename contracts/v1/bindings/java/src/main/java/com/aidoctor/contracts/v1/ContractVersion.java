package com.aidoctor.contracts.v1;

/**
 * REVIEWED_BINDING 版本戳。必须与 contracts/v1/manifest.json 同步。
 * 由 tooling/sync_version.py 写入；禁止独立修改。
 */
public final class ContractVersion {
    public static final String CONTRACT_VERSION = "1.0.0";
    public static final String SCHEMA_FAMILY = "v1";
    public static final String VERSION_NEGOTIATION = "EXACT";

    private ContractVersion() {
    }
}
