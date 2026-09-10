package com.aidoctor.contracts.v1;

/**
 * EXACT 版本协商：未知 contract_version 必须 fail-closed。
 */
public final class ExactVersion {
    private ExactVersion() {
    }

    public static void requireExact(String contractVersion) {
        if (!ContractVersion.CONTRACT_VERSION.equals(contractVersion)) {
            throw new IllegalArgumentException(
                "unknown contract_version rejected: " + contractVersion
            );
        }
    }
}
