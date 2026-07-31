package com.aidoctor.diagnosis.exception;

/**
 * Stops an unfinished clinical path when no deterministic policy exists.
 */
public class ClinicalSemanticBlockerException extends IllegalStateException {

    public ClinicalSemanticBlockerException(String message) {
        super(message);
    }
}
