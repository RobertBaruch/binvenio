package org.babbageboole.binvenio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class SearchPrinterViewModel(application: Application) : AndroidViewModel(application) {
    private var _addr = MutableLiveData<InetSocketAddress?>(null)
    val addr: LiveData<InetSocketAddress?>
        get() = _addr
    val addrStr = Transformations.map(_addr) {
        if (it != null) it.address.hostAddress
        else ""
    }
    private var _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int>
        get() = _progress

    private var _success = MutableLiveData<Boolean?>(null)
    val success: LiveData<Boolean?>
        get() = _success

    /**
     * viewModelJob allows us to cancel all coroutines started by this ViewModel.
     */
    private var viewModelJob = Job()

    /**
     * A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
     *
     * Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
     * by calling `viewModelJob.cancel()`
     *
     * By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
     * the main thread on Android. This is a sensible default because most coroutines started by
     * a [ViewModel] update the UI after performing some processing.
     */
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    /**
     * Called when the ViewModel is dismantled.
     * At this point, we want to cancel all coroutines;
     * otherwise we end up with processes that have nowhere to return to
     * using memory and resources.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun onSearch() {
        uiScope.launch {
            val bs = ByteArray(4)
            bs[0] = 192.toByte()
            bs[1] = 168.toByte()
            bs[2] = 1
            for (b in 1.rangeTo(255)) {
                bs[3] = b.toByte()
                val addr = InetSocketAddress(InetAddress.getByAddress(bs), 6101)
                _addr.value = addr
                if (search(addr)) {
                    _success.value = true
                    return@launch
                }
                _progress.value = (100 * b) / 255
            }
            _success.value = false
        }
    }

    private suspend fun search(addr: InetSocketAddress): Boolean {
        return withContext(Dispatchers.IO) {
            connect(addr)?.use { socket ->
                return@withContext checkHostInformation(socket) != null
            }
            return@withContext false
        }
    }

    private fun connect(addr: InetSocketAddress): Socket? {
        return try {
            val socket = Socket()
            socket.connect(addr, 200 /* milliseconds */)
            Timber.i("Connected to ${addr}")
            socket.soTimeout = 100
            socket
        } catch (ex: Exception) {
            Timber.i("Failed to connect to ${addr}")
            null
        }
    }

    private fun checkHostInformation(socket: Socket): String? {
        val stringBuilder = StringBuilder()

        return try {
            socket.getInputStream().use { rsp ->
                socket.getOutputStream().write("~HI\n".toByteArray())
                var c = rsp.read()
                if (c != 2) {
                    Timber.i("Expected 2 (STX) but got $c")
                    return null
                }
                while (c != 3 /* ETX */) {
                    stringBuilder.append(c.toChar())
                    if (rsp.available() <= 0) {
                        Timber.i("Failed to end with ETX")
                        break
                    }
                    c = rsp.read()
                }
                stringBuilder.deleteCharAt(0).toString()
            }
        } catch (ex: Exception) {
            Timber.i("Failed to read: $ex")
            null
        }
    }
}