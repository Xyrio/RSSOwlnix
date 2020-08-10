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

package org.rssowl.core.internal.interpreter;

import org.jdom.Attribute;
import org.jdom.Element;
import org.rssowl.core.internal.persist.Attachment;
import org.rssowl.core.interpreter.INamespaceHandler;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;

import java.net.URI;

/**
 * Handler for the Media Namespace.
 * <p>
 * Namespace Prefix: media<br>
 * Namespace URI: http://search.yahoo.com/mrss/
 * </p>
 *
 * @author bpasero
 */
public class MediaNamespaceHandler implements INamespaceHandler {

  /*
   * @see
   * org.rssowl.core.interpreter.INamespaceHandler#processAttribute(org.jdom.
   * Attribute, org.rssowl.core.persist.IPersistable)
   */
  @Override
  public void processAttribute(Attribute attribute, IPersistable type) {}

  /*
   * @see org.rssowl.core.interpreter.INamespaceHandler#processElement(org.jdom.
   * Element , org.rssowl.core.persist.IPersistable)
   */
  @Override
  public void processElement(Element element, IPersistable type) {

    /* Contribution only valid for news */
    if (!(type instanceof INews))
      return;
    INews news = (INews) type;

    StringBuilder sbDesc = new StringBuilder();

    //examples:
    // https://www.youtube.com/feeds/videos.xml?channel_id=UC0vBXGSyV14uvJ4hECDOl0Q
    // https://www.youtube.com/feeds/videos.xml?user=Techquickie
    // https://peer.tube/feeds/videos.xml
    // (peertube, media:peerLink) https://video.blender.org/feeds/videos.xml
    switch (element.getName().toLowerCase()) {
      case "content"://$NON-NLS-1$
        processContentOrPeerLink(element, news);
        break;
      case "group": {//$NON-NLS-1$
        boolean isYoutube = news.getLinkAsText().contains(".youtube.com/"); //$NON-NLS-1$
        String thumbnail = null;
        String description = null;
        String ytRatingCount = null;
        String ytRatingAvg = null;
        String ytRatingMax = null;
        String ytViews = null;

        for (Object obj : element.getChildren()) {
          Element child = (Element) obj;
          switch (child.getName().toLowerCase()) {
            case "content"://$NON-NLS-1$
            case "peerlink"://$NON-NLS-1$
              processContentOrPeerLink(child, news);
              break;
            case "description"://$NON-NLS-1$
              description = child.getText();
              break;
            case "title"://$NON-NLS-1$
//              news.setTitle(child.getText()); //duplicate and probably refers to the content
              break;
            case "thumbnail"://$NON-NLS-1$
              thumbnail = child.getAttributeValue("url"); //$NON-NLS-1$
              break;
            case "community"://$NON-NLS-1$
              for (Object obj2 : child.getChildren()) {
                Element child2 = (Element) obj2;
                switch (child2.getName().toLowerCase()) {
                  case "starrating"://$NON-NLS-1$
                    ytRatingCount = child2.getAttributeValue("count");//$NON-NLS-1$
                    ytRatingAvg = child2.getAttributeValue("average");//$NON-NLS-1$
                    ytRatingMax = child2.getAttributeValue("max");//$NON-NLS-1$
                    break;
                  case "statistics"://$NON-NLS-1$
                    ytViews = child2.getAttributeValue("views");//$NON-NLS-1$
                    break;
                }
              }
              break;
          }
        }

        if (thumbnail != null) {
          //TODO hide image when video is shown? (at least for youtube)
          sbDesc.append("<img src=\"").append(thumbnail).append("\"><br>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
//          if (isYoutube) {
        //TODO embedding video is not so easy
//            sbDesc.append("<video><source src=\"").append(news.getLinkAsText()); //$NON-NLS-1$
//            sbDesc.append("\" type=\"video/mp4\">Your browser does not support the video HTML tag.</video><br><br>"); //$NON-NLS-1$

        //https://www.youtube.com/embed/videoId
//            String videoLink = news.getLinkAsText().replace("watch?v=", "embed/"); //$NON-NLS-1$ //$NON-NLS-2$
//            sbDesc.append("<iframe width=\"1024\" height=\"665\" src=\"" + videoLink + "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"); //$NON-NLS-1$ //$NON-NLS-2$
//          }
        if (ytRatingCount != null) {
          sbDesc.append("<table><tr>");//$NON-NLS-1$
          sbDesc.append("<td id=\"views\">").append(ytViews).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$
          sbDesc.append("<td id=\"rating_avg\">").append(ytRatingAvg).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$
          sbDesc.append("<td>/</td>"); //$NON-NLS-1$
          sbDesc.append("<td id=\"rating_max\">").append(ytRatingMax).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$
          sbDesc.append("<td>(</td>");//$NON-NLS-1$
          sbDesc.append("<td id=\"rating_count\">").append(ytRatingCount).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$
          sbDesc.append("<td>)</td>");//$NON-NLS-1$
          sbDesc.append("</tr></table><br><br>");//$NON-NLS-1$
        }
        if (description != null) {
          if (isYoutube) {
            description += " "; //$NON-NLS-1$
            //TODO: links made are destroyed by prefixing feeds url without page (http://www.youtube.com/feeds/)somewhere else
//              description = description.replaceAll("(https?:\\/\\/[^\\s]+)", "<a href=\"$1\">$1</a>"); //$NON-NLS-1$ //$NON-NLS-2$
            description = description.replaceAll("\r?\n\r?", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$
            sbDesc.append(description);
//              sbDesc.append("<pre>").append(description).append("</pre>"); //$NON-NLS-1$ //$NON-NLS-2$
          } else {
            sbDesc.append(description);
          }
        }
        if (sbDesc.length() > 0) {
          String descriptionOld = news.getDescription();
          if (descriptionOld != null && descriptionOld.length() > 0) {
            sbDesc.append(descriptionOld);
            sbDesc.append("<br>"); //$NON-NLS-1$
          }
          news.setDescription(sbDesc.toString());
        }
        break;
      }
    }
  }

  private void processContentOrPeerLink(Element element, INews news) {

    if (element.getAttributes().isEmpty())
      return;

    URI attachmentUri = null;
    String attachmentType = null;
    int attachmentLength = -1;

    for (Object obj : element.getAttributes()) {
      Attribute attribute = (Attribute) obj;
      switch (attribute.getName()) {
        case "fileSize"://$NON-NLS-1$
          attachmentLength = StringUtils.stringToInt(attribute.getValue());
          break;
        case "type"://$NON-NLS-1$
          attachmentType = attribute.getValue();
          break;
        case "url"://$NON-NLS-1$
        case "href"://$NON-NLS-1$
          attachmentUri = URIUtils.createURI(attribute.getValue());
          break;
      }
    }

    Attachment.createValidAttachment(news, attachmentUri, attachmentType, attachmentLength);
  }
}