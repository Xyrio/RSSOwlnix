/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.core.internal.connection;


import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * trust everything
 */
public class EasySSLConnectionSocketFactory implements LayeredConnectionSocketFactory {

  private SSLConnectionSocketFactory fFactory;

  public EasySSLConnectionSocketFactory() {
    javax.net.ssl.SSLContext sslContext = null; //should never end up as null
    try {
      sslContext = new SSLContextBuilder() //
          // .loadTrustMaterial(null, new TrustSelfSignedStrategy()) //
//          .setTrustManagerFactoryAlgorithm("PKIX") //$NON-NLS-1$
          .loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
              return true; //trust everything
            }
          }) //
          .build();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }

    if (sslContext != null)
      fFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

  }

  @Override
  public Socket createSocket(HttpContext context) throws IOException {
    return fFactory.createSocket(context);
  }

  @Override
  public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
    return fFactory.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
  }

  @Override
  public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException {
    return fFactory.createLayeredSocket(socket, target, port, context);
  }

}