package scalapb_json

import com.google.protobuf.TextFormat.ParseException
import com.google.protobuf.timestamp.Timestamp

object Timestamps {
  // Timestamp for "0001-01-01T00:00:00Z"
  val TIMESTAMP_SECONDS_MIN = -62135596800L
  val TIMESTAMP_SECONDS_MAX = 253402300799L
  val MICROS_PER_SECOND = 1000000
  val NANOS_PER_SECOND = 1000000000
  val NANOS_PER_MILLISECOND = 1000000
  val NANOS_PER_MICROSECOND = 1000

  private[this] val timestampFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def isValid(ts: Timestamp): Boolean =
    (ts.seconds >= TIMESTAMP_SECONDS_MIN &&
      ts.seconds <= TIMESTAMP_SECONDS_MAX &&
      ts.nanos >= 0 &&
      ts.nanos < NANOS_PER_SECOND)

  def checkValid(ts: Timestamp): Timestamp = {
    require(isValid(ts), "Timestamp is not valid.")
    ts
  }

  def formatNanos(nanos: Int): String =
    WellKnownTypes.formatNanos(nanos)

  def writeTimestamp(ts: Timestamp): String = {
    checkValid(ts)

    val result = new java.lang.StringBuilder
    val date = java.time.LocalDateTime.ofEpochSecond(ts.seconds, 0, java.time.ZoneOffset.UTC)
    result.append(timestampFormat.format(date))
    if (ts.nanos != 0) {
      result.append(".")
      result.append(formatNanos(ts.nanos))
    }
    result.append("Z")
    result.toString
  }

  def parseTimezoneOffset(s: String): Long =
    s(0) match {
      case 'Z' =>
        if (s.length != 1) {
          throw new ParseException(s"Failed to parse timestamp: invalid trailing data: '$s'")
        } else {
          0
        }
      case '+' | '-' =>
        val pos = s.indexOf(':')
        if (pos == -1) {
          throw new ParseException(s"Failed to parse timestamp: invalid offset value: '$s'")
        } else {
          val hours = s.substring(1, pos)
          val minutes = s.substring(pos + 1)
          val r = hours.toLong * 3600 + minutes.toLong * 60
          if (s(0) == '-') -r else r
        }
      case _ => throw new ParseException("Failed to parse timestamp.")
    }

  def parseTimestamp(value: String): Timestamp = {
    val dayOffset = value.indexOf('T')
    if (dayOffset == -1) {
      throw new ParseException("Failed to parse timestamp.")
    }
    val timezoneOffsetPosition = {
      val zIndex = value.indexOf('Z', dayOffset)
      if (zIndex != -1) zIndex
      else {
        val pIndex = value.indexOf('+', dayOffset)
        if (pIndex != -1) pIndex
        else {
          val mIndex = value.indexOf('-', dayOffset)
          if (mIndex != -1) mIndex
          else {
            throw new ParseException("Failed to parse timestamp: missing valid timezone offset.")
          }
        }
      }
    }
    val timezoneOffset = parseTimezoneOffset(value.substring(timezoneOffsetPosition))

    val timeValue = value.substring(0, timezoneOffsetPosition)
    val pointPosition = timeValue.indexOf('.')
    val (secondValue, nanoValue) =
      if (pointPosition == -1)
        (timeValue, "")
      else
        (timeValue.substring(0, pointPosition), timeValue.substring(pointPosition + 1))

    val date = java.time.LocalDateTime.parse(secondValue)
    val seconds: Long = date.toEpochSecond(java.time.ZoneOffset.UTC) - timezoneOffset
    val nanos: Int = if (nanoValue.isEmpty) 0 else Durations.parseNanos(nanoValue)
    normalizedTimestamp(seconds, nanos)
  }

  def normalizedTimestamp(seconds: Long, nanos: Int): Timestamp = {
    val (ns, nn) = if (nanos <= -NANOS_PER_SECOND || nanos >= NANOS_PER_SECOND) {
      (seconds + nanos / NANOS_PER_SECOND, nanos % NANOS_PER_SECOND)
    } else (seconds, nanos)

    val (ns2, nn2) =
      if (nn < 0) (seconds - 1, nanos + NANOS_PER_SECOND)
      else (ns, nn)

    checkValid(Timestamp(seconds = ns2, nanos = nn2))
  }
}
