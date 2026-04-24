package com.qrcodekit.app.domain.model

enum class ErrorCorrectionLevel(val displayName: String, val description: String) {
    L("低", "容量最大，纠错能力7%"),
    M("中", "容量适中，纠错能力15%"),
    Q("中高", "容量较小，纠错能力25%"),
    H("高", "容量最小，纠错能力30%")
}

enum class ChunkSize(val value: Int, val displayName: String, val isSingleQr: Boolean = false) {
    SIZE_500(500, "500字"),
    SIZE_600(600, "600字"),
    SIZE_700(700, "700字"),
    SIZE_800(800, "800字"),
    SIZE_1500(1500, "1500字", isSingleQr = true);  // 强制单二维码，纠错等级L

    companion object {
        fun fromValue(value: Int): ChunkSize {
            return entries.find { it.value == value } ?: SIZE_700
        }
    }
}
