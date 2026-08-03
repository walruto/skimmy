package com.example.skimguard

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

private const val SUPABASE_URL = "https://dmdcpoakzemwcbikxcab.supabase.co"
private const val SUPABASE_ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRtZGNwb2FremVtd2NiaWt4Y2FiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU3NjgzMTIsImV4cCI6MjEwMTM0NDMxMn0.VC8SJ3ASAKbNiCcJxLWhai0OBLMsBuiR81hJCpiSjZU"

// Synthetic-email domain. Usernames never leave the UI as anything other than
// a username; this domain is appended only when calling Supabase Auth.
private const val USERNAME_EMAIL_DOMAIN = "skimguard.local"

val supabase = createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
    install(Auth)
}

// The ONE place username -> synthetic email conversion happens.
private fun toSyntheticEmail(username: String): String =
    "${username.lowercase()}@$USERNAME_EMAIL_DOMAIN"

fun validateUsername(username: String): String? = when {
    username.length < 3 -> "Username must be 3–20 characters"
    username.length > 20 -> "Username must be 3–20 characters"
    username.contains(' ') -> "Username cannot contain spaces"
    username.contains('@') -> "Username cannot contain @"
    else -> null
}

suspend fun authSignUp(username: String, password: String): String? = try {
    supabase.auth.signUpWith(Email) {
        email = toSyntheticEmail(username)
        this.password = password
    }
    // If the Supabase project has email confirmations off, sign-up already
    // yields a session. If not, we try sign-in to catch the auto-confirmed case;
    // if that also fails, surface a clear error.
    if (supabase.auth.currentSessionOrNull() == null) {
        supabase.auth.signInWith(Email) {
            email = toSyntheticEmail(username)
            this.password = password
        }
    }
    null
} catch (e: Exception) {
    e.message ?: "Sign up failed"
}

suspend fun authSignIn(username: String, password: String): String? = try {
    supabase.auth.signInWith(Email) {
        email = toSyntheticEmail(username)
        this.password = password
    }
    null
} catch (_: Exception) {
    "Invalid username or password"
}

suspend fun authSignOut() {
    supabase.auth.signOut()
}
