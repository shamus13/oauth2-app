package dk.grixie.oauth2.app.websocket;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import dk.grixie.oauth2.app.oauth2.token.AccessToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;

public class WebSocket {
    private static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final Socket socket;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;

    public WebSocket(Socket socket) {
        this.socket = socket;
    }

    public void connect(Uri uri, AccessToken accessToken) throws Exception {
        String host = uri.getHost();
        int port = uri.getPort() != 0 ? uri.getPort() : (uri.getScheme().equals("wss") ? 443 : 80);
        String resource = uri.getPath();

        if (accessToken != null) {
            resource += "?access_token=" + accessToken.getAccessTokenId();
        }

        if (uri.getScheme().equals("wss")) {
            socket.connect(new InetSocketAddress(host, port));

            String encodedWebSocketKey = new String(Base64.encode(generateWebSocketKey(), Base64.NO_WRAP), "UTF-8");

            bos = new BufferedOutputStream(socket.getOutputStream());

            bos.write(("GET " + resource + " HTTP/1.1\r\n").getBytes("UTF-8"));
            bos.write(("Host: " + host + ":" + port + "\r\n").getBytes("UTF-8"));
            bos.write("Upgrade: websocket\r\n".getBytes("UTF-8"));
            bos.write("Connection: Upgrade\r\n".getBytes("UTF-8"));
            bos.write(("Sec-WebSocket-Key: " + encodedWebSocketKey + "\r\n").getBytes("UTF-8"));
            bos.write("Sec-WebSocket-Version: 13\r\n".getBytes("UTF-8"));
            bos.write("\r\n".getBytes("UTF-8"));
            bos.flush();

            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            bis = new BufferedInputStream(socket.getInputStream());

            int c;
            int state = 0;

            while ((c = bis.read()) != -1) {
                switch (c) {
                    case '\r':
                        if (state == 0 || state == 2) {
                            ++state;
                        } else {
                            state = 1;
                        }
                        break;
                    case '\n':
                        if (state == 1 || state == 3) {
                            ++state;
                        } else {
                            state = 0;
                        }
                        break;
                    default:
                        state = 0;
                        break;
                }

                bas.write(c);

                if (state == 4) {
                    break;
                }
            }

            String response = bas.toString("UTF-8");

            MessageDigest digest = MessageDigest.getInstance("SHA1");

            String expectedKey = new String(Base64.encode(digest.digest((encodedWebSocketKey + MAGIC).
                    getBytes("UTF-8")), Base64.NO_WRAP), "UTF-8");

            if (!response.startsWith("HTTP/1.1 101") ||
                    !response.contains("Sec-WebSocket-Accept: " + expectedKey + "\r\n")) {

                Log.e(this.getClass().getName(), response);

                throw new Exception("OMG the sky is falling");
            }
        } else {
            throw new Exception("Unsupported protocol:" + uri.toString());
        }
    }

    public Frame read() throws Exception {
        int op = bis.read();

        if (op > 0) {
            int length;

            int temp = bis.read();

            if (temp >= 128) {
                throw new Exception("Masked frame received");
            } else if (temp == 127) {
                throw new IllegalArgumentException("Unsupported frame size");
            } else if (temp == 126) {
                length = temp;
                length <<= 8;
                length |= bis.read();
            } else {
                length = temp;
            }

            byte[] data = new byte[length];

            int count = 0;

            while (count < data.length) {
                temp = bis.read(data, count, data.length - count);

                if (temp >= 0) {
                    count += temp;
                } else {
                    throw new Exception("some sort of read error");
                }
            }

            Operation operation = Operation.PING;

            for (Operation o : Operation.values()) {
                if (o.getCode() == (op & 127)) {
                    operation = o;
                }
            }

            return new Frame(operation, (op & 128) != 0, data);
        } else {
            return null;
        }
    }

    public void write(Frame frame) throws Exception {
        bos.write((frame.isLast() ? 128 : 0) | frame.getOperation().getCode());

        if (frame.getData().length < 126) {
            bos.write(128 | frame.getData().length);//mask + length hopefully
        } else if (frame.getData().length < 65536) {
            bos.write(128 | 126);//mask + first extended length
            bos.write(frame.getData().length >> 8);
            bos.write(frame.getData().length & 8);
        } else {
            throw new IllegalArgumentException("Unsupported frame size");
        }

        //write bogus mask
        bos.write(0);//mask byte 1
        bos.write(0);//mask byte 2
        bos.write(0);//mask byte 3
        bos.write(0);//mask byte 4

        bos.write(frame.getData());
        bos.flush();
    }

    public void sendCloseFrame() throws Exception {
        socket.getOutputStream().write(128 | Operation.CLOSE.getCode());
        socket.getOutputStream().write(128);//mask + length 0
        socket.getOutputStream().write(0);//mask byte 1
        socket.getOutputStream().write(0);//mask byte 2
        socket.getOutputStream().write(0);//mask byte 3
        socket.getOutputStream().write(0);//mask byte 4

        socket.getOutputStream().flush();
    }

    public void close() throws Exception {
        socket.close();
    }

    private byte[] generateWebSocketKey() throws Exception {
        return "some random value".getBytes("UTF-8");
    }
}
