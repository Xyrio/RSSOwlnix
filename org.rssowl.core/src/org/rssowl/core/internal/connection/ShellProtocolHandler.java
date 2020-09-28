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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.util.Triple;
import org.rssowl.core.util.URIUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class ShellProtocolHandler extends DefaultProtocolHandler {

  @Override
  public Triple<IFeed, IConditionalGet, URI> reload(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws CoreException {
    String shellCmd = getShellCmd(link);
    SupplierConnectionException<InputStream> inSS = () -> openStreamOfShellResult(shellCmd);
    return super.reload(inSS, link, monitor, properties);
  }

  @Override
  public byte[] getFeedIcon(URI link, IProgressMonitor monitor) {
      return super.getFeedIcon(link, monitor);
  }

  @Override
  public String getLabel(URI link, IProgressMonitor monitor) throws ConnectionException {
    String shellCmd = getShellCmd(link);
    InputStream inS = openStreamOfShellResult(shellCmd);
    return super.getLabel(inS, monitor);
  }

  @Override
  public URI getFeed(URI website, IProgressMonitor monitor) throws ConnectionException {
    String shellCmd = getShellCmd(website);
    InputStream inS = openStreamOfShellResult(shellCmd);
    return super.getFeed(inS, website, monitor);
  }

  private InputStream openStreamOfShellResult(String shellCmd) throws ConnectionException {
    try {
      ProcessBuilder pb = new ProcessBuilder(shellCmd.split("\\s")); //$NON-NLS-1$
      Process p = pb.start();
      return p.getInputStream();
    } catch (IOException e) {
      e.printStackTrace();
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  private String getShellCmd(URI link) {
    //shell://shellcmd
    String str = link.toString().substring(8); //8="shell://".length()
    String linkIcon = null;
    String shellCmd = null;
    if (str.startsWith(";")) { //$NON-NLS-1$
      linkIcon = str.split(";")[1]; //$NON-NLS-1$
      shellCmd = str.substring(2 + linkIcon.length());
    } else {
      shellCmd = str;
    }
    if (shellCmd != null)
      shellCmd = URIUtils.fastDecode(shellCmd);
    return shellCmd;
  }
}
