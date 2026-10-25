package app.havenos.launcher.data

import java.util.Locale
import kotlin.math.pow

/**
 * Safe calculator for drawer search: recursive-descent parser over
 * numbers, + - * / % ^, parentheses and unary minus. No functions.
 * Returns null on any parse failure, or when the input contains no binary
 * operator (a bare number is not a calculation).
 */
fun evaluate(expr: String): Double? {
    val parser = CalcParser(expr)
    return try {
        val value = parser.parse()
        if (!parser.hasOperator) null
        else if (value.isNaN() || value.isInfinite()) null
        else value
    } catch (t: Throwable) {
        null
    }
}

/** Format a result without a trailing ".0", capped at 10 significant digits. */
fun formatCalcResult(v: Double): String {
    if (v == v.toLong().toDouble() && Math.abs(v) < 1e15) return v.toLong().toString()
    var s = String.format(Locale.US, "%.10g", v)
    if ('.' in s && 'e' !in s && 'E' !in s) {
        s = s.trimEnd('0').trimEnd('.')
    }
    return s
}

private class CalcParser(private val input: String) {
    private var pos = 0
    var hasOperator = false
        private set

    fun parse(): Double {
        val v = parseExpr()
        skipSpaces()
        if (pos != input.length) throw IllegalArgumentException("trailing input")
        return v
    }

    private fun skipSpaces() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char? {
        skipSpaces()
        return if (pos < input.length) input[pos] else null
    }

    private fun parseExpr(): Double {
        var v = parseTerm()
        while (true) {
            when (peek()) {
                '+' -> { pos++; hasOperator = true; v += parseTerm() }
                '-' -> { pos++; hasOperator = true; v -= parseTerm() }
                else -> return v
            }
        }
    }

    private fun parseTerm(): Double {
        var v = parseFactor()
        while (true) {
            when (peek()) {
                '*' -> { pos++; hasOperator = true; v *= parseFactor() }
                '/' -> {
                    pos++; hasOperator = true
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("div by zero")
                    v /= d
                }
                '%' -> {
                    pos++; hasOperator = true
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("mod by zero")
                    v %= d
                }
                else -> return v
            }
        }
    }

    private fun parseFactor(): Double {
        val base = parseUnary()
        if (peek() == '^') {
            pos++
            hasOperator = true
            val exp = parseFactor() // right-associative
            return base.pow(exp)
        }
        return base
    }

    private fun parseUnary(): Double {
        if (peek() == '-') {
            pos++
            return -parseUnary()
        }
        if (peek() == '+') {
            pos++
            return parseUnary()
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Double {
        val c = peek() ?: throw IllegalArgumentException("unexpected end")
        if (c == '(') {
            pos++
            val v = parseExpr()
            if (peek() != ')') throw IllegalArgumentException("missing )")
            pos++
            return v
        }
        if (c.isDigit() || c == '.') {
            val start = pos
            var seenDot = false
            var seenDigit = false
            while (pos < input.length) {
                val ch = input[pos]
                if (ch.isDigit()) {
                    seenDigit = true; pos++
                } else if (ch == '.' && !seenDot) {
                    seenDot = true; pos++
                } else {
                    break
                }
            }
            if (!seenDigit) throw IllegalArgumentException("bad number")
            return input.substring(start, pos).toDouble()
        }
        throw IllegalArgumentException("unexpected '$c'")
    }
}
