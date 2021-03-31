/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2011 RSSOwl Development Team                                  **
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

import org.rssowl.core.connection.HttpConnectionInputStream;
import org.rssowl.core.util.CoreUtils;

import java.io.InputStream;

/**
 * Utility class for connection package.
 */
public class ConnectionUtils {

  /**
   * Instantiates a new {@code ConnectionUtils}.
   */
  private ConnectionUtils() {

  }

  /**
   * If the stream contains JOSON content and returns true if so false
   * otherwise.
   *
   * @param inStream the input stream
   * @return {@code true}, if stream has json content, {@code false} otherwise
   */
  public static boolean hasJsonContent(final InputStream inStream) {
    return inStream instanceof HttpConnectionInputStream && ((HttpConnectionInputStream) inStream).getContentType().toLowerCase().equals(CoreUtils.APPLICATION_JSON);
  }

}
