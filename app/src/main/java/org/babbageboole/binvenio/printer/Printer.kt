package org.babbageboole.binvenio.printer

import java.io.Closeable

interface Printer : Closeable {
    fun canConnect(): Boolean
    fun print(data: String): Boolean
}
