package ch.threema.domain.protocol.connection.csp

import ch.threema.domain.protocol.ServerAddressProvider
import ch.threema.domain.protocol.Version
import ch.threema.domain.protocol.connection.BaseServerConnection
import ch.threema.domain.protocol.connection.BaseServerConnectionConfiguration
import ch.threema.domain.protocol.connection.ConnectionState
import ch.threema.domain.protocol.connection.ServerConnectionDependencyProvider
import ch.threema.domain.protocol.connection.csp.socket.CspSocket
import ch.threema.domain.protocol.connection.csp.socket.HostResolver
import ch.threema.domain.protocol.connection.csp.socket.SocketFactory
import ch.threema.domain.protocol.csp.ProtocolDefines
import ch.threema.domain.stores.IdentityStore
import ch.threema.domain.taskmanager.IncomingMessageProcessor
import ch.threema.domain.taskmanager.TaskManager

/**
 * Only this interface is exposed to other modules
 */
interface CspConnection

internal class CspConnectionImpl(
    dependencyProvider: ServerConnectionDependencyProvider,
    awaitAppReady: suspend () -> Unit,
) : CspConnection, BaseServerConnection(dependencyProvider, awaitAppReady) {
    override fun onConnected() {
        socket.let {
            if (it is CspSocket) {
                it.setSocketSoTimeout(ProtocolDefines.READ_TIMEOUT * 1000)
            }
        }
    }

    override fun onCspAuthenticated() {
        socket.let {
            if (it is CspSocket) {
                it.setSocketSoTimeout(0)
            }
        }
    }

    override fun onException(t: Throwable) {
        if (connectionState != ConnectionState.LOGGED_IN) {
            socket.let {
                if (it is CspSocket) {
                    it.advanceAddress()
                }
            }
        }
    }
}

data class CspConnectionConfiguration(
    override val identityStore: IdentityStore,
    override val serverAddressProvider: ServerAddressProvider,
    override val version: Version,
    override val assertDispatcherContext: Boolean,
    override val deviceCookieManager: DeviceCookieManager,
    override val incomingMessageProcessor: IncomingMessageProcessor,
    override val taskManager: TaskManager,
    val hostResolver: HostResolver,
    val socketFactory: SocketFactory,
    /**
     * Whether the chat server host is resolved on the device. If false, the [socketFactory] gets
     * unresolved addresses, for example for a tunnel that resolves the host at its other end.
     */
    val resolveHostLocally: Boolean = true,
    /**
     * The interval of the echo requests, in seconds. A transport that closes idle connections
     * sooner than [ProtocolDefines.ECHO_REQUEST_INTERVAL_CSP], for example a tunnel, needs a
     * shorter interval.
     */
    val echoRequestInterval: Short = ProtocolDefines.ECHO_REQUEST_INTERVAL_CSP,
) : BaseServerConnectionConfiguration
