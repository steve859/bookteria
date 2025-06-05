package com.soft.bookteria.helpers

enum class Error(val code: Int, val message: String) {
    NETWORK_ERROR(1001, "Network connection failed"),
    SERVER_ERROR(1002, "Server returned an error"),
    TIMEOUT_ERROR(1003, "Request timed out"),
    AUTHENTICATION_ERROR(1004, "Authentication failed"),
    UNKNOWN_ERROR(9999, "Unknown error occurred");
    
    override fun toString(): String {
        return "Error(code=$code, message='$message')"
    }
}