package com.qrcodekit.app.domain.model

enum class ErrorCorrectionLevel(val displayName: String, val description: String) {
    L("低", "容量最大，纠错能力7%"),
    M("中", "容量适中，纠错能力15%"),
    Q("中高", "容量较小，纠错能力25%"),
    H("高", "容量最小，纠错能力30%")
}

enum class ChunkSize(val value: Int, val displayName: String, val isSingleQr: Boolean = false, val isCustom: Boolean = false) {
    SIZE_500(500, "500w"),
    SIZE_600(600, "600w"),
    SIZE_700(700, "700w"),
    SIZE_800(800, "800w"),
    SIZE_1500(1500, "1500w", isSingleQr = true),  // 强制单二维码，纠错等级L
    CUSTOM(-1, "自定义", isCustom = true);

    companion object {
        fun fromValue(value: Int): ChunkSize {
            if (value == -1) return CUSTOM
            return entries.find { it.value == value } ?: SIZE_700
        }
    }
}
