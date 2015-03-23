package com.glt.sockjs;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Starter;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.sockjs.SockJSHandler;
import io.vertx.ext.apex.handler.sockjs.SockJSHandlerOptions;

/**
 * Created by levin on 3/23/2015.
 */
public class SockjsVerticle extends AbstractVerticle {

    public static final Logger log = LoggerFactory.getLogger(SockjsVerticle.class);

    HttpServer httpServer;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        SockJSHandlerOptions sockjsOptions = new SockJSHandlerOptions();

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, sockjsOptions);

        sockJSHandler.socketHandler(sock -> {

            log.info("sock connected: " + sock.writeHandlerID());

            sock.handler(d -> {
                log.info("data received: " + d.toString());
            });

            sock.exceptionHandler(t -> {
                log.error("sock error ", t);
            });

            sock.endHandler(v -> {
                log.info("sock disconnected: " + sock.writeHandlerID());
            });
        });

        StaticHandler staticHandler = StaticHandler.create();
        staticHandler.setWebRoot("public");
        staticHandler.setIndexPage("index.html");

        Router router = Router.router(vertx);

        router.route("/").handler(staticHandler);
        router.getWithRegex(".+\\.js").handler(staticHandler);

        router.route("/sjs/*").handler(sockJSHandler);

        HttpServerOptions httpOptions = new HttpServerOptions();
        httpOptions.setPort(8090);

        httpServer = vertx.createHttpServer(httpOptions);

        httpServer.requestHandler(router::accept);

        httpServer.listen(ar -> {
            if(ar.succeeded()){
                startFuture.complete();
            }
            else{
                startFuture.fail(ar.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if(httpServer != null){
            httpServer.close(ar -> {
                if(ar.succeeded()){
                    stopFuture.complete();
                }
                else{
                    stopFuture.fail(ar.cause());
                }
            });
        }
        else{
            stopFuture.complete();
        }
    }


    public static void main(String[] args) {

        Starter starter = new Starter();
        starter.run(new String[] {
                "run",
                "com.glt.sockjs.SockjsVerticle",
                "-cluster",
                "-instances 2"
        });

    }

}
