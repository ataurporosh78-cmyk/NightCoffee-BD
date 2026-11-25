package com.example

import com.example.ui.screens.AuthInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputValidatorTest {

    @Test
    fun `isValidEmail validates standard email addresses and rejects malformed inputs`() {
        assertTrue(AuthInputValidator.isValidEmail("user@example.com"))
        assertTrue(AuthInputValidator.isValidEmail("  night.coffee+dhaka@gmail.com  "))
        assertFalse(AuthInputValidator.isValidEmail(""))
        assertFalse(AuthInputValidator.isValidEmail("   "))
        assertFalse(AuthInputValidator.isValidEmail("invalid-email"))
        assertFalse(AuthInputValidator.isValidEmail("user@domain"))
    }

    @Test
    fun `validateEmailAuth enforces sign-in and sign-up requirements cleanly`() {
        // Sign-In mode ignores fullName
        assertNull(
            AuthInputValidator.validateEmailAuth(
                isSignUpMode = false,
                fullName = "",
                email = "coffee@dhaka.bd",
                password = "secret"
            )
        )

        // Sign-Up mode requires at least 2 characters for name
        assertEquals(
            "Please enter your name (at least 2 characters).",
            AuthInputValidator.validateEmailAuth(
                isSignUpMode = true,
                fullName = "A",
                email = "coffee@dhaka.bd",
                password = "secret"
            )
        )

        // Short password rejected in both modes
        assertEquals(
            "Password must be at least 6 characters.",
            AuthInputValidator.validateEmailAuth(
                isSignUpMode = true,
                fullName = "Arif Hasan",
                email = "coffee@dhaka.bd",
                password = "123"
            )
        )

        // Valid sign-up passes
        assertNull(
            AuthInputValidator.validateEmailAuth(
                isSignUpMode = true,
                fullName = "Arif Hasan",
                email = "coffee@dhaka.bd",
                password = "password123"
            )
        )
    }

    @Test
    fun `validatePasswordResetEmail requires valid email first`() {
        assertEquals(
            "Enter your email address above first to reset your password.",
            AuthInputValidator.validatePasswordResetEmail("bad-email")
        )
        assertNull(AuthInputValidator.validatePasswordResetEmail("member@nightcoffee.bd"))
    }
}
