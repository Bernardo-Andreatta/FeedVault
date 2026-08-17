package com.bernardo.feedvault.util

import java.text.Normalizer

/**
 * Lowercases and strips diacritics (accents) so search ignores them:
 * "café" and "cafe" both normalize to "cafe".
 */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
