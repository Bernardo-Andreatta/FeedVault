package com.example.securegallery.util

import java.text.Normalizer

/**
 * Lowercases and strips diacritics (accents) so search ignores them:
 * "maçã" and "maca" both normalize to "maca".
 */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
