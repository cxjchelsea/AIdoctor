/**
 * PBNC-01 State Committer mechanical core.
 *
 * <p>Pure Java, constructor-injected ports, no Spring stereotype, no HTTP,
 * no JPA, no CDPManager, no Clinical Runtime, and no Shared Contract fork.
 *
 * <p>Placement is under {@code diagnosis-service} because the transitional
 * CDP / SoR path is Java-owned. This package is not Agent Runtime.
 */
package com.aidoctor.diagnosis.state.committer;
