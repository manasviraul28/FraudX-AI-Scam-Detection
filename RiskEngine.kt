package com.fraudx.app

object RiskEngine {

    // These words alone = instant HIGH RISK
    private val highRiskKeywords = listOf(
        "otp", "win", "winner", "won", "prize", "lottery",
        "claim", "reward", "free", "urgent", "immediately",
        "suspend", "block", "expire", "kyc", "aadhar", "pan",
        "password", "pin", "verify", "upi", "collect",
        "click here", "bit.ly", "tinyurl", "http", "www",
        "bank account", "transfer", "cashback", "refund",
        "income tax", "trai", "police", "arrest", "legal action",
        "court", "emi", "loan approved", "credit card",
        "scam", "fraud", "hack", "phishing"
    )

    // These words add to score
    private val mediumRiskKeywords = listOf(
        "payment", "bank", "account", "wallet", "money",
        "call back", "customer care", "helpline", "offer",
        "discount", "deal", "limited", "exclusive", "selected",
        "congratulations", "dear customer", "dear user",
        "sbi", "hdfc", "icici", "axis", "kotak",
        "paytm", "gpay", "phonepe", "amazon", "flipkart",
        "google", "microsoft", "apple", "netflix"
    )

    fun calculateRisk(input: String): String {
        val text = input.lowercase().trim()
        var score = 0

        // Check high risk keywords — each one adds 4 points
        for (keyword in highRiskKeywords) {
            if (text.contains(keyword)) {
                score += 4
            }
        }

        // Check medium risk keywords — each adds 2 points
        for (keyword in mediumRiskKeywords) {
            if (text.contains(keyword)) {
                score += 2
            }
        }

        // Extra checks
        // Contains numbers that look like OTP (4-8 digit number)
        if (text.matches(Regex(".*\\b\\d{4,8}\\b.*"))) score += 3

        // ALL CAPS words — urgency signal
        val capsWords = input.split(" ").count { it.length > 3 && it == it.uppercase() }
        if (capsWords >= 2) score += 3

        // Exclamation marks
        val exclamations = text.count { it == '!' }
        if (exclamations >= 1) score += 2

        // Short suspicious message
        if (text.length < 20 && score > 0) score += 2

        return when {
            score >= 4 -> "HIGH RISK"
            score >= 2 -> "MEDIUM RISK"
            else       -> "LOW RISK"
        }
    }
}