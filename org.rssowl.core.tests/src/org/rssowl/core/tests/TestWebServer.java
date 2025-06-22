package org.rssowl.core.tests;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

/**
 * https://www.eclipse.org/jetty/documentation/9.4.x/embedded-examples.html
 * <pre>
 * - port: http
 * - portSsl: https
 * - data in httproot
 * - /feed/: maps to httproot/data/ folder
 * - /feed-no-listing/: public but no listing of content
 * - /redirect-feed/: redirects to /feed/
 * - /auth-feed/: requires authentication
 * - /rssowlnix/: shares local update site folder, set updateSiteLocalFolder above
 * </pre>
 */
public class TestWebServer {
  private static final String updateSiteLocalFolder = "file:///C:/rssowlnixprj/RSSOwlnix/releng/product/target/repository";
  private static final String updateSiteRemoteFolder = "https://xyrio.github.io/RSSOwlnix-site/p2/program";
  private static final String webServerRootFolder = new File("httproot").getAbsolutePath().replace("\\", "/");
  private static final String webServerDataFolder = new File("httproot/data").getAbsolutePath().replace("\\", "/");
  private static Server webServer;
  private static String host = "127.0.0.1";
  private static int port = 88;
  private static int portSsl = 8843;
  private static StatisticsHandler statisticsHandler;

  public static final String rootHttp = "http://" + host + ":" + port;
  public static final String rootHttps = "https://" + host + ":" + portSsl;
  public static final String username = "super";
  public static final String password = "12345";

  public static void main(String[] args) {
    start(false);
  }

  private static long durationIdlingMS = 0;

