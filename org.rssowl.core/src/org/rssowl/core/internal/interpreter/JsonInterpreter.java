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

package org.rssowl.core.internal.interpreter;

import org.jdom.Document;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.interpreter.json.JSONArray;
import org.rssowl.core.internal.interpreter.json.JSONException;
import org.rssowl.core.internal.interpreter.json.JSONObject;
import org.rssowl.core.interpreter.IFormatInterpreter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author r.roykrishna
 */
public class JsonInterpreter implements IFormatInterpreter {

  /* Constants used to obtain data from the JSON Objects */
  // refer to https://jsonfeed.org/version/1.1

  // Top-level attributes
  private static final String TITLE = "title"; //$NON-NLS-1$
  private static final String VERSION = "version"; //$NON-NLS-1$
  private static final String HOME_PAGE_URL = "home_page_url"; //$NON-NLS-1$
  private static final String FEED_URL = "feed_url"; //$NON-NLS-1$
  private static final String DESCRIPTION = "description"; //$NON-NLS-1$
  private static final String USER_COMMENT = "user_comment"; //$NON-NLS-1$
  private static final String NEXT_URL = "next_url"; //$NON-NLS-1$
  private static final String ICON = "icon"; //$NON-NLS-1$
  private static final String FAV_ICON = "favicon"; //$NON-NLS-1$
  private static final String AUTHOR = "author"; //$NON-NLS-1$
  private static final String AUTHORS = "authors"; //$NON-NLS-1$
  private static final String AUTHOR_NAME = "name"; //$NON-NLS-1$
  private static final String AUTHOR_URL = "url"; //$NON-NLS-1$
  private static final String AUTHOR_AVATAR = "avatar"; //$NON-NLS-1$
  private static final String LANGUAGE = "language"; //$NON-NLS-1$
  private static final String EXPIRED = "expired"; //$NON-NLS-1$
  private static final String HUB = "hub"; //$NON-NLS-1$
  private static final String ITEMS = "items"; //$NON-NLS-1$

  // Item Attributes
  private static final String ITEM_ID = "id"; //$NON-NLS-1$
  private static final String ITEM_URL = "url"; //$NON-NLS-1$
  private static final String ITEM_EXTERNAL_URL = "external_url"; //$NON-NLS-1$
  private static final String ITEM_TITLE = TITLE;
  private static final String ITEM_CONTENT_TEXT = "content_text"; //$NON-NLS-1$
  private static final String ITEM_CONTENT_HTML = "content_html"; //$NON-NLS-1$
  private static final String ITEM_SUMMARY = "summary"; //$NON-NLS-1$
  private static final String ITEM_IMAGE = "image"; //$NON-NLS-1$
  private static final String ITEM_BANNER_IMAGE = "banner_image"; //$NON-NLS-1$
  private static final String ITEM_DATE_PUBLISHED = "date_published"; //$NON-NLS-1$
  private static final String ITEM_DATE_MODIFIED = "date_modified"; //$NON-NLS-1$
  private static final String ITEM_AUTHORS = AUTHORS;
  private static final String ITEM_TAGS = "tags"; //$NON-NLS-1$
  private static final String ITEM_LANGUAGE = "language"; //$NON-NLS-1$
  private static final String ITEM_ATTACHEMENTS = "attachements"; //$NON-NLS-1$

  // Attachment Atrributes.
  private static final String ATTACHEMENT_URL = "url"; //$NON-NLS-1$
  private static final String ATTACHEMENT_MIME_TYPE = "mime_type"; //$NON-NLS-1$
  private static final String ATTACHEMENT_TITLE = TITLE;
  private static final String ATTACHEMENT_SIZE_IN_BYTES = "size_in_bytes"; //$NON-NLS-1$
  private static final String ATTACHEMENT_DURATION_IN_SECONDS = "duration_in_seconds"; //$NON-NLS-1$

  /*
   * @see
   * org.rssowl.core.interpreter.IFormatInterpreter#interpret(org.jdom.Document,
   * org.rssowl.core.persist.IFeed)
   */
  @Override
  public void interpret(Document document, IFeed feed) throws InterpreterException {
    throw new UnsupportedOperationException();
  }

