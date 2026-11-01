package io.alwayszihan.zeroxpv.ui.browser.networkstreaming.clients

import io.alwayszihan.zeroxpv.domain.network.NetworkConnection
import io.alwayszihan.zeroxpv.domain.network.NetworkProtocol

object NetworkClientFactory {
  fun createClient(connection: NetworkConnection): NetworkClient =
    when (connection.protocol) {
      NetworkProtocol.SMB -> SmbClient(connection)
      NetworkProtocol.FTP -> FtpClient(connection)
      NetworkProtocol.WEBDAV -> WebDavClient(connection)
    }
}

