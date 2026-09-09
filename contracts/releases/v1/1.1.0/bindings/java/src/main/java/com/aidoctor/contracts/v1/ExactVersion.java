package com.aidoctor.contracts.v1;

/**
 * EXACT 鐗堟湰鍗忓晢锛氭湭鐭?contract_version 蹇呴』 fail-closed銆?
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
