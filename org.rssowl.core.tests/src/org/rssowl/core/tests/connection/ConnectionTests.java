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

package org.rssowl.core.tests.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.HttpConnectionInputStream;
import org.rssowl.core.connection.IConditionalGetCompatible;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IConnectionService;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.connection.IProxyCredentials;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.connection.PlatformCredentialsProvider;
import org.rssowl.core.internal.connection.DefaultProtocolHandler;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.service.PersistenceServiceImpl;
import org.rssowl.core.interpreter.EncodingException;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.dao.OwlDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.tests.TestWebServer;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.core.util.Triple;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.LinkTransformer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This TestCase covers use-cases for the Connection Plugin.
 *
 * @author bpasero
 */
public class ConnectionTests {

  @BeforeClass
  public static void setUpOnce() {
    TestWebServer.start(true);
  }

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    ((PersistenceServiceImpl) Owl.getPersistenceService()).recreateSchemaForTests();
  }

  /**
   * Test contribution of Credentials Provider.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testProxyCredentialProvider() throws Exception {
    try {
      MyCredentialsProvider.isEnabled = true;

      IConnectionService conManager = Owl.getConnectionService();
      URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
      IFeed feed = new Feed(feedUrl);

      IProxyCredentials proxyCredentials = conManager.getProxyCredentials(feed.getLink());

      assertEquals("", proxyCredentials.getDomain());
      assertEquals("proxy-" + TestWebServer.username, proxyCredentials.getUsername());
      assertEquals("proxy-" + TestWebServer.password, proxyCredentials.getPassword());
      assertEquals("127.0.0.1", proxyCredentials.getHost());
      assertEquals(0, proxyCredentials.getPort());
    } finally {
      MyCredentialsProvider.isEnabled = false;
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testGetLabel() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
    String label = conManager.getLabel(feedUrl, new NullProgressMonitor());
    assertEquals("Some Feed Title", label);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testGetFavicon() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
    byte[] feedIcon = conManager.getFeedIcon(feedUrl, new NullProgressMonitor());
    assertNotNull("no favicon.ico found", feedIcon);
    assertTrue("favicon.ico length is 0", feedIcon.length != 0);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testHttpConnectionInputStream() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI url = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
    IProtocolHandler handler = conManager.getHandler(url);
    InputStream stream = handler.openStream(url, null, null);
    if (stream instanceof HttpConnectionInputStream) {
      HttpConnectionInputStream inS = (HttpConnectionInputStream) stream;
      assertTrue(inS.getContentLength() > 0);
      assertNotNull(inS.getIfModifiedSince());
      assertNotNull(inS.getIfNoneMatch());
    }
  }

  @Test
  public void testProtectedFeedHTTP() throws Exception {
    do_testProtectedFeed(false, false);
  }

  @Test
  public void testProtectedFeedInMemoryHTTP() throws Exception {
    do_testProtectedFeed(true, false);
  }

  @Test
  public void testProtectedFeedHTTPS() throws Exception {
    do_testProtectedFeed(false, true);
  }

  @Test
  public void testProtectedFeedInMemoryHTTPS() throws Exception {
    do_testProtectedFeed(true, true);
  }

  private void do_testProtectedFeed(boolean isInMemory, boolean isHttps) throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    String root = isHttps ? TestWebServer.rootHttps : TestWebServer.rootHttp;
//    URI feedUrl1 = new URI("http://httpbin.org/basic-auth/super/1234");
//    URI feedUrl1 = new URI("https://httpbin.org/basic-auth/super/1234");
    URI feedUrl1 = new URI(root + "/auth-feed/some_feed.xml");
    URI feedUrl2 = new URI(root + "/auth-feed/some_feed2.xml");

    ICredentials credentials = new ICredentials() {
      @Override
      public String getDomain() {
        return null;
      }

      @Override
      public String getPassword() {
        return TestWebServer.password;
      }

      @Override
      public String getUsername() {
        return TestWebServer.username;
      }
    };

    try {
      {
        IFeed feed1 = new Feed(feedUrl1);
        OwlDAO.save(feed1);

        do_testProtectedFeed_MissingCredentials(feed1);
        do_testProtectedFeed_WithAuth(isInMemory, feed1, credentials, null, true); //no realm set will accept all realms
        do_testProtectedFeed_WithAuth(isInMemory, feed1, credentials, "", true); //no realm set will accept all realms

        OwlDAO.delete(feed1);
        assertNull(conManager.getAuthCredentials(feed1.getLink(), null));
      }
      {
        IFeed feed2 = new Feed(feedUrl2);
        OwlDAO.save(feed2);

        //ones that expect exception first because success is remembered
        do_testProtectedFeed_WithAuth(isInMemory, feed2, credentials, "Other Directory", false); //wrong realm
        do_testProtectedFeed_WithAuth(isInMemory, feed2, credentials, "BASIC Restricted Directory", true); //correct realm

        OwlDAO.delete(feed2);
        assertNull(conManager.getAuthCredentials(feed2.getLink(), null));
      }
    } finally {
      ((PlatformCredentialsProvider) conManager.getCredentialsProvider(feedUrl2)).clear();
    }
  }

  private void do_testProtectedFeed_MissingCredentials(IFeed feed) throws Exception {
    URI link = feed.getLink();

    AuthenticationRequiredException e = null;
    try {
      Owl.getConnectionService().getHandler(link).openStream(link, null, null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }
    assertNotNull(e);
  }

  private void do_testProtectedFeed_WithAuth(boolean isInMemory, IFeed feed, ICredentials credentials, String authRealm, boolean expectsRssElseException) throws Exception {
    URI rssFileLink = feed.getLink();
    IConnectionService conManager = Owl.getConnectionService();
    ICredentialsProvider credProvider = conManager.getCredentialsProvider(rssFileLink);

    /* Test authentication by realm is working */
    if (authRealm == null || authRealm.length() == 0) {
      if (isInMemory)
        credProvider.setInMemoryAuthCredentials(credentials, rssFileLink, authRealm);
      else
        credProvider.setAuthCredentials(credentials, rssFileLink, authRealm);
    } else {
      URI restrictedDirectoryUri = URIUtils.normalizeUri(rssFileLink, true);
      if (isInMemory)
        credProvider.setInMemoryAuthCredentials(credentials, restrictedDirectoryUri, authRealm);
      else
        credProvider.setAuthCredentials(credentials, restrictedDirectoryUri, authRealm);
    }

    if (expectsRssElseException) {
      InputStream inS = Owl.getConnectionService().getHandler(rssFileLink).openStream(rssFileLink, null, null);
      assertNotNull(inS);

      Owl.getInterpreter().interpret(inS, feed, null);
      assertEquals("RSS 2.0", feed.getFormat());
    } else {
      AuthenticationRequiredException e = null;
      try {
        Owl.getConnectionService().getHandler(rssFileLink).openStream(rssFileLink, null, null);
      } catch (AuthenticationRequiredException e1) {
        e = e1;
      }
      assertNotNull(e);
    }
  }
  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFeedHTTP() throws Exception {
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
    IFeed feed = new Feed(feedUrl);

    InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed, null);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via FEED Protocol and HTTP.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFeedFEED() throws Exception {
    URI feedUrl = new URI((TestWebServer.rootHttp + "/feed/some_feed.xml").replace("http", "feed"));
    IFeed feed = new Feed(feedUrl);

    InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed, null);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via FEED Protocol and HTTPS.
   * https://en.wikipedia.org/wiki/Feed_URI_scheme
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFeedFEEDHTTPS() throws Exception {
    URI feedUrl = new URI("feed:" + TestWebServer.rootHttps + "/feed/some_feed.xml");
    // assertEquals("feed:https", feedUrl.getScheme()); //result: "feed"
    IFeed feed = new Feed(feedUrl);

    InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed, null);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFeedHTTPS() throws Exception {
    {
      URI feedUrl = new URI(TestWebServer.rootHttps + "/feed/some_feed.xml");
      IFeed feed = new Feed(feedUrl);

      InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
      assertNotNull(inS);

      Owl.getInterpreter().interpret(inS, feed, null);
      assertEquals("RSS 2.0", feed.getFormat());
    }
  }

  /**
   * Test a normal Feed via FILE Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFeedFILE() throws Exception {
    URL pluginLocation = FileLocator.toFileURL(Platform.getBundle("org.rssowl.core.tests").getEntry("/"));
    IConnectionService conManager = Owl.getConnectionService();
    URL feedUrl = pluginLocation.toURI().resolve("data/interpreter/feed_rss.xml").toURL();
    IFeed feed = new Feed(feedUrl.toURI());

    Triple<IFeed, IConditionalGet, URI> result = conManager.reload(feed.getLink(), null, null);

    assertEquals("RSS 2.0", result.getFirst().getFormat());
  }

  /**
   * Test Conditional GET with a compatible Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testConditionalGet() throws Exception {
//    URI feedUrl = new URI("http://rss.slashdot.org/Slashdot/slashdot/to"); // error 500 internal server error
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml"); // samer error 500 but only occasionally
    IFeed feed = new Feed(feedUrl);
    NotModifiedException e = null;

    InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
    assertNotNull(inS);

    String ifModifiedSince = null;
    String ifNoneMatch = null;
    if (inS instanceof IConditionalGetCompatible) {
      ifModifiedSince = ((IConditionalGetCompatible) inS).getIfModifiedSince();
      ifNoneMatch = ((IConditionalGetCompatible) inS).getIfNoneMatch();
    }
    IConditionalGet conditionalGet = Owl.getModelFactory().createConditionalGet(ifModifiedSince, feedUrl, ifNoneMatch);

    Map<Object, Object> conProperties = new HashMap<>();
    ifModifiedSince = conditionalGet.getIfModifiedSince();
    if (ifModifiedSince != null)
      conProperties.put(IConnectionPropertyConstants.IF_MODIFIED_SINCE, ifModifiedSince);

    ifNoneMatch = conditionalGet.getIfNoneMatch();
    if (ifNoneMatch != null)
      conProperties.put(IConnectionPropertyConstants.IF_NONE_MATCH, ifNoneMatch);

    try {
      Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, conProperties);
    } catch (NotModifiedException e1) {
      e = e1;
    }

    assertNotNull(e);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStoredCredentialsDeleted() throws Exception {
    try {
      MyCredentialsProvider.isEnabled = true;

      IConnectionService conManager = Owl.getConnectionService();
      URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
      IFeed feed = new Feed(feedUrl);

      OwlDAO.save(feed);

      ICredentials authCreds = new ICredentials() {
        @Override
        public String getDomain() {
          return null;
        }

        @Override
        public String getPassword() {
          return TestWebServer.password;
        }

        @Override
        public String getUsername() {
          return TestWebServer.username;
        }
      };

      conManager.getCredentialsProvider(feedUrl).setAuthCredentials(authCreds, feedUrl, null);

      assertNotNull(conManager.getAuthCredentials(feedUrl, null));

      OwlDAO.delete(new FeedLinkReference(feedUrl).resolve());

      assertNull(conManager.getAuthCredentials(feedUrl, null));
      assertNull(conManager.getCredentialsProvider(feedUrl).getPersistedAuthCredentials(feedUrl, null));

      ((PlatformCredentialsProvider) conManager.getCredentialsProvider(feedUrl)).clear();
    } finally {
      MyCredentialsProvider.isEnabled = false;
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testInMemoryCredentialsDeleted() throws Exception {
    try {
      MyCredentialsProvider.isEnabled = true;

      IConnectionService conManager = Owl.getConnectionService();
      URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed.xml");
      IFeed feed = new Feed(feedUrl);

      OwlDAO.save(feed);

      ICredentials authCreds = new ICredentials() {
        @Override
        public String getDomain() {
          return null;
        }

        @Override
        public String getPassword() {
          return TestWebServer.password;
        }

        @Override
        public String getUsername() {
          return TestWebServer.username;
        }
      };

      conManager.getCredentialsProvider(feedUrl).setInMemoryAuthCredentials(authCreds, feedUrl, null);

      assertNotNull(conManager.getAuthCredentials(feedUrl, null));
      assertNull(conManager.getCredentialsProvider(feedUrl).getPersistedAuthCredentials(feedUrl, null));

      OwlDAO.delete(new FeedLinkReference(feedUrl).resolve());

      assertNull(conManager.getAuthCredentials(feedUrl, null));
      assertNull(conManager.getCredentialsProvider(feedUrl).getPersistedAuthCredentials(feedUrl, null));

      ((PlatformCredentialsProvider) conManager.getCredentialsProvider(feedUrl)).clear();
    } finally {
      MyCredentialsProvider.isEnabled = false;
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testLoadFeedFromWebsiteWithRedirect() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI(TestWebServer.rootHttp + "/redirect-feed/some_feed_linked.html");

    URI feedUrlOut = conManager.getFeed(feedUrl, new NullProgressMonitor());
    assertNotNull(feedUrlOut);
    assertEquals(TestWebServer.rootHttp + "/feed/some_feed.xml", feedUrlOut.toString());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testLoadFeedFromWebsiteWithoutRedirect() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed_linked.html");

    URI feedUrlOut = conManager.getFeed(feedUrl, new NullProgressMonitor());
    assertNotNull(feedUrlOut);
    assertEquals(TestWebServer.rootHttp + "/feed/some_feed.xml", feedUrlOut.toString());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testLoadEntityEncodedFeedFromWebsite() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed/some_feed_linked2.html");

    assertEquals(TestWebServer.rootHttp + "/feed/some_feed&#63;a=1&b=2", conManager.getFeed(feedUrl, new NullProgressMonitor()).toString());
  }

  /**
   * @throws Exception
   * @deprecated TODO digg keyword feed replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testKeywordFeeds() throws Exception {
    String keywords = "blog feed";
    String URL_INPUT_TOKEN = "[:]";
    String KEYWORD_FEED_EXTENSION_POINT = "org.rssowl.ui.KeywordFeed";

    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(KEYWORD_FEED_EXTENSION_POINT);

    /* For each contributed property keyword feed */
    for (IConfigurationElement element : elements) {
      String id = element.getAttribute("id");
      if ("org.rssowl.ui.DiggKeywordFeed".equals(id))
        continue;

      String url = element.getAttribute("url");

      String feedUrlStr = StringUtils.replaceAll(url, URL_INPUT_TOKEN, URIUtils.urlEncode(keywords));
      URI feedUrl = new URI(feedUrlStr);
      IFeed feed = new Feed(feedUrl);

      try {
        InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
        assertNotNull(id, inS);

        assertNull(id, feed.getFormat());
        try {
          Owl.getInterpreter().interpret(inS, feed, null);
        } catch (EncodingException e) {
          inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
          Owl.getInterpreter().interpret(inS, feed, Collections.singletonMap((Object) DefaultProtocolHandler.USE_PLATFORM_ENCODING, (Object) Boolean.TRUE));
        }
        assertNotNull(id, feed.getFormat());
      } catch (Exception e) {
        e.printStackTrace();
        fail(feedUrlStr);
      }
    }
  }

  /**
   * @throws Exception
   * @deprecated TODO broken on rssowl.org and no replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testFeedSearch_SingleLanguage() throws Exception {
    String link = Controller.getDefault().toFeedSearchLink("blog");

    Map<Object, Object> properties = new HashMap<>();
    properties.put(IConnectionPropertyConstants.CON_TIMEOUT, 60000);
    properties.put(IConnectionPropertyConstants.ACCEPT_LANGUAGE, "de");

    InputStream inS = Owl.getConnectionService().getHandler(new URI(link)).openStream(new URI(link), null, properties);
    String content = StringUtils.readString(new BufferedReader(new InputStreamReader(inS)));
    assertNotNull(content);

    List<String> links = RegExUtils.extractLinksFromText(content, false);
    assertTrue(!links.isEmpty());
    assertTrue(links.size() > 40);
  }

  /**
   * @throws Exception
   * @deprecated TODO broken on rssowl.org and no replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testFeedSearch_DoubleLanguage() throws Exception {
    String link = Controller.getDefault().toFeedSearchLink("blog");

    Map<Object, Object> properties = new HashMap<>();
    properties.put(IConnectionPropertyConstants.CON_TIMEOUT, 60000);
    properties.put(IConnectionPropertyConstants.ACCEPT_LANGUAGE, "en,de");

    InputStream inS = Owl.getConnectionService().getHandler(new URI(link)).openStream(new URI(link), null, properties);
    String content = StringUtils.readString(new BufferedReader(new InputStreamReader(inS)));
    assertNotNull(content);

    List<String> links = RegExUtils.extractLinksFromText(content, false);
    assertTrue(!links.isEmpty());
    assertTrue(links.size() > 40);
  }

  /**
   * @throws Exception
   * @deprecated TODO broken on rssowl.org and no replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testFeedSearch_WrongLanguage() throws Exception {
    String link = Controller.getDefault().toFeedSearchLink("blog");

    Map<Object, Object> properties = new HashMap<>();
    properties.put(IConnectionPropertyConstants.CON_TIMEOUT, 60000);
    properties.put(IConnectionPropertyConstants.ACCEPT_LANGUAGE, "en-us,de_de");

    InputStream inS = Owl.getConnectionService().getHandler(new URI(link)).openStream(new URI(link), null, properties);
    String content = StringUtils.readString(new BufferedReader(new InputStreamReader(inS)));
    assertNotNull(content);

    List<String> links = RegExUtils.extractLinksFromText(content, false);
    assertTrue(!links.isEmpty());
    assertTrue(links.size() > 40);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWebsite() throws Exception {
    String link = TestWebServer.rootHttp + "/feed/some_feed_linked.html";

    Map<Object, Object> properties = new HashMap<>();
    properties.put(IConnectionPropertyConstants.CON_TIMEOUT, 60000);

    InputStream inS = Owl.getConnectionService().getHandler(new URI(link)).openStream(new URI(link), null, properties);
    String content = StringUtils.readString(new BufferedReader(new InputStreamReader(inS)));
    assertNotNull(content);

    List<String> links = RegExUtils.extractLinksFromText(content, false);
    assertTrue(!links.isEmpty());
    assertTrue(links.size() >= 3); //must find the 3 absolute links (with http: or https:)
  }

  /**
   * @throws Exception
   * @deprecated TODO google reader was discontinued, find google reader
   * replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testGoogleReaderSync() throws Exception {
    String authToken = SyncUtils.getGoogleAuthToken("rssowl@mailinator.com", "rssowl.org", true, new NullProgressMonitor());
    assertNotNull(authToken);

    assertEquals(authToken, SyncUtils.getGoogleAuthToken("rssowl@mailinator.com", "rssowl.org", false, new NullProgressMonitor()));

    String newAuthToken = SyncUtils.getGoogleAuthToken("rssowl@mailinator.com", "rssowl.org", true, new NullProgressMonitor());
    assertFalse(authToken.equals(newAuthToken));

    authToken = newAuthToken;

    URI uri = URI.create(SyncUtils.GOOGLE_READER_OPML_URI);
    IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);

    Map<Object, Object> properties = new HashMap<>();

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", SyncUtils.getGoogleAuthorizationHeader(authToken)); //$NON-NLS-1$
    properties.put(IConnectionPropertyConstants.HEADERS, headers);

    InputStream inS = handler.openStream(uri, new NullProgressMonitor(), properties);

    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(inS);
    assertTrue(!elements.isEmpty());
  }

  /**
   * @throws Exception
   * @deprecated TODO google reader was discontinued, find google reader
   * replacement
   */
  @Deprecated
  @Test
  @Ignore
  public void testGetGoogleReaderAPIToken() throws Exception {
    String apiToken = SyncUtils.getGoogleApiToken("rssowl@mailinator.com", "rssowl.org", new NullProgressMonitor());
    assertNotNull(apiToken);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNewsTransformer() throws Exception {
    String link = TestWebServer.rootHttp + "/feed/some_feed.xml";

    List<LinkTransformer> transformers = Controller.getDefault().getLinkTransformers();
    for (LinkTransformer transformer : transformers) {
      String transformedUrl = transformer.toTransformedUrl(link);
      String urlMask = transformer.getUrlMask();
      String urlPrefix = urlMask.substring(0, urlMask.indexOf(LinkTransformer.URL_INPUT_TOKEN));
      if (urlPrefix == null || urlPrefix.length()==0)
        fail("unexpected");
      assertTrue("failed url transform for: " + transformedUrl, transformedUrl.startsWith(urlPrefix));
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNewsTransformerEmbedded() throws Exception {
    String link = TestWebServer.rootHttp + "/feed/some_feed.xml";

    LinkTransformer transformer = Controller.getDefault().getLinkTransformer(Controller.DEFAULT_TRANSFORMER_ID);
    String transformedUrl = Controller.getDefault().getEmbeddedTransformedUrl(link);
    String urlMask = transformer.getUrlMask();
    String urlPrefix = urlMask.substring(0, urlMask.indexOf(LinkTransformer.URL_INPUT_TOKEN));
    if (urlPrefix == null || urlPrefix.length()==0)
      fail("unexpected");
    assertTrue("failed url transform for: " + transformedUrl, transformedUrl.startsWith(urlPrefix));
  }

  @Test
  @SuppressWarnings("nls")
  @Ignore
  public void testJiraUrl() throws Exception {
//TODO:UNFINISHED https://github.com/Xyrio/RSSOwlnix/issues/24
    URI feedUrl = new URI(TestWebServer.rootHttp + "/feed-jira/sr/jira.issueviews:searchrequest-rss/temp/SearchRequest.xml?jqlQuery=project+in+%28XYZ%29+and+updated+%3E%3D+-14d+ORDER+BY+updated+DESC&tempMax=1000");

    IFeed feed1 = Owl.getModelFactory().createFeed(null, feedUrl);
    OwlDAO.save(feed1);

    IFeed feed = new Feed(feedUrl);
    OwlDAO.save(feed);

    InputStream inS = Owl.getConnectionService().getHandler(feed.getLink()).openStream(feed.getLink(), null, null);
    assertNotNull(inS);
    String content = StringUtils.readString(new BufferedReader(new InputStreamReader(inS)));
    assertNotNull(content);
  }
}