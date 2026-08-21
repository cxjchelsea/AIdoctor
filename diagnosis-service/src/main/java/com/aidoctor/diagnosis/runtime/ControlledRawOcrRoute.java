package com.aidoctor.diagnosis.runtime;

/**
 * 有界 Raw OCR 受控路由状态。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER。
 * 本枚举不是遗留 OCR 与 Python 之间的选择器，也不表达自动回退。
 * 网关自身只处于关闭，或显式将有界 Raw OCR 路由到 Python Runtime。
 */
public enum ControlledRawOcrRoute {
    DISABLED,
    PYTHON_RUNTIME_CONTROLLED
}
