package com.aidoctor.diagnosis.runtime.u03;

/**
 * P05 persistence boundary for non-authoritative U03 runtime trace facts.
 *
 * <p>Implementations may persist/forward trace facts, but they have no Clinical
 * State mutation authority and cannot change C02, D09, K09, or commit outcomes.</p>
 */
public interface U03RuntimeTracePort {
    void record(U03RuntimeTraceRecord record);
}
