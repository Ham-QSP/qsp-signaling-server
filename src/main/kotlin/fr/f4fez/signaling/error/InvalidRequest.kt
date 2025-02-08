package fr.f4fez.signaling.error

import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
class InvalidRequest(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
