package me.msile.app.androidapp.common.net.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;


/**
 * 进度通知requestBody
 */
public class ProgressMultipartRequestBody extends RequestBody {
    private final MultipartBody requestBody;
    private final ProgressListener progressListener;
    private BufferedSink bufferedSink;

    private long bytesWritten = 0L;
    private long contentLength;

    private ProgressMultipartRequestBody(MultipartBody requestBody, ProgressListener progressListener) throws IOException {
        this.requestBody = requestBody;
        this.progressListener = progressListener;
        contentLength = requestBody.contentLength();
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
            bufferedSink = Okio.buffer(new ProgressBufferSink(sink));
        }
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    class ProgressBufferSink extends ForwardingSink {

        ProgressBufferSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            progressListener.onProgress(bytesWritten, contentLength);
        }

    }

    public interface ProgressListener {
        void onProgress(long currentBytes, long contentLength);
    }

    public static final class Builder {
        private MediaType type = okhttp3.MultipartBody.MIXED;
        private final List<MultipartBody.Part> parts = new ArrayList<>();
        private ProgressListener progressListener;

        public Builder() {
        }

        public Builder setType(MediaType type) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            if (!type.type().equals("multipart")) {
                throw new IllegalArgumentException("multipart != " + type);
            }
            this.type = type;
            return this;
        }

        public Builder addPart(RequestBody body) {
            return addPart(MultipartBody.Part.create(body));
        }

        public Builder addPart(Headers headers, RequestBody body) {
            return addPart(MultipartBody.Part.create(headers, body));
        }

        public Builder addFormDataPart(String name, String value) {
            return addPart(MultipartBody.Part.createFormData(name, value));
        }

        public Builder addFormDataPart(String name, String filename, RequestBody body) {
            return addPart(MultipartBody.Part.createFormData(name, filename, body));
        }

        public Builder addPart(MultipartBody.Part part) {
            if (part == null) throw new NullPointerException("part == null");
            parts.add(part);
            return this;
        }

        public Builder addProgressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }

        public ProgressMultipartRequestBody build() throws IOException, IllegalStateException {
            if (parts.isEmpty()) {
                throw new IllegalStateException("Multipart body must have at least one part.");
            }
            if (progressListener == null) {
                throw new IllegalStateException("progress listener ");
            }
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(type);
            for (MultipartBody.Part part : parts) {
                builder.addPart(part);
            }
            return new ProgressMultipartRequestBody(builder.build(), progressListener);
        }
    }
}


