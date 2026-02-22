/*
    240979: Since I am using netty http core5, I can not just assign local address, so I am using this planner
    This should add IP address from the correct network IF to the route, therefore the App should use it
 */
package models;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.net.InetAddress;

public class LocalAddressRoutePlanner implements HttpRoutePlanner {
    private final InetAddress localAddress;
    private final HttpRoutePlanner defaultPlanner;

    public LocalAddressRoutePlanner(InetAddress localAddress) {
        this.localAddress = localAddress;
        this.defaultPlanner = new DefaultRoutePlanner(null);
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpContext context) throws HttpException {
        HttpRoute route = defaultPlanner.determineRoute(target, context);
        if (localAddress != null) {
            // Create a new route with the same parameters but add local address
            return new HttpRoute(
                    route.getTargetHost(),
                    localAddress,
                    route.getProxyHost(),
                    route.isSecure(),
                    route.getTunnelType(),
                    route.getLayerType()
            );
        }
        return route;
    }
}