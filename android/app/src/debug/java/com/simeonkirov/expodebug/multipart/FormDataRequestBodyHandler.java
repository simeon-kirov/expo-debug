package com.simeonkirov.expodebug.multipart;

import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.network.NetworkingModule;
import com.facebook.react.modules.network.ResponseUtil;

import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * <p>This class is part of the workaround for the React Native bug with Multipart
 * Form Data requests handling.</p>
 * <p>In general, this is a code copy-pasted from {@code com.facebook.react.modules.network.NetworkingModule}
 * and then optimized, where the final goal is to allow more safe and flexible management of the Multiparts.</p>
 */
public class FormDataRequestBodyHandler implements NetworkingModule.RequestBodyHandler {
    private static final String CONTENT_TYPE_HEADER_NAME = "content-type";
    private static final String REQUEST_BODY_KEY_STRING = "string";
    private static final String REQUEST_BODY_KEY_URI = "uri";
    private static final String REQUEST_BODY_KEY_FORM_DATA = "formData";
    private static final String REQUEST_BODY_KEY_MIME_TYPE = "mimeType";


    private static final int FAKE_REQUEST_ID = -1;

    private final ReactApplicationContext context;

    public FormDataRequestBodyHandler(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    public boolean supports(ReadableMap data) {
        return data.hasKey(REQUEST_BODY_KEY_FORM_DATA);
    }

    @Override
    public RequestBody toRequestBody(ReadableMap data, String contentType) {
        MediaType requestMediaType =
                contentType!=null ? MediaType.parse(contentType)
                                  : RequestUtils.MimeTypes.MULTIPART_FORM_DATA.getMediaType();

        ReadableArray parts = data.getArray(REQUEST_BODY_KEY_FORM_DATA);
        if (parts==null || parts.size()==0) {
            return RequestUtils.getEmptyBody("post");
        }

        RequestBody result;
        try {
            MultipartBody.Builder multipartBuilder = constructMultipartBody(parts, requestMediaType);
            result =  multipartBuilder.build();
        } catch (Exception e) {
            String error = "Failed to compose multipart request from the data payload: "+e.getMessage();
            Log.e(LOG_TAG, error, e);
            ResponseUtil.onRequestError(context, FAKE_REQUEST_ID, error, e);
            result = RequestUtils.getEmptyBody("post");
        }

        return result;
    }

    /**
     * Enhanced copy of original {@code NetworkingModule.constructMultipartBody()} method.
     * It delegates the evaluation of form-data part to {@link #getPartContentType} which is the
     * actual bug fix.
     *
     * @param body The form-data part to be processed.
     * @param requestMediaType The MIME Type of the form-data part.
     *
     * @return Builder for the form-data part.
     */
    private MultipartBody.Builder constructMultipartBody(ReadableArray body, MediaType requestMediaType) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(requestMediaType);

        for (int i = 0, size = body.size(); i < size; i++) {
            ReadableMap bodyPart = body.getMap(i);

            // Determine part's content type.
            ReadableArray headersArray = bodyPart.getArray("headers");
            Headers headers = extractHeaders(headersArray);
            if (headers == null) {
                throw new BadDataException("Missing or invalid header format for FormData part.");
            }

            MediaType partContentType = getPartContentType(bodyPart, headers);

            if (bodyPart.hasKey(REQUEST_BODY_KEY_STRING)) {
                String bodyValue = bodyPart.getString(REQUEST_BODY_KEY_STRING);
                multipartBuilder.addPart(headers, RequestBody.create(partContentType, bodyValue));
            } else if (bodyPart.hasKey(REQUEST_BODY_KEY_URI)) {
                if (partContentType == null) {
                    throw new BadDataException("Binary FormData part needs a content-type header.");
                }
                String fileContentUriStr = bodyPart.getString(REQUEST_BODY_KEY_URI);
                InputStream fileInputStream = RequestUtils.getFileInputStream(context, fileContentUriStr);
                if (fileInputStream == null) {
                    throw new BadDataException("Could not retrieve file for uri " + fileContentUriStr);
                }
                multipartBuilder.addPart(headers, RequestUtils.create(partContentType, fileInputStream));
            } else {
                ResponseUtil.onRequestError(
                        context, FAKE_REQUEST_ID, "Unrecognized FormData part.", null);
            }
        }
        return multipartBuilder;
    }

    /**
     * This method is the actual fix for the multipart bug. It tries to evaluate correctly the MIME
     * Type of the form-data part either from Content-type header, or from the mimeType property.
     *
     * @param bodyPart The form-data part body.
     * @param headers The form-data part headers.
     *
     * @return The MIME Type for the form-data part.
     */
    private MediaType getPartContentType(ReadableMap bodyPart, Headers headers) {
        MediaType partContentType = null;
        String partContentTypeStr = headers.get(CONTENT_TYPE_HEADER_NAME);

        if (partContentTypeStr==null) {
            partContentTypeStr = bodyPart.getString(REQUEST_BODY_KEY_MIME_TYPE);
        }

        if (partContentTypeStr!=null) {
            partContentType = MediaType.parse(partContentTypeStr);
            // Remove the content-type header because MultipartBuilder gets it explicitly as an
            // argument and doesn't expect it in the headers array.
            headers = headers.newBuilder().removeAll(CONTENT_TYPE_HEADER_NAME).build();
        } else {
            partContentType = RequestUtils.MimeTypes.APPLICATION_OCTET_STREAM.getMediaType();
        }

        return partContentType;
    }

    private @Nullable Headers extractHeaders(@Nullable ReadableArray headersArray) {
        if (headersArray == null) {
            return null;
        }
        Headers.Builder headersBuilder = new Headers.Builder();
        for (int headersIdx = 0, size = headersArray.size(); headersIdx < size; headersIdx++) {
            ReadableArray header = headersArray.getArray(headersIdx);
            if (header.size() == 2) {
                String headerName = RequestUtils.stripHeaderName(header.getString(0));
                String headerValue = header.getString(1);
                headersBuilder.addUnsafeNonAscii(headerName, headerValue);
            }
        }

        return headersBuilder.build();
    }
}
