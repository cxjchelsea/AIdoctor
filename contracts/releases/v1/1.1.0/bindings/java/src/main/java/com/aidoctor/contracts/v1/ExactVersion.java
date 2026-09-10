package com.aidoctor.contracts.v1;

/** Exact-version guard for fail-closed contract_version checks. */
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
