package com.lightningkite.kiteui.views

/**
 * Thrown when a driver action fails. The [message] describes what went wrong.
 * Caught by the AI driver WebSocket handler and returned as an error response.
 */
public class DriverActionException(message: String) : Exception(message)
