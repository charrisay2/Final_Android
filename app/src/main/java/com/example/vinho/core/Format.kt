package com.example.vinho.core

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Các hàm định dạng dùng chung.
 *
 * Vì sao tách riêng: ViewModel và Composable cần cùng một cách hiển thị tiền/ngày.
 * Đặt ở đây thì unit test gọi được mà không cần Context.
 */
private val VI = Locale.forLanguageTag("vi-VN")

/** 1250000 -> "1.250.000 đ" */
fun formatMoney(amount: Long): String = "${formatNumber(amount)} đ"

/** 1250000 -> "1.250.000" (không kèm đơn vị) */
fun formatNumber(amount: Long): String =
    NumberFormat.getIntegerInstance(VI).format(amount)

/** 1234 -> "1.234" — dùng cho ô nhập tiền khi người dùng gõ */
fun formatInput(amount: Long): String = formatNumber(amount)

/** "1.250.000" -> 1250000; chuỗi rỗng/ký tự lạ -> null */
fun parseAmount(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    if (digits.isEmpty() || digits.length > 15) return null
    return digits.toLongOrNull()
}

/** 12,5% -> "12,5%" (1 chữ số thập phân, bỏ ".0") */
fun formatPercent(value: Float): String {
    val rounded = Math.round(value * 10) / 10f
    val text = if (rounded % 1f == 0f) rounded.toInt().toString()
    else String.format(Locale.US, "%.1f", rounded).replace('.', ',')
    return "$text%"
}

/** "2026-11" — khoá tháng lưu trên Firestore, nhờ đó query chỉ cần whereEqualTo */
fun YearMonth.monthKey(): String = format(DateTimeFormatter.ofPattern("yyyy-MM"))

/** YearMonth(2026, 11) -> "11/2026" */
fun YearMonth.monthLabel(): String = "$monthValue/$year"

fun YearMonth.previous(): YearMonth = minusMonths(1)

fun YearMonth.next(): YearMonth = plusMonths(1)

fun LocalDate.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** "06/11/2026" */
fun formatDate(epochMillis: Long): String =
    epochMillis.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

/** "06/11" — nhãn ngắn cho biểu đồ */
fun formatDayMonth(epochMillis: Long): String =
    epochMillis.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"))
