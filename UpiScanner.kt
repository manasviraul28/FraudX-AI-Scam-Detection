package com.fraudx.app

object UpiScanner {

    fun isUpiLink(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("upi://") ||
                lower.contains("upi://pay") ||
                lower.contains("gpay://") ||
                lower.contains("paytm://") ||
                lower.contains("phonepe://") ||
                lower.contains("bhim://") ||
                lower.contains("pa=") ||
                lower.contains("pn=") ||
                lower.contains("am=") ||
                (lower.contains("upi") && lower.contains("collect")) ||
                (lower.contains("upi") && lower.contains("pay"))
    }

    fun analyzeUpiLink(text: String): String {
        val lower = text.lowercase()
        var score = 0
        val warnings = mutableListOf<String>()

        if (lower.contains("collect") || lower.contains("tr=collect")) {
            score += 5
            warnings.add("Collect request — money will be debited from YOU")
        }

        if (!lower.contains("@ybl") && !lower.contains("@oksbi") &&
            !lower.contains("@okaxis") && !lower.contains("@okhdfcbank") &&
            lower.contains("@")) {
            score += 2
            warnings.add("Unknown UPI handle")
        }

        val amountMatch = Regex("am=([0-9]+)").find(text)
        if (amountMatch != null) {
            val amount = amountMatch.groupValues[1].toIntOrNull() ?: 0
            if (amount > 10000) { score += 3; warnings.add("Large amount: ₹$amount") }
            else score += 1
        }

        val suspicious = listOf("prize","lottery","winner","reward","free","govt","bank","verify","kyc","urgent")
        for (kw in suspicious) {
            if (lower.contains(kw)) { score += 2; warnings.add("Suspicious keyword: '$kw'"); break }
        }

        return when {
            score >= 5 -> "🔴 HIGH RISK UPI — ${warnings.joinToString("; ")}"
            score >= 2 -> "🟡 MEDIUM RISK UPI — ${warnings.joinToString("; ")}"
            else       -> "🟢 LOW RISK UPI — Appears legitimate"
        }
    }
}