  public void interpret(JSONObject jsonFeedObject, IFeed feed) throws InterpreterException {
    try {
      processFeed(jsonFeedObject, feed);
    } catch (JSONException | URISyntaxException e) {
      throw new InterpreterException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  private void processFeed(JSONObject json, IFeed feed) throws JSONException, URISyntaxException {

    feed.setFormat(CoreUtils.JSON);
    
    /* Title */
    feed.setTitle(getString(json, TITLE));

    /* Description */
    feed.setDescription(getString(json, DESCRIPTION));

    // language
    feed.setLanguage(getString(json, LANGUAGE));

    /* News Items */
    if (json.has(ITEMS)) {
      JSONArray items = json.getJSONArray(ITEMS);
      for (int i = 0; i < items.length(); i++) {
        JSONObject item = items.getJSONObject(i);
        processItem(item, feed);
      }
    }
  }

  private void processItem(JSONObject item, IFeed feed) throws JSONException, URISyntaxException {
    IModelFactory factory = Owl.getModelFactory();
    INews news = factory.createNews(null, feed, new Date());
    news.setBase(feed.getBase());

    /* GUID */
    if (item.has(ITEM_ID))
      news.setGuid(factory.createGuid(news, item.getString(ITEM_ID), true));

    /* Title */
    news.setTitle(getString(item, TITLE));

    /* Publish Date */
    news.setPublishDate(getDate(item, ITEM_DATE_PUBLISHED));

    /* Description */
    news.setDescription(getContent(item));

    /* Link */
    news.setLink(getURI(item, ITEM_URL));

    /* Author */
    if (item.has(AUTHOR)) {
      String author = getString(item, AUTHOR);
      if (StringUtils.isSet(author)) {
        IPerson person = factory.createPerson(null, news);
        person.setName(author);
      }
    }

    /* Attachments */
    if (item.has(ITEM_ATTACHEMENTS)) {
      JSONArray attachments = item.getJSONArray(ITEM_ATTACHEMENTS);
      for (int i = 0; i < attachments.length(); i++) {
        JSONObject attachment = attachments.getJSONObject(i);
        if (attachment.has(ATTACHEMENT_URL)) {
          IAttachment att = factory.createAttachment(null, news);
          att.setLink(new URI(attachment.getString(ATTACHEMENT_URL)));

          if (attachment.has(ATTACHEMENT_SIZE_IN_BYTES)) {
            try {
              att.setLength(attachment.getInt(ATTACHEMENT_SIZE_IN_BYTES));
            } catch (JSONException e) {
              // Can happen if the length is larger than Integer.MAX_VALUE, in that case just ignore
            }
          }

          if (attachment.has(ATTACHEMENT_MIME_TYPE))
            att.setType(attachment.getString(ATTACHEMENT_MIME_TYPE));
        }
      }
    }

    /* Categories / Labels / State */
    Set<String> labels = new HashSet<String>(1);
    if (item.has(ITEM_TAGS)) {
      JSONArray categories = item.getJSONArray(ITEM_TAGS);
      for (int i = 0; i < categories.length(); i++) {
        if (categories.isNull(i))
          continue;

        String category = categories.getString(i);
        if (!StringUtils.isSet(category))
          continue;

      }

      /*
       * Store Labels as Properties first and create them in ApplicationService
       * with a single Thread to avoid that Labels are created as duplicates.
       */
      if (!labels.isEmpty())
        news.setProperty(ITEM_TAGS, labels.toArray(new String[labels.size()]));
    }
  }

  private String getString(JSONObject object, String key) throws JSONException {
    return object.has(key) ? object.getString(key) : null;
  }

  private String getContent(JSONObject item) throws JSONException {
    if (item != null) {
      if (item.has(ITEM_CONTENT_HTML)) {
        return item.getString(ITEM_CONTENT_HTML);
      }
      if (item.has(ITEM_CONTENT_TEXT)) {
        return item.getString(ITEM_CONTENT_TEXT);
      }
    }
    return null;
  }

  private URI getURI(JSONObject object, String key) throws URISyntaxException, JSONException {
    return object.has(key) ? new URI(object.getString(key)) : null;
  }

  private Date getDate(JSONObject object, String key) throws JSONException {
    return object.has(key) ? parseDateTime(getString(object, key)) : null;
  }

  private Date parseDateTime(String dateString) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(dateString); //$NON-NLS-1$
    } catch (ParseException e) {
      return new Date();
    }
  }

}
