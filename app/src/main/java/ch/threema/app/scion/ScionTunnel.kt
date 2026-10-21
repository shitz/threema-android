package ch.threema.app.scion

import android.content.Context
import ch.threema.app.BuildConfig
import ch.threema.base.utils.getThreemaLogger
import com.anapaya.scion.http3.AuthTokenSource
import com.anapaya.scion.http3.ScionHttp3Client
import com.anapaya.scion.http3.ScionTunnelSocket
import com.anapaya.scion.http3.TrustAnchors
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.Socket
import java.net.SocketAddress
import java.net.URI
import javax.net.SocketFactory
import okhttp3.Dns

private val logger = getThreemaLogger("ScionTunnel")

/**
 * Sends the TCP connections of the app through a CONNECT tunnel over SCION.
 *
 * The tunnel is on when the user entered a SCION API key. The SCION client exchanges the key at
 * the Anapaya AA and gets its connectivity from the endhost API in
 * [BuildConfig.SCION_ENDHOST_API_URL]. The gateway listens on [GATEWAY_PORT] for all targets, so
 * each socket dials that port and sends its own port in the CONNECT request.
 */
object ScionTunnel {
    private const val PREFERENCES_NAME = "scion_tunnel"
    private const val PREFERENCE_API_KEY = "api_key"
    private const val GATEWAY_PORT = 443

    /**
     * The echo request interval of the chat server connection, in seconds. The SCION client closes
     * a QUIC connection after 30 seconds without traffic and sends no keepalives, so the default
     * interval of 60 seconds lets the tunnel drop between two echo requests.
     */
    const val ECHO_REQUEST_INTERVAL: Short = 20

    private lateinit var context: Context

    @Volatile
    private var apiKey: String? = null

    private var client: ScionHttp3Client? = null

    /** Reads the stored API key. Call it before anything opens a connection. */
    fun init(context: Context) {
        this.context = context.applicationContext
        apiKey = preferences().getString(PREFERENCE_API_KEY, null)
        logger.info("SCION tunnel is {}", if (isEnabled) "on" else "off")
    }

    /** True if the connections go through the tunnel. */
    val isEnabled: Boolean
        get() = apiKey != null

    /** Stores [key] and turns the tunnel on, or turns it off if [key] is blank. */
    fun setApiKey(key: String?) {
        val value = key?.trim()?.takeIf { it.isNotEmpty() }
        preferences().edit().putString(PREFERENCE_API_KEY, value).apply()
        synchronized(this) {
            apiKey = value
            client?.close()
            client = null
        }
        logger.info("SCION tunnel is {}", if (isEnabled) "on" else "off")
    }

    /** An unconnected socket: a tunnel socket if the tunnel is on, else a platform socket. */
    fun newSocket(): Socket = if (isEnabled) ScionTunnelSocket(client(), GATEWAY_PORT) else Socket()

    /** Creates the sockets of [newSocket], for OkHttp. Use it together with [dns]. */
    val socketFactory: SocketFactory = object : SocketFactory() {
        override fun createSocket(): Socket = newSocket()

        override fun createSocket(host: String, port: Int): Socket =
            connected(
                if (isEnabled) InetSocketAddress.createUnresolved(host, port) else InetSocketAddress(host, port),
            )

        override fun createSocket(host: String, port: Int, localHost: InetAddress?, localPort: Int): Socket =
            createSocket(host, port)

        override fun createSocket(host: InetAddress, port: Int): Socket = connected(InetSocketAddress(host, port))

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
            createSocket(address, port)

        private fun connected(endpoint: InetSocketAddress): Socket {
            val socket = newSocket()
            try {
                socket.connect(endpoint)
            } catch (e: IOException) {
                socket.close()
                throw e
            }
            return socket
        }
    }

    /**
     * Resolves no host while the tunnel is on. The placeholder address keeps the host name, and the
     * tunnel socket sends the name to the gateway, which resolves it.
     */
    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            if (isEnabled) {
                listOf(InetAddress.getByAddress(hostname, byteArrayOf(0, 0, 0, 0)))
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
    }

    /** Selects no proxy while the tunnel is on, and the system proxy if it is off. */
    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> =
            if (isEnabled) listOf(Proxy.NO_PROXY) else getDefault()?.select(uri) ?: listOf(Proxy.NO_PROXY)

        override fun connectFailed(uri: URI?, address: SocketAddress?, e: IOException?) {
            if (!isEnabled) getDefault()?.connectFailed(uri, address, e)
        }
    }

    private fun client(): ScionHttp3Client = synchronized(this) {
        client ?: build(apiKey ?: throw IOException("no SCION API key is set")).also { client = it }
    }

    private fun build(key: String): ScionHttp3Client {
        logger.info(
            "Building the SCION client, AA {}, endhost API {}",
            BuildConfig.SCION_AA_URL,
            BuildConfig.SCION_ENDHOST_API_URL,
        )
        return ScionHttp3Client.Builder(context)
            .endhostApi(BuildConfig.SCION_ENDHOST_API_URL)
            .authTokenSource(AuthTokenSource.anapayaAa(key, BuildConfig.SCION_AA_URL))
            // The gateway presents a self-signed certificate. The inner TLS to the Threema
            // server checks the server.
            .trust(TrustAnchors.insecureNoVerify())
            .build()
    }

    private fun preferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
