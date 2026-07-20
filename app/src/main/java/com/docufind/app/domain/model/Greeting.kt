package com.docufind.app.domain.model

import java.util.Calendar

enum class Greeting(val display: String) {
    MORNING("Good Morning"),
    AFTERNOON("Good Afternoon"),
    EVENING("Good Evening");

    companion object {
        fun current(): Greeting {
            return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> MORNING
                in 12..16 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}
