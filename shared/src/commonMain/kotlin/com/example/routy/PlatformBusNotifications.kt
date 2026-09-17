package com.example.routy

/** Delivers an arrival alert on platforms that support local notifications. */
expect fun notifyBusApproaching(title: String, body: String)

/** Requests notification access at the moment the user enables bus tracking. */
expect fun requestBusNotificationPermission()
