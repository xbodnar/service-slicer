package cz.bodnor.serviceslicer

import java.util.UUID

fun Int.toUUID(): UUID {
    assert(this >= 0)
    return createUuidFromNumber(this.toLong())
}

private fun createUuidFromNumber(number: Long): UUID {
    val uuidString = String.format("00000000-000-0000-0000-%012d", number)
    return UUID.fromString(uuidString.replaceRange(9, 12, "000"))
}