  private static void startShutdownTimer(boolean autoShutdown) {
    if (autoShutdown) {
      final long msCheckInterval = 1000;
      final long msShutdownDuration = 10000;

      Timer timer = new Timer("webServerShutdownTimer", true);
      timer.scheduleAtFixedRate(new TimerTask() {
        long lastBytesCount = 0;

        @Override
        public void run() {
          synchronized (TestWebServer.class) {
            long currentBytesCount = statisticsHandler.getBytesWritten() + statisticsHandler.getBytesRead();
//            System.out.println("*** TestWebServer: currentBytesCount=" + currentBytesCount);
            if (currentBytesCount != lastBytesCount) {
              lastBytesCount = currentBytesCount; //changed
              durationIdlingMS = 0;
            } else {
              durationIdlingMS += msCheckInterval; //idling
            }
            if (durationIdlingMS >= msShutdownDuration) {
              try {
                System.out.println("*** TestWebServer: shutting down due to inactivity for over ms=" + msShutdownDuration);
                webServer.stop();
                timer.cancel();
                webServer = null;
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          }
        }
      }, 0, msCheckInterval);
    }
  }

  public static void start(boolean autoShutdown) {
    synchronized (TestWebServer.class) {
      durationIdlingMS = 0;

      if (webServer != null)
        return;

      System.out.println("*** TestWebServer: starting webserver...");

      // sni stuff needed because of fake keystore
      // if true:
      // must not use ip for hostname
      // must not use localhost
      // must have a . in hostname (java 17+)
      boolean isSniRequired = false;
      boolean isSniHostCheck = false;

      try {
        File keystoreFile = new File("unimportant_rsa_2048.keystore");
        if (!keystoreFile.exists())
          throw new FileNotFoundException(keystoreFile.getAbsolutePath());

        final Server server = new Server();
        server.setRequestLog(new CustomRequestLog());
        server.setStopAtShutdown(true); // daemon
        server.setStopTimeout(1000);
        {
          HttpConfiguration httpConfig = new HttpConfiguration();
          httpConfig.setSecureScheme("https");
          httpConfig.setSecurePort(portSsl);
          httpConfig.setOutputBufferSize(32768);

          ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
          http.setHost(host);
          http.setPort(port);
          http.setIdleTimeout(5000);

          SslContextFactory.Server sslContextFactoryServer = new SslContextFactory.Server();
          sslContextFactoryServer.setKeyStorePath(keystoreFile.getAbsolutePath());
          sslContextFactoryServer.setKeyStorePassword("somepassstore"); //when changed must recreate keystore
          sslContextFactoryServer.setKeyManagerPassword("somepasskey"); //when changed must recreate keystore
          sslContextFactoryServer.setSniRequired(isSniRequired);
          sslContextFactoryServer.setRenegotiationAllowed(false);

          // OPTIONAL: Un-comment the following to use Conscrypt for SSL instead of
          // the native JSSE implementation.

//      Security.addProvider(new OpenSSLProvider());
//      sslContextFactory.setProvider("Conscrypt");

          HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
          SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
          secureRequestCustomizer.setStsMaxAge(2000);
          secureRequestCustomizer.setStsIncludeSubDomains(true);
          secureRequestCustomizer.setSniHostCheck(isSniHostCheck);
          httpsConfig.addCustomizer(secureRequestCustomizer);

          ServerConnector https = new ServerConnector(server, new SslConnectionFactory(sslContextFactoryServer, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
          https.setHost(host);
          https.setPort(portSsl);
          https.setIdleTimeout(5000);

          server.setConnectors(new Connector[] { http, https });
        }

//        Collection<ServletHolder> servletHolders = new ArrayList<>();

        ContextHandler feedHandler = handlerRessources("/feed", webServerDataFolder);
//        servletHolders.add(serveletRessources("/feed/*", webServerRootFolder));

        ContextHandler feedNoListingHandler = handlerRessources("/feed-no-listing", webServerDataFolder, false);
//        servletHolders.add(serveletRessources("/feed/*", webServerRootFolder));

        MovedContextHandler redirectHandler = handlerRedirect("/redirect-feed", "/feed/");
//        servletHolders.add(serveletRedirect("/redirect-feed/*", "/feed/"));

        ContextHandler authBasicHandler = handlerRessources("/auth-feed", webServerDataFolder);
//      servletHolders.add(serveletRessources("/auth-feed/*", webServerRootFolder));
        SecurityHandler securityBasicHandler = handlerSecurity("/auth-feed/*", new BasicAuthenticator(), "admin");
        securityBasicHandler.setHandler(authBasicHandler);

        ContextHandler quitHandler = new ContextHandler("/manual-quit");
        quitHandler.setHandler(new DefaultHandler() {
          @Override
          public boolean handle(Request request, Response response, Callback callback) throws Exception {
            new Thread(new Runnable() {
              @Override
              public void run() {
                try {
                  server.stop();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }).start();
            return super.handle(request, response, callback);
          }
        });

        ContextHandler updateProgramHandler = handlerRessources("/rssowlnix", updateSiteLocalFolder);

        ContextHandler rootHandler = handlerRessources("/", webServerRootFolder, false);

        DefaultHandler defaultHandler = new DefaultHandler() {
          @SuppressWarnings("unused")
          @Override
//          public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
          public boolean handle(Request request, Response response, Callback callback) throws Exception {
            //favicon.ico request goes through here
            //super.handle(target, baseRequest, request, response); //prevent 404 return

            if (response.getStatus() == 0)
              response.setStatus(HttpStatus.NOT_FOUND_404);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML.toString());

            try (ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);) {
//                writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found");
//                writer.write("</TITLE>\n<BODY>\n<H2>Error 404 - Not Found.</H2>\n");
//                writer.write("No context on this server matched or handled this request.<BR>");
              if (response.getStatus() != HttpStatus.OK_200) {
                writer.write("status: " + response.getStatus() + ": " + HttpStatus.getMessage(response.getStatus()) + "<br>");
              }
              writer.write("Contexts known to this server are: <ul>");

              Server server = getServer();
              List<ContextHandler> handlers = server == null ? null : server.getDescendants(ContextHandler.class);

              for (int i = 0; handlers != null && i < handlers.size(); i++) {
                ContextHandler context = handlers.get(i);
                int localPort = Request.getLocalPort(request);
                if (context.isRunning()) {
                  writer.write("<li><a href=\"");
                  if (context.getVirtualHosts() != null && context.getVirtualHosts().size() > 0)
                    writer.write(request.getHttpURI().getScheme() + "://" + context.getVirtualHosts().get(0) + ":" + localPort);
                  writer.write(context.getContextPath());
                  if (context.getContextPath().length() > 1 && context.getContextPath().endsWith("/"))
                    writer.write("/");
                  writer.write("\">");
                  writer.write(context.getContextPath());
                  if (context.getVirtualHosts() != null && context.getVirtualHosts().size() > 0)
                    writer.write("&nbsp;@&nbsp;" + context.getVirtualHosts().get(0) + ":" + localPort);
                  writer.write("&nbsp;--->&nbsp;");
                  writer.write(context.toString());
                  writer.write("</a></li>\n");
                } else {
                  writer.write("<li>");
                  writer.write(context.getContextPath());
                  if (context.getVirtualHosts() != null && context.getVirtualHosts().size() > 0)
                    writer.write("&nbsp;@&nbsp;" + context.getVirtualHosts().get(0) + ":" + localPort);
                  writer.write("&nbsp;--->&nbsp;");
                  writer.write(context.toString());
                  if (context.isFailed())
                    writer.write(" [failed]");
                  if (context.isStopped())
                    writer.write(" [stopped]");
                  writer.write("</li>\n");
                }
              }

              writer.write("</ul><hr>");

//                baseRequest.getHttpChannel().getHttpConfiguration()
//                    .writePoweredBy(writer,"<a href=\"http://eclipse.org/jetty\"><img border=0 src=\"/favicon.ico\"/></a>&nbsp;","<hr/>\n");

//                writer.write("\n</BODY>\n</HTML>\n");
              writer.flush();
//              response.setContentLength(writer.size());
              response.getHeaders().put(HttpHeader.CONTENT_LENGTH, writer.size());
              response.write(true, ByteBuffer.wrap(writer.getByteArray()), null);
            }
            return true;
          }
        };

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.addHandler(updateProgramHandler);
        handlers.addHandler(feedHandler);
        handlers.addHandler(feedNoListingHandler);
        handlers.addHandler(redirectHandler);
        handlers.addHandler(securityBasicHandler);
        handlers.addHandler(quitHandler);
        handlers.addHandler(rootHandler);

//        // ### servlets start
//        ServletContextHandler servletHandlers = new ServletContextHandler();
//        {
//          servletHandlers.setContextPath("/");
//          for (ServletHolder servletHolder : servletHolders)
//            servletHandlers.addServlet(servletHolder, servletHolder.getName());
//
//          //root last, required by spec
//          ServletHolder holderRoot = new ServletHolder("default", new HttpServlet() {
//            @SuppressWarnings("unused")
//            @Override
//            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//
//              StringBuilder sb = new StringBuilder();
//              sb.append("<html><head></head><body>\n");
//              for (ServletHolder sh : servletHolders) {
//                sb.append("  <p><a href=\"").append(sh.getName()).append("\">");
//                sb.append(sh.getName());
//                sb.append(" -> ");
//                sb.append(sh.getInitParameter("resourceBase"));
//                sb.append("</a></p>\n");
//              }
//              sb.append("</body></html>");
//              String content = sb.toString();
//              resp.setContentLength(sb.length());
//              resp.getWriter().write(content);
//              resp.setContentType("text/html");
//              resp.setStatus(HttpServletResponse.SC_OK);
//            }
//
//          });
////        ret.setResourceBase(resolveIfAlias(webServerRootFolder));
////        holderRoot.setInitParameter("resourceBase", resolveIfAlias(webServerRootFolder));
////        holderRoot.setInitParameter("dirAllowed", "true");
//          servletHandlers.addServlet(holderRoot, "/");
//
////          securityHandler.setHandler(servletHandlers);
//          // ### servlets end
//        }

        statisticsHandler = new StatisticsHandler();
        statisticsHandler.setHandler(handlers);

        server.setHandler(statisticsHandler);
        server.setDefaultHandler(defaultHandler);
        server.setErrorHandler(defaultHandler);

//    System.out.println(server.dump());
        server.start();
        webServer = server;
        startShutdownTimer(autoShutdown);
        System.out.println("*** TestWebServer: started webserver");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * because server ignores aliases (mklink/junction/etc) instead of using real
   * location
   */
  private static String resolveIfAlias(String sdir) {
    try {
      Path path = null;
      if (sdir.startsWith("file:///"))
        path = Path.of(sdir.substring(8));
      else
        path = Path.of(sdir);
      path = path.toFile().getAbsoluteFile().toPath();
      if (!Files.exists(path))
        throw new FileNotFoundException(path + "(" + sdir + ")");
//      if (Files.isSymbolicLink(path)) //does not notice mklink/junctions
      path = path.toRealPath();
      String ret = path.toString().replace("\\", "/");
      return ret;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static MovedContextHandler handlerRedirect(String fromUrl, String toUrl) {

    MovedContextHandler ret = new MovedContextHandler();
    ret.setContextPath(fromUrl);
    ret.setRedirectURI(toUrl);
    ret.setDiscardPathInContext(false);
    ret.setDiscardQuery(false);
    ret.setDynamic(false);
    //https://stackoverflow.com/questions/77861586/jetty-12-03-gives-a-301-when-there-is-no-trailing-slash-for-post-and-get-request
//    ret.setAllowNullPathInContext(true); //destroys servlet behaviour and causes problems with rest, json, soap, spring, etc that expect Servlet behaviors.
    return ret;
  }

//  public static ServletHolder serveletRedirect(String fromUrl, String toUrl) {
//    return new ServletHolder(fromUrl, new HttpServlet() {
//      @SuppressWarnings("unused")
//      @Override
//      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        resp.sendRedirect(toUrl);
//      }
//    });
//  }

  private static ContextHandler handlerRessources(String contextUrl, String sdir) {
    return handlerRessources(contextUrl, sdir, true);
  }

  private static ContextHandler handlerRessources(String contextUrl, String sdir, boolean isDirAllowed) {

    ContextHandler ret = new ContextHandler(contextUrl);
    /**
     * because server ignores aliases (mklink/junction/etc) instead of using
     * real location
     */
//    ret.addAliasCheck(new AliasCheck() {
//      @Override
//      public boolean checkAlias(String pathInContext, Resource resource) {
//        //compare backwards only what exists in both
//        String r1 = new StringBuilder(pathInContext).reverse().toString();
//        String r2 = new StringBuilder(resource.toString()).reverse().toString();
//        int minLength = Math.min(r1.length(), r2.length());
//        r1 = r1.substring(0, minLength);
//        r2 = r2.substring(0, minLength);
//        return r1.equals(r2);
//      }
//    });
    ret.setHandler(handlerRessources(sdir, isDirAllowed));
    return ret;
  }

  private static ResourceHandler handlerRessources(String sdir, boolean isDirAllowed) {

    String sdirResolved = resolveIfAlias(sdir);
    ResourceHandler ret = new ResourceHandler();
    ret.setDirAllowed(isDirAllowed);
    ret.setBaseResourceAsString(sdirResolved);
    //bug: refresh of a feed switches between 200 and 404
    // activating caching in any way causes the resource handler to not mark the request as
    // handled, when "resource did not change" (304), which then allows the request to reach
    // the defaultHandler which returns 404 for the feed
//    ret.setCacheControl("public, max-age=-1");
    ret.setEtags(true);
//    ret.setAcceptRanges(true);
    return ret;
  }

//  private static ServletHolder servletRessources(String contextUrl, String sdir) {
//
//    ServletHolder holder = new ServletHolder(contextUrl, DefaultServlet.class);
//    holder.setInitParameter("resourceBase", resolveIfAlias(sdir));
//    holder.setInitParameter("dirAllowed", "true");
//    holder.setInitParameter("pathInfoOnly", "true");
////    holder.setInitParameter("cacheControl", "public, max-age=600");
////    holder.setInitParameter("etags", "true");
//    return holder;
//  }

  private static SecurityHandler handlerSecurity(String contextUrl, Authenticator authenticator, String... rolesAccept) {
//    ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler() {
////      @Override
////      public void handle(String pathInContext, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
////        System.out.println("*** request: "+request);
////        super.handle(pathInContext, baseRequest, request, response);
////        System.out.println("*** response:\n"+response);
////      }
//    };
    SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
//    //least significant first
////    securityHandler.put("/*", Constraint.ALLOWED);
////    securityHandler.put("", Constraint.ALLOWED);

    String authRealm = authenticator.getAuthenticationType() + " Restricted Directory";
//  securityHandler.setRealmName(authRealm); //is ignored. login service name is used instead
    {
      // http://www.eclipse.org/jetty/documentation/current/configuring-security.html#configuring-security-authentication
      securityHandler.setAuthenticator(authenticator);
//      securityHandler.setAuthenticator(new BasicAuthenticator());
//      //TODO other authentication methods
//      securityHandler.setAuthenticator(new DigestAuthenticator());
//      securityHandler.setAuthenticator(new FormAuthenticator());
//      securityHandler.setAuthenticator(new ClientCertAuthenticator());
//      securityHandler.setAuthenticator(new SpnegoAuthenticator());
    }
    {
      UserStore userStore = new UserStore();
//    userStore.addUser("usr", new Password("pw"), new String[] { "user" });
      userStore.addUser(username, new Password(password), new String[] { "user", "admin" });

      HashLoginService loginService = new HashLoginService();
      loginService.setName(authRealm); //used as realm name
//    loginService.setConfig(realmPropertiesFile.getAbsolutePath());
      loginService.setUserStore(userStore);
      loginService.setReloadInterval(0); //off
//    server.addBean(loginService);
      securityHandler.setLoginService(loginService);
    }
    {
//      Constraint constraint = new Constraint() {
//        @Override
//        public String getName() {
//          return "unnamed@" + hashCode();
//        }
//
//        @Override
//        public Transport getTransport() {
//          return Transport.ANY;
//        }
//
//        @Override
//        public Authorization getAuthorization() {
//          return Authorization.SPECIFIC_ROLE;
//        }
//
//        @Override
//        public Set<String> getRoles() {
//          if (rolesAccept == null)
//            return new HashSet<>();
//          return new HashSet<>(Arrays.asList(rolesAccept));
//        }
//      };
//      securityHandler.put(contextUrl, constraint);
      securityHandler.put(contextUrl, Constraint.from(rolesAccept));
    }
    return securityHandler;
  }

}
