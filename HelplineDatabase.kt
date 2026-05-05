package com.fraudx.app

object HelplineDatabase {

    fun getHelplineLabel(number: String): String? {
        val clean = number.replace(" ", "").replace("-", "").trim()
        return helplines[clean]
    }

    private val helplines = mapOf(
        "100"  to "🚔 Police Emergency",
        "101"  to "🚒 Fire Brigade",
        "102"  to "🚑 Ambulance",
        "108"  to "🚑 Emergency Ambulance",
        "112"  to "🆘 National Emergency",
        "1930" to "💰 Cyber Crime / Financial Fraud Helpline",
        "14440" to "💳 RBI Banking Ombudsman",
        "18001201740" to "🏦 NPCI UPI Helpline",
        "1800111109"  to "🏦 SBI Fraud Helpline",
        "18001804167" to "🏦 HDFC Fraud Helpline",
        "18002662667" to "🏦 ICICI Fraud Helpline",
        "18002100"    to "🏦 Axis Bank Helpline",
        "1800110420"  to "📱 TRAI Do Not Disturb",
        "198"  to "📡 BSNL Complaints",
        "1909" to "📵 DND Registration",
        "1076" to "🏛️ PM Helpline",
        "14566" to "👴 Senior Citizen Helpline",
        "181"  to "👩 Women Helpline",
        "1098" to "👶 Child Helpline",
        "155260" to "🛡️ Cyber Crime Helpline"
    )
}