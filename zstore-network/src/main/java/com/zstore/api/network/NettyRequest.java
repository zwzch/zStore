package com.zstore.api.network;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public class NettyRequest {
    private final HttpRequest request;
    private final QueryStringDecoder query;
    private RequestMethod method;
    private final static char PATH_SEPARATOR_CHAR = '/';
    private final static String PATH_SEPARATOR_STRING = String.valueOf(PATH_SEPARATOR_CHAR);
    public NettyRequest(HttpRequest request) {
        this.request = request;
        query = new QueryStringDecoder(request.uri());
        HttpMethod httpMethod = request.method();
        if (httpMethod.equals(HttpMethod.GET)) {
            method = RequestMethod.GET;
        } else if(httpMethod.equals(HttpMethod.POST)) {
            method = RequestMethod.POST;
        } else {
            throw new NetWorkException(NetWorkException.Code.METHOD_NOT_SUPPORT, "http method not supported: " + httpMethod);
        }
    }
    public String getUri() {
        return request.uri();
    }

    public HttpRequest getRequest() {
        return request;
    }

    public RequestMethod getMethod() {
        return this.method;
    }

    public boolean matchesOperation(String operation) {
        return matchPathSegments(request.uri(), 0, operation, true) >= 0;
    }

    private static int matchPathSegments(String path, int pathOffset, String pathSegments, boolean ignoreCase) {
        // start the search past the leading slash, if one exists
        pathOffset += path.startsWith("/", pathOffset) ? 1 : 0;
        // for search purposes we strip off leading and trailing slashes from the pathSegments to search for.
        int pathSegmentsStartOffset = pathSegments.startsWith(PATH_SEPARATOR_STRING) ? 1 : 0;
        int pathSegmentsLength = Math.max(
                pathSegments.length() - pathSegmentsStartOffset - (pathSegments.endsWith(PATH_SEPARATOR_STRING) ? 1 : 0), 0);
        int nextPathSegmentOffset = -1;
        if (path.regionMatches(ignoreCase, pathOffset, pathSegments, pathSegmentsStartOffset, pathSegmentsLength)) {
            int nextCharOffset = pathOffset + pathSegmentsLength;
            // this is only a match if the end of the path was reached or the next character is a slash (starts new segment).
            if (nextCharOffset == path.length() || path.charAt(nextCharOffset) == PATH_SEPARATOR_CHAR) {
                nextPathSegmentOffset = nextCharOffset;
            }
        }
        return nextPathSegmentOffset;
    }
}
